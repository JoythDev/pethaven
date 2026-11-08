package io.github.pethaven.service;

import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.PetMapper;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.security.ssrf.OutboundUrlPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetMapper petMapper;
    private final OwnerService ownerService;

    // SVG queda fuera a propósito: puede traer scripts y se serviría desde el origen del API.
    private static final Set<String> PHOTO_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/avif");
    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);
    private static final Duration HOP_TIMEOUT = Duration.ofSeconds(10);
    /** Tiempo máximo de toda la descarga, redirecciones incluidas (la URL la controla un usuario). */
    private static final Duration DOWNLOAD_BUDGET = Duration.ofSeconds(20);
    // Las redirecciones se siguen a mano para validar cada salto contra la política anti-SSRF.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** Respuesta de un salto: estado, tipo de contenido, Location (si redirige) y cuerpo (solo con 200). */
    record FetchResult(int status, String contentType, String location, byte[] body) { }

    /** Transporte de un salto HTTP; sustituible en pruebas (p. ej. para simular orígenes HTTPS). */
    @FunctionalInterface
    interface PhotoFetcher {
        FetchResult fetch(URI uri, Duration timeout) throws IOException, InterruptedException;
    }

    private final OutboundUrlPolicy outboundUrlPolicy;
    private final Duration downloadBudget;
    private final PhotoFetcher photoFetcher;

    @Autowired
    public PetServiceImpl(
            PetRepository petRepository,
            PetMapper petMapper,
            OwnerService ownerService,
            OutboundUrlPolicy outboundUrlPolicy
    ) {
        this(petRepository, petMapper, ownerService, outboundUrlPolicy, DOWNLOAD_BUDGET);
    }

    PetServiceImpl(PetRepository petRepository, PetMapper petMapper, OwnerService ownerService,
                   OutboundUrlPolicy outboundUrlPolicy, Duration downloadBudget) {
        this(petRepository, petMapper, ownerService, outboundUrlPolicy, downloadBudget, PetServiceImpl::httpFetch);
    }

    PetServiceImpl(PetRepository petRepository, PetMapper petMapper, OwnerService ownerService,
                   OutboundUrlPolicy outboundUrlPolicy, Duration downloadBudget, PhotoFetcher photoFetcher) {
        this.petRepository = petRepository;
        this.petMapper = petMapper;
        this.ownerService = ownerService;
        this.outboundUrlPolicy = outboundUrlPolicy;
        this.downloadBudget = downloadBudget;
        this.photoFetcher = photoFetcher;
    }

    @Override
    public Pet getPetEntity(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getAllPets() {
        return petRepository.findAll().stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> searchPets(String name) {
        return petRepository.findByNameContainingIgnoreCase(name).stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPetById(Long id) {
        return petMapper.toResponse(getPetEntity(id));
    }

    @Override
    @Transactional
    public PetResponse createPet(PetRequest request) {
        Pet pet = petMapper.toEntity(request);
        Owner owner = ownerService.getOwnerEntity(request.ownerId());
        pet.setOwner(owner);
        Pet saved = petRepository.save(pet);
        return petMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PetResponse updatePet(Long id, PetRequest updatedPet) {
        Pet existing = getPetEntity(id);
        Owner owner = ownerService.getOwnerEntity(updatedPet.ownerId());
        petMapper.applyUpdate(updatedPet, existing);
        existing.setOwner(owner);
        Pet updated = petRepository.save(existing);
        return petMapper.toResponse(updated);
    }

    // NOTE: Una mascota no se puede eliminar directamente, solo se puede desactivar.
    // Para eliminar una mascota, se debe eliminar su dueño (eliminación en cascada)
    @Override
    @Transactional
    public PetResponse switchPetActiveStatus(Long id, boolean active) {
        Pet pet = getPetEntity(id);
        pet.setActive(active);
        petRepository.save(pet);
        return petMapper.toResponse(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getPetsByOwnerId(Long ownerId) {
        return petRepository.findByOwnerId(ownerId).stream()
                .map(petMapper::toResponse)
                .toList();
    }

    // Descarga la foto desde su URL externa para servirla desde el API: varios hosts
    // (p. ej. placedog.net) no permiten CORS y el navegador no podría leerla para el PDF.
    // La URL la escribe un usuario, así que cada destino (incluidas hasta 3 redirecciones) pasa por la
    // política anti-SSRF: nunca se piden direcciones internas (loopback, red privada, metadatos de la nube).
    // Sin foto, con URL inválida o bloqueada, o si la descarga falla, devuelve vacío.
    @Override
    public Optional<Photo> getPetPhoto(Long id) {
        String url = getPetEntity(id).getPhotoUrl();
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(url);
            long deadline = System.nanoTime() + downloadBudget.toNanos();
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                if (!outboundUrlPolicy.isAllowed(uri)) {
                    log.warn("Descarga de la foto de la mascota {} bloqueada: destino no permitido ({})", id, uri.getHost());
                    return Optional.empty();
                }
                Duration remaining = Duration.ofNanos(deadline - System.nanoTime());
                if (remaining.isNegative() || remaining.isZero()) {
                    return Optional.empty();
                }
                FetchResult response = photoFetcher.fetch(uri, remaining.compareTo(HOP_TIMEOUT) < 0 ? remaining : HOP_TIMEOUT);
                if (REDIRECT_STATUSES.contains(response.status())) {
                    if (response.location() == null || response.location().isBlank()) {
                        return Optional.empty();
                    }
                    URI next = uri.resolve(response.location());
                    // Nunca se rebaja de https a http: la descarga quedaría expuesta en claro.
                    if ("https".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(next.getScheme())) {
                        return Optional.empty();
                    }
                    uri = next;
                    continue;
                }
                String type = response.contentType().split(";")[0].trim().toLowerCase();
                if (response.status() != 200 || !PHOTO_TYPES.contains(type)) {
                    return Optional.empty();
                }
                byte[] data = response.body();
                return data.length > MAX_PHOTO_BYTES ? Optional.empty() : Optional.of(new Photo(data, type));
            }
            // Más redirecciones de las permitidas.
            return Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** Un salto con el cliente HTTP real; el cuerpo solo se lee con 200 y como máximo MAX_PHOTO_BYTES + 1. */
    private static FetchResult httpFetch(URI uri, Duration timeout) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            byte[] data = response.statusCode() == 200 ? body.readNBytes(MAX_PHOTO_BYTES + 1) : new byte[0];
            return new FetchResult(response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.headers().firstValue("Location").orElse(null),
                    data);
        }
    }
}
