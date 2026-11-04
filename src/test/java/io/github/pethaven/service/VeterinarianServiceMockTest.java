package io.github.pethaven.service;

import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.AccountDisabledException;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.VeterinarianMapper;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el VeterinarianMapper es real y el repositorio es simulado, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class VeterinarianServiceMockTest {

    @Mock
    private VeterinarianRepository veterinarianRepository;

    private VeterinarianServiceImpl veterinarianService;

    @BeforeEach
    public void setUp() {
        veterinarianService = new VeterinarianServiceImpl(veterinarianRepository, new VeterinarianMapper());
    }

    // ---------- Helpers ----------

    private Veterinarian veterinarian(
            Long id, String document, String name, String email, String specialty, String photoUrl) {
        Veterinarian veterinarian = RepositoryTestData.veterinarian();
        veterinarian.setId(id);
        veterinarian.setDocument(document);
        veterinarian.setName(name);
        veterinarian.setEmail(email);
        veterinarian.setSpecialty(specialty);
        veterinarian.setPhotoUrl(photoUrl);
        return veterinarian;
    }

    private VeterinarianRequest createRequest() {
        return new VeterinarianRequest(
                "VET-900",
                "Dra. Laura Gómez",
                "laura.gomez@pethaven.test",
                "plain-password",
                "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
    }

    // ---------- getVeterinarianEntity ----------

    @Test
    public void veterinarianService_getVeterinarianEntity_ExistingId_ReturnsEntity() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(veterinarian));

        // Act
        Veterinarian result = veterinarianService.getVeterinarianEntity(1L);

        // Assert
        Assertions.assertThat(result).isSameAs(veterinarian);
        Mockito.verify(veterinarianRepository).findById(1L);
    }

    @Test
    public void veterinarianService_getVeterinarianEntity_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(veterinarianRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.getVeterinarianEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Veterinarian' con id: '99' no fue encontrado");
    }

    // ---------- getAllVeterinarians ----------

    @Test
    public void veterinarianService_getAllVeterinarians_TwoVeterinarians_ReturnsMappedResponsesInOrder() {
        // Arrange
        Veterinarian first = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        first.setAttentions(5);
        first.setActive(true);
        Veterinarian second = veterinarian(
                2L, "VET-200", "Dr. Mario Peña", "mario@pethaven.test", "Cirugía",
                "https://pethaven.test/photos/mario.jpg");
        second.setAttentions(11);
        second.setActive(false);
        Mockito.when(veterinarianRepository.findAll()).thenReturn(List.of(first, second));

        // Act
        List<VeterinarianResponse> result = veterinarianService.getAllVeterinarians();

        // Assert
        Assertions.assertThat(result).containsExactly(
                new VeterinarianResponse(
                        1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                        "https://pethaven.test/photos/laura.jpg", 5, true),
                new VeterinarianResponse(
                        2L, "VET-200", "Dr. Mario Peña", "mario@pethaven.test", "Cirugía",
                        "https://pethaven.test/photos/mario.jpg", 11, false));
        Mockito.verify(veterinarianRepository).findAll();
    }

    @Test
    public void veterinarianService_getAllVeterinarians_NoVeterinarians_ReturnsEmptyList() {
        // Arrange
        Mockito.when(veterinarianRepository.findAll()).thenReturn(List.of());

        // Act
        List<VeterinarianResponse> result = veterinarianService.getAllVeterinarians();

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    // ---------- getVeterinarianById ----------

    @Test
    public void veterinarianService_getVeterinarianById_ExistingId_ReturnsMappedResponse() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        veterinarian.setAttentions(3);
        veterinarian.setActive(true);
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(veterinarian));

        // Act
        VeterinarianResponse result = veterinarianService.getVeterinarianById(1L);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new VeterinarianResponse(
                        1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                        "https://pethaven.test/photos/laura.jpg", 3, true));
        Mockito.verify(veterinarianRepository).findById(1L);
    }

    @Test
    public void veterinarianService_getVeterinarianById_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(veterinarianRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.getVeterinarianById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Veterinarian' con id: '99' no fue encontrado");
    }

    // ---------- getVeterinarianByDocument ----------

    @Test
    public void veterinarianService_getVeterinarianByDocument_ExistingDocument_ReturnsMappedResponse() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        Mockito.when(veterinarianRepository.findByDocument("VET-100")).thenReturn(Optional.of(veterinarian));

        // Act
        VeterinarianResponse result = veterinarianService.getVeterinarianByDocument("VET-100");

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new VeterinarianResponse(
                        1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                        "https://pethaven.test/photos/laura.jpg", 0, true));
        Mockito.verify(veterinarianRepository).findByDocument("VET-100");
    }

    @Test
    public void veterinarianService_getVeterinarianByDocument_NonExistingDocument_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(veterinarianRepository.findByDocument("VET-404")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.getVeterinarianByDocument("VET-404"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Veterinarian' con document: 'VET-404' no fue encontrado");
    }

    // ---------- getVeterinarianByEmail ----------

    @Test
    public void veterinarianService_getVeterinarianByEmail_ExistingEmail_ReturnsMappedResponse() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        Mockito.when(veterinarianRepository.findByEmail("laura@pethaven.test")).thenReturn(Optional.of(veterinarian));

        // Act
        VeterinarianResponse result = veterinarianService.getVeterinarianByEmail("laura@pethaven.test");

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new VeterinarianResponse(
                        1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                        "https://pethaven.test/photos/laura.jpg", 0, true));
        Mockito.verify(veterinarianRepository).findByEmail("laura@pethaven.test");
    }

    @Test
    public void veterinarianService_getVeterinarianByEmail_NonExistingEmail_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(veterinarianRepository.findByEmail("nadie@pethaven.test")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.getVeterinarianByEmail("nadie@pethaven.test"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Veterinarian' con email: 'nadie@pethaven.test' no fue encontrado");
    }

    // ---------- createVeterinarian ----------

    @Test
    public void veterinarianService_createVeterinarian_ValidRequest_SavesEntityAndReturnsSavedResponse() {
        // Arrange
        VeterinarianRequest request = createRequest();
        Mockito.when(veterinarianRepository.existsByDocument("VET-900")).thenReturn(false);
        Mockito.when(veterinarianRepository.existsByEmail("laura.gomez@pethaven.test")).thenReturn(false);
        // Simula la asignación del id que hace la base de datos al guardar.
        Mockito.when(veterinarianRepository.save(ArgumentMatchers.any(Veterinarian.class)))
                .thenAnswer(invocation -> {
                    Veterinarian toSave = invocation.getArgument(0);
                    toSave.setId(10L);
                    return toSave;
                });

        // Act
        VeterinarianResponse result = veterinarianService.createVeterinarian(request);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new VeterinarianResponse(
                        10L, "VET-900", "Dra. Laura Gómez", "laura.gomez@pethaven.test", "Cuidados Críticos",
                        "https://pethaven.test/photos/laura.jpg", 0, true));

        ArgumentCaptor<Veterinarian> captor = ArgumentCaptor.forClass(Veterinarian.class);
        Mockito.verify(veterinarianRepository).save(captor.capture());
        Veterinarian saved = captor.getValue();
        Assertions.assertThat(saved.getDocument()).isEqualTo("VET-900");
        Assertions.assertThat(saved.getName()).isEqualTo("Dra. Laura Gómez");
        Assertions.assertThat(saved.getEmail()).isEqualTo("laura.gomez@pethaven.test");
        Assertions.assertThat(saved.getPassword()).isEqualTo("plain-password");
        Assertions.assertThat(saved.getSpecialty()).isEqualTo("Cuidados Críticos");
        Assertions.assertThat(saved.getPhotoUrl()).isEqualTo("https://pethaven.test/photos/laura.jpg");
        // El mapper inicializa los campos gestionados por el sistema al crear la entidad.
        Assertions.assertThat(saved.getAttentions()).isZero();
        Assertions.assertThat(saved.isActive()).isTrue();

        // La creación comprueba unicidad por documento y correo sin excluir ningún id.
        Mockito.verify(veterinarianRepository).existsByDocument("VET-900");
        Mockito.verify(veterinarianRepository).existsByEmail("laura.gomez@pethaven.test");
        Mockito.verify(veterinarianRepository, Mockito.never())
                .existsByDocumentAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
        Mockito.verify(veterinarianRepository, Mockito.never())
                .existsByEmailAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void veterinarianService_createVeterinarian_DuplicateDocument_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        VeterinarianRequest request = createRequest();
        Mockito.when(veterinarianRepository.existsByDocument("VET-900")).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.createVeterinarian(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un veterinario con el documento 'VET-900'.");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
        // El documento ya es duplicado: no hace falta comprobar el correo.
        Mockito.verify(veterinarianRepository, Mockito.never()).existsByEmail(ArgumentMatchers.anyString());
    }

    @Test
    public void veterinarianService_createVeterinarian_DuplicateEmail_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        VeterinarianRequest request = createRequest();
        Mockito.when(veterinarianRepository.existsByDocument("VET-900")).thenReturn(false);
        Mockito.when(veterinarianRepository.existsByEmail("laura.gomez@pethaven.test")).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.createVeterinarian(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un veterinario con el correo 'laura.gomez@pethaven.test'.");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
    }

    // ---------- updateVeterinarian ----------

    @Test
    public void veterinarianService_updateVeterinarian_ValidRequestWithoutPassword_AppliesFieldsAndKeepsStateAndTreatments() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        existing.setPassword("old-password");
        existing.setAttentions(7);
        existing.setActive(false);
        Treatment treatment = RepositoryTestData.treatment(
                RepositoryTestData.pet(RepositoryTestData.owner()), existing,
                LocalDateTime.of(2026, 1, 15, 9, 30), Evolution.STABLE);
        existing.getTreatments().add(treatment);
        VeterinarianUpdateRequest request = new VeterinarianUpdateRequest(
                "VET-101", "Dra. Laura Gómez Ruiz", "laura.ruiz@pethaven.test", null, "Cirugía",
                "https://pethaven.test/photos/laura-ruiz.jpg");
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(veterinarianRepository.existsByDocumentAndIdNot("VET-101", 1L)).thenReturn(false);
        Mockito.when(veterinarianRepository.existsByEmailAndIdNot("laura.ruiz@pethaven.test", 1L)).thenReturn(false);
        Mockito.when(veterinarianRepository.save(ArgumentMatchers.any(Veterinarian.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VeterinarianResponse result = veterinarianService.updateVeterinarian(1L, request);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new VeterinarianResponse(
                        1L, "VET-101", "Dra. Laura Gómez Ruiz", "laura.ruiz@pethaven.test", "Cirugía",
                        "https://pethaven.test/photos/laura-ruiz.jpg", 7, false));

        ArgumentCaptor<Veterinarian> captor = ArgumentCaptor.forClass(Veterinarian.class);
        Mockito.verify(veterinarianRepository).save(captor.capture());
        Veterinarian saved = captor.getValue();
        // Se actualiza la misma instancia persistida, sin crear una entidad nueva.
        Assertions.assertThat(saved).isSameAs(existing);
        Assertions.assertThat(saved.getDocument()).isEqualTo("VET-101");
        Assertions.assertThat(saved.getName()).isEqualTo("Dra. Laura Gómez Ruiz");
        Assertions.assertThat(saved.getEmail()).isEqualTo("laura.ruiz@pethaven.test");
        Assertions.assertThat(saved.getSpecialty()).isEqualTo("Cirugía");
        Assertions.assertThat(saved.getPhotoUrl()).isEqualTo("https://pethaven.test/photos/laura-ruiz.jpg");
        // Sin contraseña en la petición se conserva la actual.
        Assertions.assertThat(saved.getPassword()).isEqualTo("old-password");
        // El contador de atenciones y el estado laboral no forman parte del request: se conservan.
        Assertions.assertThat(saved.getAttentions()).isEqualTo(7);
        Assertions.assertThat(saved.isActive()).isFalse();
        // Los tratamientos existentes siguen asociados a la entidad guardada.
        Assertions.assertThat(saved.getTreatments()).containsExactly(treatment);

        // La actualización excluye al propio veterinario de las comprobaciones de unicidad.
        Mockito.verify(veterinarianRepository).existsByDocumentAndIdNot("VET-101", 1L);
        Mockito.verify(veterinarianRepository).existsByEmailAndIdNot("laura.ruiz@pethaven.test", 1L);
        Mockito.verify(veterinarianRepository, Mockito.never()).existsByDocument(ArgumentMatchers.anyString());
        Mockito.verify(veterinarianRepository, Mockito.never()).existsByEmail(ArgumentMatchers.anyString());
    }

    @Test
    public void veterinarianService_updateVeterinarian_ValidRequestWithPassword_ReplacesPassword() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        existing.setPassword("old-password");
        VeterinarianUpdateRequest request = new VeterinarianUpdateRequest(
                "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "new-password", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(veterinarianRepository.existsByDocumentAndIdNot("VET-100", 1L)).thenReturn(false);
        Mockito.when(veterinarianRepository.existsByEmailAndIdNot("laura@pethaven.test", 1L)).thenReturn(false);
        Mockito.when(veterinarianRepository.save(ArgumentMatchers.any(Veterinarian.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        veterinarianService.updateVeterinarian(1L, request);

        // Assert
        ArgumentCaptor<Veterinarian> captor = ArgumentCaptor.forClass(Veterinarian.class);
        Mockito.verify(veterinarianRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getPassword()).isEqualTo("new-password");
    }

    @Test
    public void veterinarianService_updateVeterinarian_NonExistingId_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        VeterinarianUpdateRequest request = new VeterinarianUpdateRequest(
                "VET-101", "Dra. Laura Gómez Ruiz", "laura.ruiz@pethaven.test", null, "Cirugía",
                "https://pethaven.test/photos/laura-ruiz.jpg");
        Mockito.when(veterinarianRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.updateVeterinarian(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Veterinarian' con id: '99' no fue encontrado");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
        Mockito.verify(veterinarianRepository, Mockito.never())
                .existsByDocumentAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void veterinarianService_updateVeterinarian_DuplicateDocument_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        VeterinarianUpdateRequest request = new VeterinarianUpdateRequest(
                "VET-200", "Dra. Laura Gómez", "laura@pethaven.test", null, "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(veterinarianRepository.existsByDocumentAndIdNot("VET-200", 1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.updateVeterinarian(1L, request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un veterinario con el documento 'VET-200'.");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
        Mockito.verify(veterinarianRepository, Mockito.never())
                .existsByEmailAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void veterinarianService_updateVeterinarian_DuplicateEmail_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        VeterinarianUpdateRequest request = new VeterinarianUpdateRequest(
                "VET-100", "Dra. Laura Gómez", "tomado@pethaven.test", null, "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(veterinarianRepository.existsByDocumentAndIdNot("VET-100", 1L)).thenReturn(false);
        Mockito.when(veterinarianRepository.existsByEmailAndIdNot("tomado@pethaven.test", 1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.updateVeterinarian(1L, request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un veterinario con el correo 'tomado@pethaven.test'.");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
    }

    // ---------- switchVeterinarianActiveStatus ----------

    @Test
    public void veterinarianService_switchVeterinarianActiveStatus_ActiveVeterinarianDeactivated_SavesSameInstanceInactive() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        existing.setActive(true);
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(veterinarianRepository.save(ArgumentMatchers.any(Veterinarian.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VeterinarianResponse result = veterinarianService.switchVeterinarianActiveStatus(1L, false);

        // Assert
        Assertions.assertThat(result.active()).isFalse();
        ArgumentCaptor<Veterinarian> captor = ArgumentCaptor.forClass(Veterinarian.class);
        Mockito.verify(veterinarianRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue()).isSameAs(existing);
        Assertions.assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    public void veterinarianService_switchVeterinarianActiveStatus_InactiveVeterinarianReactivated_ReturnsActiveResponse() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        existing.setActive(false);
        Mockito.when(veterinarianRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(veterinarianRepository.save(ArgumentMatchers.any(Veterinarian.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VeterinarianResponse result = veterinarianService.switchVeterinarianActiveStatus(1L, true);

        // Assert
        Assertions.assertThat(result.active()).isTrue();
        ArgumentCaptor<Veterinarian> captor = ArgumentCaptor.forClass(Veterinarian.class);
        Mockito.verify(veterinarianRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue()).isSameAs(existing);
        Assertions.assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    public void veterinarianService_switchVeterinarianActiveStatus_NonExistingId_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        Mockito.when(veterinarianRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.switchVeterinarianActiveStatus(99L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Veterinarian' con id: '99' no fue encontrado");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
    }

    // ---------- authenticate ----------
    // Comportamiento provisional: la contraseña se compara en texto plano. Estos tests fijan
    // ese comportamiento hasta que se implemente Spring Security + BCrypt + JWT.

    @Test
    public void veterinarianService_authenticate_CorrectPassword_ReturnsVeterinarian() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        veterinarian.setPassword("secret-password");
        veterinarian.setActive(true);
        Mockito.when(veterinarianRepository.findByEmail("laura@pethaven.test")).thenReturn(Optional.of(veterinarian));

        // Act
        Veterinarian result = veterinarianService.authenticate("laura@pethaven.test", "secret-password");

        // Assert
        Assertions.assertThat(result).isSameAs(veterinarian);
    }

    @Test
    public void veterinarianService_authenticate_WrongPassword_ThrowsInvalidCredentials() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        veterinarian.setPassword("secret-password");
        veterinarian.setActive(true);
        Mockito.when(veterinarianRepository.findByEmail("laura@pethaven.test")).thenReturn(Optional.of(veterinarian));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.authenticate("laura@pethaven.test", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    public void veterinarianService_authenticate_UnknownEmail_ThrowsInvalidCredentials() {
        // Arrange
        Mockito.when(veterinarianRepository.findByEmail("nadie@pethaven.test")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.authenticate("nadie@pethaven.test", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    public void veterinarianService_authenticate_InactiveVeterinarianWithCorrectPassword_ThrowsAccountDisabled() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        veterinarian.setPassword("secret-password");
        veterinarian.setActive(false);
        Mockito.when(veterinarianRepository.findByEmail("laura@pethaven.test")).thenReturn(Optional.of(veterinarian));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.authenticate("laura@pethaven.test", "secret-password"))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessage("La cuenta está desactivada.");
    }

    @Test
    public void veterinarianService_authenticate_InactiveVeterinarianWithWrongPassword_ThrowsAccountDisabled() {
        // Arrange
        Veterinarian veterinarian = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        veterinarian.setPassword("secret-password");
        veterinarian.setActive(false);
        Mockito.when(veterinarianRepository.findByEmail("laura@pethaven.test")).thenReturn(Optional.of(veterinarian));

        // Act + Assert
        // El estado activo se comprueba antes que la contraseña.
        Assertions.assertThatThrownBy(() -> veterinarianService.authenticate("laura@pethaven.test", "wrong-password"))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessage("La cuenta está desactivada.");
    }
}
