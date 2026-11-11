package io.github.pethaven.service;

import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.VeterinarianMapper;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.repository.UserRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el VeterinarianMapper y el UserAccountService son reales (con BCrypt)
// y los repositorios son simulados, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class VeterinarianServiceMockTest {

    // Coste bajo de BCrypt para que los tests sean rápidos; el algoritmo es el mismo.
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    private VeterinarianServiceImpl veterinarianService;

    @BeforeEach
    public void setUp() {
        UserAccountService userAccountService = new UserAccountServiceImpl(userRepository, roleRepository, PASSWORD_ENCODER);
        veterinarianService = new VeterinarianServiceImpl(veterinarianRepository, new VeterinarianMapper(), userAccountService);
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
        veterinarian.setUser(account(email, "secret-password"));
        return veterinarian;
    }

    private UserEntity account(String username, String rawPassword) {
        UserEntity account = new UserEntity();
        account.setUsername(username);
        account.setPassword(PASSWORD_ENCODER.encode(rawPassword));
        account.setRoles(new ArrayList<>(List.of(new Role(Role.VETERINARIAN))));
        return account;
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
        Mockito.when(roleRepository.findByName(Role.VETERINARIAN)).thenReturn(Optional.of(new Role(Role.VETERINARIAN)));
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
        Assertions.assertThat(saved.getSpecialty()).isEqualTo("Cuidados Críticos");
        // La cuenta de login usa el email como username y guarda la contraseña hasheada.
        UserEntity account = saved.getUser();
        Assertions.assertThat(account.getUsername()).isEqualTo("laura.gomez@pethaven.test");
        Assertions.assertThat(account.getPassword()).isNotEqualTo("plain-password");
        Assertions.assertThat(PASSWORD_ENCODER.matches("plain-password", account.getPassword())).isTrue();
        Assertions.assertThat(account.getRoles()).extracting(Role::getName).containsExactly(Role.VETERINARIAN);
        // Veterinario nuevo activo: su cuenta de login nace habilitada.
        Assertions.assertThat(saved.isActive()).isTrue();
        Assertions.assertThat(account.isEnabled()).isTrue();
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

    @Test
    public void veterinarianService_createVeterinarian_UsernameTakenByAnotherAccount_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        // El correo está libre entre los veterinarios, pero otra cuenta (p. ej. un dueño) ya lo usa como username.
        VeterinarianRequest request = createRequest();
        Mockito.when(veterinarianRepository.existsByDocument("VET-900")).thenReturn(false);
        Mockito.when(veterinarianRepository.existsByEmail("laura.gomez@pethaven.test")).thenReturn(false);
        Mockito.when(userRepository.existsByUsername("laura.gomez@pethaven.test")).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> veterinarianService.createVeterinarian(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe una cuenta con el usuario 'laura.gomez@pethaven.test'.");
        Mockito.verify(veterinarianRepository, Mockito.never()).save(ArgumentMatchers.any(Veterinarian.class));
    }

    // ---------- updateVeterinarian ----------

    @Test
    public void veterinarianService_updateVeterinarian_ValidRequestWithoutPassword_AppliesFieldsAndKeepsStateAndTreatments() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        String previousHash = existing.getUser().getPassword();
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
        // El username de la cuenta sigue al email del perfil.
        Assertions.assertThat(saved.getUser().getUsername()).isEqualTo("laura.ruiz@pethaven.test");
        // Sin contraseña en la petición se conserva la actual.
        Assertions.assertThat(saved.getUser().getPassword()).isEqualTo(previousHash);
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
        UserEntity account = captor.getValue().getUser();
        Assertions.assertThat(PASSWORD_ENCODER.matches("new-password", account.getPassword())).isTrue();
        Assertions.assertThat(PASSWORD_ENCODER.matches("secret-password", account.getPassword())).isFalse();
        // El email no cambió: no se consulta la disponibilidad del username.
        Mockito.verify(userRepository, Mockito.never()).existsByUsername(ArgumentMatchers.anyString());
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
        // La cuenta de login se deshabilita con el perfil: no puede iniciar sesión y sus tokens dejan de servir.
        Assertions.assertThat(captor.getValue().getUser().isEnabled()).isFalse();
    }

    @Test
    public void veterinarianService_switchVeterinarianActiveStatus_InactiveVeterinarianReactivated_ReturnsActiveResponse() {
        // Arrange
        Veterinarian existing = veterinarian(
                1L, "VET-100", "Dra. Laura Gómez", "laura@pethaven.test", "Cuidados Críticos",
                "https://pethaven.test/photos/laura.jpg");
        existing.setActive(false);
        existing.getUser().setEnabled(false);
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
        Assertions.assertThat(captor.getValue().getUser().isEnabled()).isTrue();
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
}
