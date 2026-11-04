package io.github.pethaven.service;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.OwnerMapper;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.TreatmentRepository;
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

import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el OwnerMapper es real y los repositorios son simulados, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class OwnerServiceMockTest {

    @Mock
    private OwnerRepository ownerRepository;
    @Mock
    private TreatmentRepository treatmentRepository;

    private OwnerServiceImpl ownerService;

    @BeforeEach
    public void setUp() {
        ownerService = new OwnerServiceImpl(ownerRepository, new OwnerMapper(), treatmentRepository);
    }

    // ---------- Helpers ----------

    private Owner owner(Long id, String document, String name, String email, String phone) {
        Owner owner = RepositoryTestData.owner();
        owner.setId(id);
        owner.setDocument(document);
        owner.setName(name);
        owner.setEmail(email);
        owner.setPhone(phone);
        return owner;
    }

    private OwnerRequest createRequest() {
        return new OwnerRequest("DOC-900", "Ana Torres", "ana.torres@pethaven.test", "plain-password", "3001234567");
    }

    // ---------- getOwnerEntity ----------

    @Test
    public void ownerService_getOwnerEntity_ExistingId_ReturnsEntity() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Act
        Owner result = ownerService.getOwnerEntity(1L);

        // Assert
        Assertions.assertThat(result).isSameAs(owner);
        Mockito.verify(ownerRepository).findById(1L);
    }

    @Test
    public void ownerService_getOwnerEntity_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.getOwnerEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con id: '99' no fue encontrado");
    }

    // ---------- getAllOwners ----------

    @Test
    public void ownerService_getAllOwners_TwoOwners_ReturnsMappedResponsesInOrder() {
        // Arrange
        Owner first = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001112233");
        Owner second = owner(2L, "DOC-200", "Beto Ruiz", "beto@pethaven.test", "3004445566");
        Mockito.when(ownerRepository.findAll()).thenReturn(List.of(first, second));

        // Act
        List<OwnerResponse> result = ownerService.getAllOwners();

        // Assert
        Assertions.assertThat(result).containsExactly(
                new OwnerResponse(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001112233"),
                new OwnerResponse(2L, "DOC-200", "Beto Ruiz", "beto@pethaven.test", "3004445566"));
    }

    @Test
    public void ownerService_getAllOwners_NoOwners_ReturnsEmptyList() {
        // Arrange
        Mockito.when(ownerRepository.findAll()).thenReturn(List.of());

        // Act
        List<OwnerResponse> result = ownerService.getAllOwners();

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    // ---------- getOwnerById ----------

    @Test
    public void ownerService_getOwnerById_ExistingId_ReturnsMappedResponse() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Act
        OwnerResponse result = ownerService.getOwnerById(1L);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new OwnerResponse(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567"));
    }

    @Test
    public void ownerService_getOwnerById_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.getOwnerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con id: '99' no fue encontrado");
    }

    // ---------- getOwnerByDocument ----------

    @Test
    public void ownerService_getOwnerByDocument_ExistingDocument_ReturnsMappedResponse() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        Mockito.when(ownerRepository.findByDocument("DOC-100")).thenReturn(Optional.of(owner));

        // Act
        OwnerResponse result = ownerService.getOwnerByDocument("DOC-100");

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new OwnerResponse(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567"));
    }

    @Test
    public void ownerService_getOwnerByDocument_NonExistingDocument_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(ownerRepository.findByDocument("DOC-404")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.getOwnerByDocument("DOC-404"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con document: 'DOC-404' no fue encontrado");
    }

    // ---------- getOwnerByEmail ----------

    @Test
    public void ownerService_getOwnerByEmail_ExistingEmail_ReturnsMappedResponse() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        Mockito.when(ownerRepository.findByEmail("ana@pethaven.test")).thenReturn(Optional.of(owner));

        // Act
        OwnerResponse result = ownerService.getOwnerByEmail("ana@pethaven.test");

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new OwnerResponse(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567"));
    }

    @Test
    public void ownerService_getOwnerByEmail_NonExistingEmail_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(ownerRepository.findByEmail("nadie@pethaven.test")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.getOwnerByEmail("nadie@pethaven.test"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con email: 'nadie@pethaven.test' no fue encontrado");
    }

    // ---------- createOwner ----------

    @Test
    public void ownerService_createOwner_ValidRequest_SavesEntityAndReturnsSavedResponse() {
        // Arrange
        OwnerRequest request = createRequest();
        Mockito.when(ownerRepository.existsByDocument("DOC-900")).thenReturn(false);
        Mockito.when(ownerRepository.existsByEmail("ana.torres@pethaven.test")).thenReturn(false);
        // Simula la asignación del id que hace la base de datos al guardar.
        Mockito.when(ownerRepository.save(ArgumentMatchers.any(Owner.class)))
                .thenAnswer(invocation -> {
                    Owner toSave = invocation.getArgument(0);
                    toSave.setId(10L);
                    return toSave;
                });

        // Act
        OwnerResponse result = ownerService.createOwner(request);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new OwnerResponse(10L, "DOC-900", "Ana Torres", "ana.torres@pethaven.test", "3001234567"));

        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        Mockito.verify(ownerRepository).save(captor.capture());
        Owner saved = captor.getValue();
        Assertions.assertThat(saved.getDocument()).isEqualTo("DOC-900");
        Assertions.assertThat(saved.getName()).isEqualTo("Ana Torres");
        Assertions.assertThat(saved.getEmail()).isEqualTo("ana.torres@pethaven.test");
        Assertions.assertThat(saved.getPassword()).isEqualTo("plain-password");
        Assertions.assertThat(saved.getPhone()).isEqualTo("3001234567");

        // La creación comprueba unicidad por documento y correo sin excluir ningún id.
        Mockito.verify(ownerRepository).existsByDocument("DOC-900");
        Mockito.verify(ownerRepository).existsByEmail("ana.torres@pethaven.test");
        Mockito.verify(ownerRepository, Mockito.never())
                .existsByDocumentAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
        Mockito.verify(ownerRepository, Mockito.never())
                .existsByEmailAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void ownerService_createOwner_DuplicateDocument_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        OwnerRequest request = createRequest();
        Mockito.when(ownerRepository.existsByDocument("DOC-900")).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.createOwner(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un dueño con el documento 'DOC-900'.");
        Mockito.verify(ownerRepository, Mockito.never()).save(ArgumentMatchers.any(Owner.class));
        // El documento ya es duplicado: no hace falta comprobar el correo.
        Mockito.verify(ownerRepository, Mockito.never()).existsByEmail(ArgumentMatchers.anyString());
    }

    @Test
    public void ownerService_createOwner_DuplicateEmail_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        OwnerRequest request = createRequest();
        Mockito.when(ownerRepository.existsByDocument("DOC-900")).thenReturn(false);
        Mockito.when(ownerRepository.existsByEmail("ana.torres@pethaven.test")).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.createOwner(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un dueño con el correo 'ana.torres@pethaven.test'.");
        Mockito.verify(ownerRepository, Mockito.never()).save(ArgumentMatchers.any(Owner.class));
    }

    // ---------- updateOwner ----------

    @Test
    public void ownerService_updateOwner_ValidRequestWithoutPassword_AppliesFieldsAndKeepsPasswordAndPets() {
        // Arrange
        Owner existing = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        existing.setPassword("old-password");
        Pet pet = RepositoryTestData.pet(existing);
        existing.getPets().add(pet);
        OwnerUpdateRequest request = new OwnerUpdateRequest(
                "DOC-101", "Ana Torres Ruiz", "ana.ruiz@pethaven.test", null, "3112223344");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(ownerRepository.existsByDocumentAndIdNot("DOC-101", 1L)).thenReturn(false);
        Mockito.when(ownerRepository.existsByEmailAndIdNot("ana.ruiz@pethaven.test", 1L)).thenReturn(false);
        Mockito.when(ownerRepository.save(ArgumentMatchers.any(Owner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OwnerResponse result = ownerService.updateOwner(1L, request);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new OwnerResponse(1L, "DOC-101", "Ana Torres Ruiz", "ana.ruiz@pethaven.test", "3112223344"));

        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        Mockito.verify(ownerRepository).save(captor.capture());
        Owner saved = captor.getValue();
        // Se actualiza la misma instancia persistida, sin crear una entidad nueva.
        Assertions.assertThat(saved).isSameAs(existing);
        Assertions.assertThat(saved.getDocument()).isEqualTo("DOC-101");
        Assertions.assertThat(saved.getName()).isEqualTo("Ana Torres Ruiz");
        Assertions.assertThat(saved.getEmail()).isEqualTo("ana.ruiz@pethaven.test");
        Assertions.assertThat(saved.getPhone()).isEqualTo("3112223344");
        // Sin contraseña en la petición se conserva la actual.
        Assertions.assertThat(saved.getPassword()).isEqualTo("old-password");
        // Las mascotas existentes siguen asociadas a la entidad guardada.
        Assertions.assertThat(saved.getPets()).containsExactly(pet);

        // La actualización excluye al propio dueño de las comprobaciones de unicidad.
        Mockito.verify(ownerRepository).existsByDocumentAndIdNot("DOC-101", 1L);
        Mockito.verify(ownerRepository).existsByEmailAndIdNot("ana.ruiz@pethaven.test", 1L);
        Mockito.verify(ownerRepository, Mockito.never()).existsByDocument(ArgumentMatchers.anyString());
        Mockito.verify(ownerRepository, Mockito.never()).existsByEmail(ArgumentMatchers.anyString());
    }

    @Test
    public void ownerService_updateOwner_ValidRequestWithPassword_ReplacesPassword() {
        // Arrange
        Owner existing = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        existing.setPassword("old-password");
        OwnerUpdateRequest request = new OwnerUpdateRequest(
                "DOC-100", "Ana Torres", "ana@pethaven.test", "new-password", "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(ownerRepository.existsByDocumentAndIdNot("DOC-100", 1L)).thenReturn(false);
        Mockito.when(ownerRepository.existsByEmailAndIdNot("ana@pethaven.test", 1L)).thenReturn(false);
        Mockito.when(ownerRepository.save(ArgumentMatchers.any(Owner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ownerService.updateOwner(1L, request);

        // Assert
        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        Mockito.verify(ownerRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getPassword()).isEqualTo("new-password");
    }

    @Test
    public void ownerService_updateOwner_NonExistingId_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        OwnerUpdateRequest request = new OwnerUpdateRequest(
                "DOC-101", "Ana Torres Ruiz", "ana.ruiz@pethaven.test", null, "3112223344");
        Mockito.when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.updateOwner(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con id: '99' no fue encontrado");
        Mockito.verify(ownerRepository, Mockito.never()).save(ArgumentMatchers.any(Owner.class));
        Mockito.verify(ownerRepository, Mockito.never())
                .existsByDocumentAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void ownerService_updateOwner_DuplicateDocument_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Owner existing = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        OwnerUpdateRequest request = new OwnerUpdateRequest(
                "DOC-200", "Ana Torres", "ana@pethaven.test", null, "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(ownerRepository.existsByDocumentAndIdNot("DOC-200", 1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.updateOwner(1L, request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un dueño con el documento 'DOC-200'.");
        Mockito.verify(ownerRepository, Mockito.never()).save(ArgumentMatchers.any(Owner.class));
        Mockito.verify(ownerRepository, Mockito.never())
                .existsByEmailAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void ownerService_updateOwner_DuplicateEmail_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Owner existing = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        OwnerUpdateRequest request = new OwnerUpdateRequest(
                "DOC-100", "Ana Torres", "tomado@pethaven.test", null, "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(ownerRepository.existsByDocumentAndIdNot("DOC-100", 1L)).thenReturn(false);
        Mockito.when(ownerRepository.existsByEmailAndIdNot("tomado@pethaven.test", 1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.updateOwner(1L, request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un dueño con el correo 'tomado@pethaven.test'.");
        Mockito.verify(ownerRepository, Mockito.never()).save(ArgumentMatchers.any(Owner.class));
    }

    // ---------- deleteOwnerById ----------

    @Test
    public void ownerService_deleteOwnerById_NonExistingId_ThrowsResourceNotFoundAndDoesNotDelete() {
        // Arrange
        Mockito.when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.deleteOwnerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Owner' con id: '99' no fue encontrado");
        Mockito.verify(ownerRepository, Mockito.never()).deleteById(ArgumentMatchers.anyLong());
        Mockito.verifyNoInteractions(treatmentRepository);
    }

    @Test
    public void ownerService_deleteOwnerById_OwnerWithTreatedPets_ThrowsIllegalOperationAndDoesNotDelete() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
        Mockito.when(treatmentRepository.existsByPetOwnerId(1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.deleteOwnerById(1L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No se puede eliminar a 'Ana Torres' porque una o más de sus mascotas tienen "
                        + "tratamientos registrados.");
        Mockito.verify(ownerRepository, Mockito.never()).deleteById(ArgumentMatchers.anyLong());
    }

    @Test
    public void ownerService_deleteOwnerById_OwnerWithoutTreatments_DeletesOwner() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        Mockito.when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
        Mockito.when(treatmentRepository.existsByPetOwnerId(1L)).thenReturn(false);

        // Act
        ownerService.deleteOwnerById(1L);

        // Assert
        Mockito.verify(ownerRepository).deleteById(1L);
    }

    // ---------- authenticate ----------
    // Comportamiento provisional: la contraseña se compara en texto plano. Estos tests fijan
    // ese comportamiento hasta que se implemente Spring Security + BCrypt + JWT.

    @Test
    public void ownerService_authenticate_CorrectPassword_ReturnsOwner() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        owner.setPassword("secret-password");
        Mockito.when(ownerRepository.findByEmail("ana@pethaven.test")).thenReturn(Optional.of(owner));

        // Act
        Owner result = ownerService.authenticate("ana@pethaven.test", "secret-password");

        // Assert
        Assertions.assertThat(result).isSameAs(owner);
    }

    @Test
    public void ownerService_authenticate_WrongPassword_ThrowsInvalidCredentials() {
        // Arrange
        Owner owner = owner(1L, "DOC-100", "Ana Torres", "ana@pethaven.test", "3001234567");
        owner.setPassword("secret-password");
        Mockito.when(ownerRepository.findByEmail("ana@pethaven.test")).thenReturn(Optional.of(owner));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.authenticate("ana@pethaven.test", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    public void ownerService_authenticate_UnknownEmail_ThrowsInvalidCredentials() {
        // Arrange
        Mockito.when(ownerRepository.findByEmail("nadie@pethaven.test")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> ownerService.authenticate("nadie@pethaven.test", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }
}
