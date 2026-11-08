package io.github.pethaven.service;

import com.sun.net.httpserver.HttpServer;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.PetMapper;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.security.ssrf.OutboundUrlPolicy;
import io.github.pethaven.security.ssrf.PublicNetworkUrlPolicy;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

// Pruebas del servicio con mocks: el PetMapper es real, el repositorio está simulado y la descarga de fotos se sirve desde un HTTP local.
@ExtendWith(MockitoExtension.class)
public class PetServiceMockTest {

    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;

    @Mock
    private PetRepository petRepository;
    @Mock
    private OwnerService ownerService;

    private PetServiceImpl petService;
    private HttpServer photoServer;
    private HttpServer secondServer;
    // La descarga de pruebas usa un HTTP local (loopback), que la política real bloquea: por defecto se permite
    // todo y cada prueba anti-SSRF reemplaza la política.
    private OutboundUrlPolicy outboundUrlPolicy = uri -> true;

    @BeforeEach
    public void setUp() {
        petService = new PetServiceImpl(petRepository, new PetMapper(), ownerService,
                uri -> outboundUrlPolicy.isAllowed(uri));
    }

    @AfterEach
    public void tearDown() {
        stopPhotoServer();
        if (secondServer != null) {
            secondServer.stop(0);
            secondServer = null;
        }
    }

    // ---------- Helpers ----------

    private Owner owner(Long id, String name) {
        Owner owner = RepositoryTestData.owner();
        owner.setId(id);
        owner.setName(name);
        return owner;
    }

    private Pet pet(Long id, Owner owner, String name, Species species, String breed, Integer age, Double weight) {
        Pet pet = RepositoryTestData.pet(owner);
        pet.setId(id);
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setAge(age);
        pet.setWeight(weight);
        return pet;
    }

    private Pet petWithPhoto(String photoUrl) {
        Pet pet = pet(1L, owner(1L, "Ana Torres"), "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        pet.setPhotoUrl(photoUrl);
        return pet;
    }

    private PetRequest createRequest() {
        return new PetRequest("Luna", 7L, Species.CAT, "Persa", 2, 3.75, "https://cdn.pethaven.test/luna.png");
    }

    // Servidor HTTP en un puerto efímero de loopback: la descarga de fotos no usa la red externa.
    private void startPhotoServer() throws IOException {
        photoServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        photoServer.start();
    }

    // Registra un contexto que responde siempre con el estado, el tipo y el cuerpo indicados.
    private void startPhotoServer(String path, int status, String contentType, byte[] body) throws IOException {
        photoServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        photoServer.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        photoServer.start();
    }

    private void stopPhotoServer() {
        if (photoServer != null) {
            photoServer.stop(0);
            photoServer = null;
        }
    }

    private String photoServerUrl(String path) {
        return "http://127.0.0.1:" + photoServer.getAddress().getPort() + path;
    }

    // ---------- getPetEntity ----------

    @Test
    public void petService_getPetEntity_ExistingId_ReturnsSameEntity() {
        // Arrange
        Pet pet = pet(1L, owner(1L, "Ana Torres"), "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Pet result = petService.getPetEntity(1L);

        // Assert
        Assertions.assertThat(result).isSameAs(pet);
        Mockito.verify(petRepository).findById(1L);
    }

    @Test
    public void petService_getPetEntity_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(petRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.getPetEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Pet' con id: '99' no fue encontrado");
    }

    // ---------- getAllPets ----------

    @Test
    public void petService_getAllPets_TwoPets_ReturnsMappedResponsesInOrder() {
        // Arrange
        Owner ana = owner(1L, "Ana Torres");
        Owner beto = owner(2L, "Beto Ruiz");
        Pet buddy = pet(1L, ana, "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        buddy.setDisease("Otitis externa");
        buddy.setPhotoUrl("https://cdn.pethaven.test/buddy.png");
        buddy.setActive(true);
        Pet michi = pet(2L, beto, "Michi", Species.CAT, "Siames", 5, 4.25);
        michi.setDisease("Dermatitis");
        michi.setPhotoUrl("https://cdn.pethaven.test/michi.png");
        michi.setActive(false);
        Mockito.when(petRepository.findAll()).thenReturn(List.of(buddy, michi));

        // Act
        List<PetResponse> result = petService.getAllPets();

        // Assert
        Assertions.assertThat(result).containsExactly(
                new PetResponse(1L, "Buddy", Species.DOG, "Golden Retriever", 3, 15.5, "Otitis externa",
                        "https://cdn.pethaven.test/buddy.png", true, 1L, "Ana Torres"),
                new PetResponse(2L, "Michi", Species.CAT, "Siames", 5, 4.25, "Dermatitis",
                        "https://cdn.pethaven.test/michi.png", false, 2L, "Beto Ruiz"));
        Mockito.verify(petRepository).findAll();
    }

    @Test
    public void petService_getAllPets_NoPets_ReturnsEmptyList() {
        // Arrange
        Mockito.when(petRepository.findAll()).thenReturn(List.of());

        // Act
        List<PetResponse> result = petService.getAllPets();

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(petRepository).findAll();
    }

    // ---------- searchPets ----------

    @Test
    public void petService_searchPets_NameArgument_ReturnsMappedResponses() {
        // Arrange
        Pet pet = pet(1L, owner(1L, "Ana Torres"), "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        Mockito.when(petRepository.findByNameContainingIgnoreCase("BuD")).thenReturn(List.of(pet));

        // Act
        List<PetResponse> result = petService.searchPets("BuD");

        // Assert
        Assertions.assertThat(result).containsExactly(
                new PetResponse(1L, "Buddy", Species.DOG, "Golden Retriever", 3, 15.5, null, null, true, 1L,
                        "Ana Torres"));
        // El nombre se delega tal cual, sin normalizar mayúsculas ni recortar espacios.
        Mockito.verify(petRepository).findByNameContainingIgnoreCase("BuD");
    }

    @Test
    public void petService_searchPets_NoMatches_ReturnsEmptyList() {
        // Arrange
        Mockito.when(petRepository.findByNameContainingIgnoreCase("zzz")).thenReturn(List.of());

        // Act
        List<PetResponse> result = petService.searchPets("zzz");

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(petRepository).findByNameContainingIgnoreCase("zzz");
    }

    // ---------- getPetById ----------

    @Test
    public void petService_getPetById_ExistingId_ReturnsMappedResponse() {
        // Arrange
        Pet pet = pet(1L, owner(1L, "Ana Torres"), "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        pet.setDisease("Otitis externa");
        pet.setPhotoUrl("https://cdn.pethaven.test/buddy.png");
        pet.setActive(false);
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        PetResponse result = petService.getPetById(1L);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new PetResponse(1L, "Buddy", Species.DOG, "Golden Retriever", 3, 15.5, "Otitis externa",
                        "https://cdn.pethaven.test/buddy.png", false, 1L, "Ana Torres"));
        Mockito.verify(petRepository).findById(1L);
    }

    @Test
    public void petService_getPetById_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(petRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.getPetById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Pet' con id: '99' no fue encontrado");
    }

    // ---------- createPet ----------

    @Test
    public void petService_createPet_ValidRequest_SavesEntityAndReturnsSavedResponse() {
        // Arrange
        PetRequest request = createRequest();
        Owner owner = owner(7L, "Ana Torres");
        Mockito.when(ownerService.getOwnerEntity(7L)).thenReturn(owner);
        // Simula la asignación del id que hace la base de datos al guardar y devuelve la instancia persistida.
        Mockito.when(petRepository.save(ArgumentMatchers.any(Pet.class)))
                .thenAnswer(invocation -> {
                    Pet toSave = invocation.getArgument(0);
                    return Pet.builder()
                            .id(42L)
                            .name(toSave.getName())
                            .species(toSave.getSpecies())
                            .breed(toSave.getBreed())
                            .age(toSave.getAge())
                            .weight(toSave.getWeight())
                            .disease(toSave.getDisease())
                            .photoUrl(toSave.getPhotoUrl())
                            .active(toSave.isActive())
                            .owner(toSave.getOwner())
                            .build();
                });

        // Act
        PetResponse result = petService.createPet(request);

        // Assert
        // La respuesta se construye desde la entidad guardada, ya con el id asignado.
        Assertions.assertThat(result)
                .isEqualTo(new PetResponse(42L, "Luna", Species.CAT, "Persa", 2, 3.75, null,
                        "https://cdn.pethaven.test/luna.png", true, 7L, "Ana Torres"));

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        Mockito.verify(petRepository).save(captor.capture());
        Pet saved = captor.getValue();
        Assertions.assertThat(saved.getName()).isEqualTo("Luna");
        Assertions.assertThat(saved.getSpecies()).isEqualTo(Species.CAT);
        Assertions.assertThat(saved.getBreed()).isEqualTo("Persa");
        Assertions.assertThat(saved.getAge()).isEqualTo(2);
        Assertions.assertThat(saved.getWeight()).isEqualTo(3.75);
        Assertions.assertThat(saved.getPhotoUrl()).isEqualTo("https://cdn.pethaven.test/luna.png");
        // El mapper marca la mascota como activa y el diagnóstico queda nulo hasta el primer tratamiento.
        Assertions.assertThat(saved.isActive()).isTrue();
        Assertions.assertThat(saved.getDisease()).isNull();
        Assertions.assertThat(saved.getOwner()).isSameAs(owner);
        Mockito.verify(ownerService).getOwnerEntity(7L);
    }

    @Test
    public void petService_createPet_NonExistingOwner_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        PetRequest request = createRequest();
        Mockito.when(ownerService.getOwnerEntity(7L))
                .thenThrow(new ResourceNotFoundException("Owner", "id", 7L));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.createPet(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con id: '7' no fue encontrado");
        Mockito.verify(petRepository, Mockito.never()).save(ArgumentMatchers.any(Pet.class));
    }

    // ---------- updatePet ----------

    @Test
    public void petService_updatePet_ValidRequest_AppliesFieldsAndReassignsOwner() {
        // Arrange
        Owner oldOwner = owner(1L, "Ana Torres");
        Owner newOwner = owner(2L, "Beto Ruiz");
        Pet existing = pet(1L, oldOwner, "Buddy", Species.DOG, "Labrador", 4, 20.0);
        existing.setDisease("Dermatitis");
        existing.setPhotoUrl("https://cdn.pethaven.test/old.png");
        existing.setActive(false);
        PetRequest request = new PetRequest(
                "Buddy II", 2L, Species.CAT, "Siames", 6, 8.5, "https://cdn.pethaven.test/new.png");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(ownerService.getOwnerEntity(2L)).thenReturn(newOwner);
        Mockito.when(petRepository.save(ArgumentMatchers.any(Pet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PetResponse result = petService.updatePet(1L, request);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new PetResponse(1L, "Buddy II", Species.CAT, "Siames", 6, 8.5, "Dermatitis",
                        "https://cdn.pethaven.test/new.png", false, 2L, "Beto Ruiz"));

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        Mockito.verify(petRepository).save(captor.capture());
        Pet saved = captor.getValue();
        // Se actualiza la misma instancia persistida, sin crear una entidad nueva.
        Assertions.assertThat(saved).isSameAs(existing);
        Assertions.assertThat(saved.getName()).isEqualTo("Buddy II");
        Assertions.assertThat(saved.getSpecies()).isEqualTo(Species.CAT);
        Assertions.assertThat(saved.getBreed()).isEqualTo("Siames");
        Assertions.assertThat(saved.getAge()).isEqualTo(6);
        Assertions.assertThat(saved.getWeight()).isEqualTo(8.5);
        Assertions.assertThat(saved.getPhotoUrl()).isEqualTo("https://cdn.pethaven.test/new.png");
        // El estado activo y el diagnóstico no se editan desde la petición: los gestiona el servicio y los tratamientos.
        Assertions.assertThat(saved.isActive()).isFalse();
        Assertions.assertThat(saved.getDisease()).isEqualTo("Dermatitis");
        Assertions.assertThat(saved.getOwner()).isSameAs(newOwner);
        Mockito.verify(ownerService).getOwnerEntity(2L);
    }

    @Test
    public void petService_updatePet_NonExistingId_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        PetRequest request = createRequest();
        Mockito.when(petRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.updatePet(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Pet' con id: '99' no fue encontrado");
        Mockito.verifyNoInteractions(ownerService);
        Mockito.verify(petRepository, Mockito.never()).save(ArgumentMatchers.any(Pet.class));
    }

    @Test
    public void petService_updatePet_NonExistingOwner_ThrowsResourceNotFoundAndDoesNotModifyPet() {
        // Arrange
        Owner oldOwner = owner(1L, "Ana Torres");
        Pet existing = pet(1L, oldOwner, "Buddy", Species.DOG, "Labrador", 4, 20.0);
        existing.setDisease("Dermatitis");
        existing.setPhotoUrl("https://cdn.pethaven.test/old.png");
        PetRequest request = new PetRequest(
                "Buddy II", 2L, Species.CAT, "Siames", 6, 8.5, "https://cdn.pethaven.test/new.png");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(ownerService.getOwnerEntity(2L))
                .thenThrow(new ResourceNotFoundException("Owner", "id", 2L));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.updatePet(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con id: '2' no fue encontrado");
        // El dueño se busca antes de aplicar los cambios: la mascota existente queda intacta.
        Assertions.assertThat(existing.getName()).isEqualTo("Buddy");
        Assertions.assertThat(existing.getSpecies()).isEqualTo(Species.DOG);
        Assertions.assertThat(existing.getBreed()).isEqualTo("Labrador");
        Assertions.assertThat(existing.getAge()).isEqualTo(4);
        Assertions.assertThat(existing.getWeight()).isEqualTo(20.0);
        Assertions.assertThat(existing.getPhotoUrl()).isEqualTo("https://cdn.pethaven.test/old.png");
        Assertions.assertThat(existing.getOwner()).isSameAs(oldOwner);
        Assertions.assertThat(existing.getDisease()).isEqualTo("Dermatitis");
        Assertions.assertThat(existing.isActive()).isTrue();
        Mockito.verify(petRepository, Mockito.never()).save(ArgumentMatchers.any(Pet.class));
    }

    // ---------- switchPetActiveStatus ----------

    @Test
    public void petService_switchPetActiveStatus_ActivePet_DeactivatesAndReturnsInactiveResponse() {
        // Arrange
        Pet pet = pet(1L, owner(1L, "Ana Torres"), "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        pet.setDisease("Otitis externa");
        pet.setPhotoUrl("https://cdn.pethaven.test/buddy.png");
        pet.setActive(true);
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));
        Mockito.when(petRepository.save(ArgumentMatchers.any(Pet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PetResponse result = petService.switchPetActiveStatus(1L, false);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new PetResponse(1L, "Buddy", Species.DOG, "Golden Retriever", 3, 15.5, "Otitis externa",
                        "https://cdn.pethaven.test/buddy.png", false, 1L, "Ana Torres"));

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        Mockito.verify(petRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue()).isSameAs(pet);
        Assertions.assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    public void petService_switchPetActiveStatus_InactivePet_ReactivatesAndReturnsActiveResponse() {
        // Arrange
        Pet pet = pet(1L, owner(1L, "Ana Torres"), "Buddy", Species.DOG, "Golden Retriever", 3, 15.5);
        pet.setDisease("Otitis externa");
        pet.setPhotoUrl("https://cdn.pethaven.test/buddy.png");
        pet.setActive(false);
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));
        Mockito.when(petRepository.save(ArgumentMatchers.any(Pet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PetResponse result = petService.switchPetActiveStatus(1L, true);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new PetResponse(1L, "Buddy", Species.DOG, "Golden Retriever", 3, 15.5, "Otitis externa",
                        "https://cdn.pethaven.test/buddy.png", true, 1L, "Ana Torres"));

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        Mockito.verify(petRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue()).isSameAs(pet);
        Assertions.assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    public void petService_switchPetActiveStatus_NonExistingId_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        Mockito.when(petRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.switchPetActiveStatus(99L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Pet' con id: '99' no fue encontrado");
        Mockito.verify(petRepository, Mockito.never()).save(ArgumentMatchers.any(Pet.class));
    }

    // ---------- getPetsByOwnerId ----------

    @Test
    public void petService_getPetsByOwnerId_OwnerWithPets_ReturnsMappedResponses() {
        // Arrange
        Owner owner = owner(3L, "Carla Gomez");
        Pet rex = pet(1L, owner, "Rex", Species.DOG, "Labrador", 2, 12.0);
        rex.setDisease("Dermatitis");
        rex.setPhotoUrl("https://cdn.pethaven.test/rex.png");
        rex.setActive(true);
        Pet nala = pet(2L, owner, "Nala", Species.CAT, "Persa", 4, 3.5);
        nala.setDisease(null);
        nala.setPhotoUrl(null);
        nala.setActive(false);
        Mockito.when(petRepository.findByOwnerId(3L)).thenReturn(List.of(rex, nala));

        // Act
        List<PetResponse> result = petService.getPetsByOwnerId(3L);

        // Assert
        Assertions.assertThat(result).containsExactly(
                new PetResponse(1L, "Rex", Species.DOG, "Labrador", 2, 12.0, "Dermatitis",
                        "https://cdn.pethaven.test/rex.png", true, 3L, "Carla Gomez"),
                new PetResponse(2L, "Nala", Species.CAT, "Persa", 4, 3.5, null, null, false, 3L, "Carla Gomez"));
        Mockito.verify(petRepository).findByOwnerId(3L);
    }

    @Test
    public void petService_getPetsByOwnerId_OwnerWithoutPets_ReturnsEmptyList() {
        // Arrange
        Mockito.when(petRepository.findByOwnerId(3L)).thenReturn(List.of());

        // Act
        List<PetResponse> result = petService.getPetsByOwnerId(3L);

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(petRepository).findByOwnerId(3L);
    }

    // ---------- getPetPhoto ----------
    // Las descargas usan un HttpClient interno del servicio, así que cada escenario levanta un
    // servidor HTTP de loopback en un puerto efímero.

    @Test
    public void petService_getPetPhoto_NonExistingPet_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(petRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> petService.getPetPhoto(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Pet' con id: '99' no fue encontrado");
    }

    @Test
    public void petService_getPetPhoto_NullPhotoUrl_ReturnsEmpty() {
        // Arrange
        Pet pet = petWithPhoto(null);
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_NonHttpPhotoUrl_ReturnsEmpty() {
        // Arrange
        Pet pet = petWithPhoto("ftp://example.test/a.png");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_RelativePhotoUrl_ReturnsEmpty() {
        // Arrange
        Pet pet = petWithPhoto("/local/a.png");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_PngResponse_ReturnsPhoto() throws IOException {
        // Arrange
        byte[] body = new byte[]{10, 20, 30, 40, 50};
        startPhotoServer("/photo.png", 200, "image/png", body);
        Pet pet = petWithPhoto(photoServerUrl("/photo.png"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().data()).isEqualTo(body);
        Assertions.assertThat(result.get().contentType()).isEqualTo("image/png");
    }

    @Test
    public void petService_getPetPhoto_UppercaseJpegContentType_NormalizesContentType() throws IOException {
        // Arrange
        byte[] body = new byte[]{1, 2, 3, 4};
        startPhotoServer("/photo.jpg", 200, "IMAGE/JPEG; charset=binary", body);
        Pet pet = petWithPhoto(photoServerUrl("/photo.jpg"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().data()).isEqualTo(body);
        // Se descarta el juego de caracteres y se normaliza a minúsculas.
        Assertions.assertThat(result.get().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    public void petService_getPetPhoto_ErrorStatus_ReturnsEmpty() throws IOException {
        // Arrange
        startPhotoServer("/missing.png", 404, "image/png", new byte[]{1, 2, 3, 4});
        Pet pet = petWithPhoto(photoServerUrl("/missing.png"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_HtmlContentType_ReturnsEmpty() throws IOException {
        // Arrange
        startPhotoServer("/page.html", 200, "text/html", new byte[]{1, 2, 3, 4});
        Pet pet = petWithPhoto(photoServerUrl("/page.html"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_SvgContentType_ReturnsEmpty() throws IOException {
        // Arrange
        startPhotoServer("/vector.svg", 200, "image/svg+xml", new byte[]{1, 2, 3, 4});
        Pet pet = petWithPhoto(photoServerUrl("/vector.svg"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        // SVG queda excluido a propósito porque puede traer scripts.
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_BodyAtMaxSize_ReturnsPhoto() throws IOException {
        // Arrange
        byte[] body = new byte[MAX_PHOTO_BYTES];
        startPhotoServer("/large.png", 200, "image/png", body);
        Pet pet = petWithPhoto(photoServerUrl("/large.png"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().data()).hasSize(MAX_PHOTO_BYTES);
        Assertions.assertThat(result.get().contentType()).isEqualTo("image/png");
    }

    @Test
    public void petService_getPetPhoto_BodyOverMaxSize_ReturnsEmpty() throws IOException {
        // Arrange
        byte[] body = new byte[MAX_PHOTO_BYTES + 1];
        startPhotoServer("/too-large.png", 200, "image/png", body);
        Pet pet = petWithPhoto(photoServerUrl("/too-large.png"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_MalformedUrl_ReturnsEmpty() {
        // Arrange
        Pet pet = petWithPhoto("http://bad host/a.png");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        // La URL empieza por http:// pero no es válida: se captura IllegalArgumentException.
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petService_getPetPhoto_ConnectionRefused_ReturnsEmpty() throws IOException {
        // Arrange
        startPhotoServer();
        int closedPort = photoServer.getAddress().getPort();
        stopPhotoServer();
        Pet pet = petWithPhoto("http://127.0.0.1:" + closedPort + "/photo.png");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(pet));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        // No hay nada escuchando en el puerto cerrado: se captura IOException.
        Assertions.assertThat(result).isEmpty();
    }

    // ---------- getPetPhoto: protección SSRF ----------

    // Servidor que cuenta las peticiones recibidas y responde una redirección (Location vacío = sin cabecera)
    // o una imagen PNG.
    private HttpServer countingServer(AtomicInteger hits, String redirectLocation) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            if (redirectLocation != null) {
                if (!redirectLocation.isEmpty()) {
                    exchange.getResponseHeaders().set("Location", redirectLocation);
                }
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            byte[] body = {1, 2, 3, 4};
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        server.start();
        return server;
    }

    private static String url(HttpServer server, String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Test
    public void petService_getPetPhoto_BlockedDestination_ReturnsEmptyWithoutRequesting() throws IOException {
        // Arrange
        AtomicInteger hits = new AtomicInteger();
        photoServer = countingServer(hits, null);
        outboundUrlPolicy = uri -> false;
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/photo.png"))));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
        Assertions.assertThat(hits).hasValue(0);
    }

    @Test
    public void petService_getPetPhoto_RedirectToBlockedDestination_ReturnsEmptyWithoutFollowing() throws IOException {
        // Arrange
        // Un host permitido redirige a otro bloqueado (p. ej. una IP interna): el salto se valida y no se sigue.
        AtomicInteger internalHits = new AtomicInteger();
        secondServer = countingServer(internalHits, null);
        AtomicInteger publicHits = new AtomicInteger();
        photoServer = countingServer(publicHits, url(secondServer, "/secret.png"));
        int allowedPort = photoServer.getAddress().getPort();
        outboundUrlPolicy = uri -> uri.getPort() == allowedPort;
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/start"))));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).isEmpty();
        Assertions.assertThat(publicHits).hasValue(1);
        Assertions.assertThat(internalHits).hasValue(0);
    }

    @Test
    public void petService_getPetPhoto_RedirectToAllowedDestination_FollowsAndReturnsPhoto() throws IOException {
        // Arrange
        // Redirecciones legítimas (p. ej. http -> https o a un CDN) se siguen si el destino está permitido.
        AtomicInteger cdnHits = new AtomicInteger();
        secondServer = countingServer(cdnHits, null);
        AtomicInteger originHits = new AtomicInteger();
        photoServer = countingServer(originHits, url(secondServer, "/photo.png"));
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/start"))));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        Assertions.assertThat(result).hasValueSatisfying(photo -> {
            Assertions.assertThat(photo.contentType()).isEqualTo("image/png");
            Assertions.assertThat(photo.data()).containsExactly(1, 2, 3, 4);
        });
        Assertions.assertThat(originHits).hasValue(1);
        Assertions.assertThat(cdnHits).hasValue(1);
    }

    @Test
    public void petService_getPetPhoto_RelativeRedirect_IsResolvedAgainstCurrentUrl() throws IOException {
        // Arrange
        AtomicInteger hits = new AtomicInteger();
        photoServer = countingServer(hits, "/other");
        int[] seen = new int[1];
        outboundUrlPolicy = uri -> {
            seen[0]++;
            // La redirección relativa conserva host y puerto del servidor actual.
            return uri.getHost().equals("127.0.0.1") && uri.getPort() == photoServer.getAddress().getPort();
        };
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/start"))));

        // Act
        petService.getPetPhoto(1L);

        // Assert
        // Cada salto pasa por la política: inicial + 3 redirecciones (todas a /other) = 4 validaciones.
        Assertions.assertThat(seen[0]).isEqualTo(4);
    }

    @Test
    public void petService_getPetPhoto_MoreThanThreeRedirects_ReturnsEmpty() throws IOException {
        // Arrange
        AtomicInteger hits = new AtomicInteger();
        photoServer = countingServer(hits, "/loop");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/loop"))));

        // Act
        Optional<PetService.Photo> result = petService.getPetPhoto(1L);

        // Assert
        // Petición inicial + 3 redirecciones seguidas; la cuarta redirección ya no se sigue.
        Assertions.assertThat(result).isEmpty();
        Assertions.assertThat(hits).hasValue(4);
    }

    @Test
    public void petService_getPetPhoto_RedirectWithoutLocation_ReturnsEmpty() throws IOException {
        // Arrange
        AtomicInteger hits = new AtomicInteger();
        photoServer = countingServer(hits, "");
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/start"))));

        // Act + Assert
        Assertions.assertThat(petService.getPetPhoto(1L)).isEmpty();
        Assertions.assertThat(hits).hasValue(1);
    }

    @Test
    public void petService_getPetPhoto_ProductionPolicyAndLoopbackUrl_ReturnsEmptyWithoutRequesting() throws IOException {
        // Arrange
        // Con la política real, una foto apuntando a la propia máquina (127.0.0.1) nunca se descarga.
        AtomicInteger hits = new AtomicInteger();
        photoServer = countingServer(hits, null);
        PetServiceImpl protectedService =
                new PetServiceImpl(petRepository, new PetMapper(), ownerService, new PublicNetworkUrlPolicy());
        Mockito.when(petRepository.findById(1L)).thenReturn(Optional.of(petWithPhoto(url(photoServer, "/photo.png"))));

        // Act + Assert
        Assertions.assertThat(protectedService.getPetPhoto(1L)).isEmpty();
        Assertions.assertThat(hits).hasValue(0);
        Assertions.assertThat(URI.create(url(photoServer, "/photo.png")).getHost()).isEqualTo("127.0.0.1");
    }
}
