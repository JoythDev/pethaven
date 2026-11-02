package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.TreatmentMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: no hay Spring ni base de datos. Todas las dependencias
// (repositorios, mapper y otros servicios) son falsas y se les dice qué responder con when(...).
@ExtendWith(MockitoExtension.class)
public class TreatmentServiceMockTest {

    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private TreatmentMapper treatmentMapper;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private PetService petService;
    @Mock
    private VeterinarianService veterinarianService;
    @Mock
    private DrugRepository drugRepository;
    @Mock
    private TreatmentDrugService treatmentDrugService;

    @InjectMocks
    private TreatmentServiceImpl treatmentService;

    private Pet pet;
    private Veterinarian vet;

    @BeforeEach
    public void setUp() {
        pet = RepositoryTestData.pet(RepositoryTestData.owner());
        pet.setId(1L);
        vet = RepositoryTestData.veterinarian();
        vet.setId(2L);
    }

    // ---------- Helpers ----------

    private TreatmentRequest request(Evolution evolution, String diagnosis) {
        return new TreatmentRequest(
                1L, 2L, null, 12.4, diagnosis, evolution, null,
                List.of(new TreatmentDrugRequest(5L, 2)));
    }

    // Lo que devolvería el mapper al convertir la petición en entidad.
    private Treatment treatmentFrom(Evolution evolution, String diagnosis) {
        return Treatment.builder()
                .weight(12.4)
                .diagnosis(diagnosis)
                .evolution(evolution)
                .build();
    }

    private TreatmentResponse response(Long id) {
        return new TreatmentResponse(
                id, LocalDateTime.of(2026, 11, 2, 10, 0), 1L, "Pet", 2L, "Vet",
                12.4, "Otitis externa", null, null, List.of());
    }

    private void stubPetAndVet() {
        Mockito.when(petService.getPetEntity(1L)).thenReturn(pet);
        Mockito.when(veterinarianService.getVeterinarianEntity(2L)).thenReturn(vet);
    }

    // ---------- createTreatment ----------

    @Test
    public void treatmentService_createTreatment_ValidAdmission_SavesTreatmentAndUpdatesPetAndVeterinarian() {
        // Arrange
        TreatmentRequest request = request(null, "Otitis externa");
        TreatmentResponse expected = response(10L);
        stubPetAndVet();
        Mockito.when(drugRepository.existsByUnitsAvailableGreaterThan(0)).thenReturn(true);
        Mockito.when(treatmentMapper.toEntity(request)).thenReturn(treatmentFrom(null, "Otitis externa"));
        Mockito.when(treatmentRepository.save(ArgumentMatchers.any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(treatmentMapper.toResponse(ArgumentMatchers.any(Treatment.class))).thenReturn(expected);

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Assertions.assertThat(result).isSameAs(expected);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        Mockito.verify(treatmentRepository).save(captor.capture());
        Treatment saved = captor.getValue();
        Assertions.assertThat(saved.getPet()).isSameAs(pet);
        Assertions.assertThat(saved.getVeterinarian()).isSameAs(vet);
        Assertions.assertThat(saved.getDate()).isNotNull();

        Mockito.verify(treatmentDrugService).administerDrugs(saved, request.drugs());
        Mockito.verify(veterinarianRepository).save(vet);
        Assertions.assertThat(vet.getAttentions()).isEqualTo(1);
        Assertions.assertThat(pet.getDisease()).isEqualTo("Otitis externa");
        Assertions.assertThat(pet.getWeight()).isEqualTo(12.4);
    }

    @Test
    public void treatmentService_createTreatment_InactivePet_ThrowsIllegalOperationAndSavesNothing() {
        // Arrange
        pet.setActive(false);
        stubPetAndVet();
        TreatmentRequest request = request(null, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no está activa");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_InactiveVeterinarian_ThrowsIllegalOperationAndSavesNothing() {
        // Arrange
        vet.setActive(false);
        stubPetAndVet();
        TreatmentRequest request = request(null, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no está activo");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_NoDrugsInStock_ThrowsIllegalOperationAndSavesNothing() {
        // Arrange
        stubPetAndVet();
        Mockito.when(drugRepository.existsByUnitsAvailableGreaterThan(0)).thenReturn(false);
        TreatmentRequest request = request(null, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("No hay medicamentos disponibles");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    @Test
    public void treatmentService_createTreatment_NonExistingPet_ThrowsResourceNotFoundBeforeAnythingElse() {
        // Arrange
        Mockito.when(petService.getPetEntity(1L)).thenThrow(new ResourceNotFoundException("Pet", "id", 1L));
        TreatmentRequest request = request(null, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(ResourceNotFoundException.class);
        Mockito.verifyNoInteractions(veterinarianService, treatmentRepository, treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_StablePetWithEvolution_ThrowsIllegalOperation() {
        // Arrange: la mascota no tiene diagnóstico (es un ingreso), pero la petición trae evolución.
        stubPetAndVet();
        Mockito.when(drugRepository.existsByUnitsAvailableGreaterThan(0)).thenReturn(true);
        TreatmentRequest request = request(Evolution.IMPROVING, "Otitis externa");
        Mockito.when(treatmentMapper.toEntity(request)).thenReturn(treatmentFrom(Evolution.IMPROVING, "Otitis externa"));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no lleva evolución");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    @Test
    public void treatmentService_createTreatment_RecoveredEvolution_ThrowsIllegalOperation() {
        // Arrange: RECOVERED solo lo usa el alta, nunca un tratamiento.
        pet.setDisease("Gastroenteritis");
        stubPetAndVet();
        Mockito.when(drugRepository.existsByUnitsAvailableGreaterThan(0)).thenReturn(true);
        TreatmentRequest request = request(Evolution.RECOVERED, "Gastroenteritis");
        Mockito.when(treatmentMapper.toEntity(request)).thenReturn(treatmentFrom(Evolution.RECOVERED, "Gastroenteritis"));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("alta");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    @Test
    public void treatmentService_createTreatment_StableEvolutionWithDifferentDiagnosis_ThrowsIllegalOperation() {
        // Arrange: con "sin cambios" el diagnóstico debe seguir siendo el mismo.
        pet.setDisease("Gastroenteritis");
        stubPetAndVet();
        Mockito.when(drugRepository.existsByUnitsAvailableGreaterThan(0)).thenReturn(true);
        TreatmentRequest request = request(Evolution.STABLE, "Otra cosa");
        Mockito.when(treatmentMapper.toEntity(request)).thenReturn(treatmentFrom(Evolution.STABLE, "Otra cosa"));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("sin cambios");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    // ---------- dischargePet ----------

    @Test
    public void treatmentService_dischargePet_SickPet_SavesRecoveredRecordAndClearsDisease() {
        // Arrange
        pet.setDisease("Gastroenteritis");
        TreatmentResponse expected = response(11L);
        stubPetAndVet();
        Mockito.when(treatmentRepository.save(ArgumentMatchers.any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(treatmentMapper.toResponse(ArgumentMatchers.any(Treatment.class))).thenReturn(expected);

        // Act
        TreatmentResponse result = treatmentService.dischargePet(1L, 2L);

        // Assert
        Assertions.assertThat(result).isSameAs(expected);
        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        Mockito.verify(treatmentRepository).save(captor.capture());
        Treatment saved = captor.getValue();
        Assertions.assertThat(saved.getEvolution()).isEqualTo(Evolution.RECOVERED);
        Assertions.assertThat(saved.getDiagnosis()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(saved.getWeight()).isEqualTo(pet.getWeight());
        Assertions.assertThat(saved.getTreatmentDrugs()).isEmpty();
        Assertions.assertThat(pet.getDisease()).isNull();
        // El alta no suma atenciones ni administra medicamentos.
        Mockito.verifyNoInteractions(veterinarianRepository, treatmentDrugService);
    }

    @Test
    public void treatmentService_dischargePet_StablePet_ThrowsIllegalOperation() {
        // Arrange: la mascota de setUp no tiene diagnóstico.
        stubPetAndVet();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.dischargePet(1L, 2L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("ya está estable");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    @Test
    public void treatmentService_dischargePet_InactiveVeterinarian_ThrowsIllegalOperation() {
        // Arrange
        pet.setDisease("Gastroenteritis");
        vet.setActive(false);
        stubPetAndVet();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.dischargePet(1L, 2L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no puede dar de alta");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    // ---------- Consultas ----------

    @Test
    public void treatmentService_getTreatmentById_ExistingId_ReturnsMappedResponse() {
        // Arrange
        Treatment treatment = treatmentFrom(null, "Otitis externa");
        TreatmentResponse expected = response(1L);
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        Mockito.when(treatmentMapper.toResponse(treatment)).thenReturn(expected);

        // Act
        TreatmentResponse result = treatmentService.getTreatmentById(1L);

        // Assert
        Assertions.assertThat(result).isSameAs(expected);
    }

    @Test
    public void treatmentService_getTreatmentById_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(treatmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.getTreatmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        Mockito.verifyNoInteractions(treatmentMapper);
    }

    @Test
    public void treatmentService_getAllTreatments_TwoTreatments_ReturnsTwoMappedResponses() {
        // Arrange
        Treatment first = treatmentFrom(null, "Otitis externa");
        Treatment second = treatmentFrom(Evolution.IMPROVING, "Otitis externa");
        Mockito.when(treatmentRepository.findAll()).thenReturn(List.of(first, second));
        Mockito.when(treatmentMapper.toResponse(first)).thenReturn(response(1L));
        Mockito.when(treatmentMapper.toResponse(second)).thenReturn(response(2L));

        // Act
        List<TreatmentResponse> result = treatmentService.getAllTreatments();

        // Assert
        Assertions.assertThat(result).extracting(TreatmentResponse::id).containsExactly(1L, 2L);
    }

    @Test
    public void treatmentService_getTreatmentsByPetId_PetWithTreatment_ReturnsMappedResponses() {
        // Arrange
        Treatment treatment = treatmentFrom(null, "Otitis externa");
        Mockito.when(treatmentRepository.findByPetId(1L)).thenReturn(List.of(treatment));
        Mockito.when(treatmentMapper.toResponse(treatment)).thenReturn(response(1L));

        // Act
        List<TreatmentResponse> result = treatmentService.getTreatmentsByPetId(1L);

        // Assert
        Assertions.assertThat(result).hasSize(1);
        Mockito.verify(treatmentRepository).findByPetId(1L);
    }
}
