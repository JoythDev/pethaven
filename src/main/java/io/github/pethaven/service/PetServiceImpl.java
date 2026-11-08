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
    // Las redirecciones se siguen a mano para validar cada salto contra la política anti-SSRF.
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final OutboundUrlPolicy outboundUrlPolicy;

    @Autowired
    public PetServiceImpl(
            PetRepository petRepository,
            PetMapper petMapper,
            OwnerService ownerService,
            OutboundUrlPolicy outboundUrlPolicy
    ) {
        this.petRepository = petRepository;
        this.petMapper = petMapper;
        this.ownerService = ownerService;
        this.outboundUrlPolicy = outboundUrlPolicy;
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
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                if (!outboundUrlPolicy.isAllowed(uri)) {
                    log.warn("Descarga de la foto de la mascota {} bloqueada: destino no permitido ({})", id, uri.getHost());
                    return Optional.empty();
                }
                HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    if (REDIRECT_STATUSES.contains(response.statusCode())) {
                        Optional<String> location = response.headers().firstValue("Location");
                        if (location.isEmpty()) {
                            return Optional.empty();
                        }
                        uri = uri.resolve(location.get());
                        continue;
                    }
                    String type = response.headers().firstValue("Content-Type").orElse("").split(";")[0].trim().toLowerCase();
                    if (response.statusCode() != 200 || !PHOTO_TYPES.contains(type)) {
                        return Optional.empty();
                    }
                    byte[] data = body.readNBytes(MAX_PHOTO_BYTES + 1);
                    return data.length > MAX_PHOTO_BYTES ? Optional.empty() : Optional.of(new Photo(data, type));
                }
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
}
