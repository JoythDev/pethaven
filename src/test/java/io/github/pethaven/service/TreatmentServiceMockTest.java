package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.TreatmentDrugMapper;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el TreatmentMapper es real y los repositorios y demás servicios son simulados.
@ExtendWith(MockitoExtension.class)
public class TreatmentServiceMockTest {

    @Mock
    private TreatmentRepository treatmentRepository;
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

    private TreatmentServiceImpl treatmentService;

    private Pet pet;
    private Veterinarian vet;
    private Drug drug;

    @BeforeEach
    public void setUp() {
        pet = RepositoryTestData.pet(RepositoryTestData.owner());
        pet.setId(1L);
        vet = RepositoryTestData.veterinarian();
        vet.setId(2L);
        drug = RepositoryTestData.drug("Amoxicilina 250mg");
        drug.setId(5L);
        // El servicio se arma a mano para poder inyectar los mappers reales junto a los colaboradores simulados.
        treatmentService = new TreatmentServiceImpl(
                treatmentRepository,
                new TreatmentMapper(new TreatmentDrugMapper()),
                veterinarianRepository,
                petService,
                veterinarianService,
                drugRepository,
                treatmentDrugService);
    }

    // ---------- Helpers ----------

    private TreatmentRequest request(Evolution evolution, String diagnosis) {
        return new TreatmentRequest(
                1L, 2L, null, 12.4, diagnosis, evolution, null,
                List.of(new TreatmentDrugRequest(5L, 2)));
    }

    private Treatment treatment(
            Long id,
            Pet treatmentPet,
            Veterinarian treatmentVet,
            LocalDateTime date,
            Double weight,
            String diagnosis,
            Evolution evolution,
            String notes) {
        return Treatment.builder()
                .id(id)
                .pet(treatmentPet)
                .veterinarian(treatmentVet)
                .date(date)
                .weight(weight)
                .diagnosis(diagnosis)
                .evolution(evolution)
                .notes(notes)
                .build();
    }

    private void stubPetAndVet() {
        Mockito.when(petService.getPetEntity(1L)).thenReturn(pet);
        Mockito.when(veterinarianService.getVeterinarianEntity(2L)).thenReturn(vet);
    }

    private void stubDrugsInStock() {
        Mockito.when(drugRepository.existsByUnitsAvailableGreaterThan(0)).thenReturn(true);
    }

    // Simula la asignación del id que hace la base de datos al guardar y devuelve la misma instancia.
    private void stubTreatmentSaveAssigningId(Long id) {
        Mockito.when(treatmentRepository.save(ArgumentMatchers.any(Treatment.class)))
                .thenAnswer(invocation -> {
                    Treatment toSave = invocation.getArgument(0);
                    toSave.setId(id);
                    return toSave;
                });
    }

    // Captura la entidad que el servicio entrega al repositorio y exige que se haya guardado una sola vez.
    private Treatment savedTreatment() {
        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        Mockito.verify(treatmentRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- createTreatment ----------

    @Test
    public void treatmentService_createTreatment_ValidAdmission_SavesTreatmentAndUpdatesPetAndVeterinarian() {
        // Arrange
        TreatmentRequest request = request(null, "Otitis externa");
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(100L);
        // El servicio de medicamentos está simulado: se imita que agrega el renglón al tratamiento guardado.
        Mockito.doAnswer(invocation -> {
            Treatment toAdminister = invocation.getArgument(0);
            TreatmentDrug row = RepositoryTestData.treatmentDrug(
                    toAdminister, drug, 2, new BigDecimal("1.00"), new BigDecimal("2.00"));
            row.setId(20L);
            toAdminister.getTreatmentDrugs().add(row);
            return null;
        }).when(treatmentDrugService).administerDrugs(
                ArgumentMatchers.any(Treatment.class), ArgumentMatchers.anyList());

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getPet()).isSameAs(pet);
        Assertions.assertThat(saved.getVeterinarian()).isSameAs(vet);
        Assertions.assertThat(saved.getDate()).isNotNull();
        Assertions.assertThat(saved.getWeight()).isEqualTo(12.4);
        Assertions.assertThat(saved.getDiagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(saved.getEvolution()).isNull();
        Assertions.assertThat(saved.getNotes()).isNull();

        // La respuesta se arma con el registro guardado (id incluido) y sus renglones de medicamentos.
        Assertions.assertThat(result.id()).isEqualTo(100L);
        Assertions.assertThat(result.date()).isEqualTo(saved.getDate());
        Assertions.assertThat(result.petId()).isEqualTo(1L);
        Assertions.assertThat(result.petName()).isEqualTo(pet.getName());
        Assertions.assertThat(result.veterinarianId()).isEqualTo(2L);
        Assertions.assertThat(result.veterinarianName()).isEqualTo(vet.getName());
        Assertions.assertThat(result.weight()).isEqualTo(12.4);
        Assertions.assertThat(result.diagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(result.evolution()).isNull();
        Assertions.assertThat(result.notes()).isNull();
        Assertions.assertThat(result.drugs()).containsExactly(
                new TreatmentDrugResponse(20L, 5L, "Amoxicilina 250mg", 2,
                        new BigDecimal("1.00"), new BigDecimal("2.00")));

        Mockito.verify(treatmentDrugService)
                .administerDrugs(Mockito.same(saved), Mockito.same(request.drugs()));
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
                .hasMessage("La mascota '" + pet.getName() + "' no está activa:"
                        + " no se le pueden registrar tratamientos.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        // La validación corta antes de consultar el inventario y antes de administrar medicamentos.
        Mockito.verifyNoInteractions(drugRepository, treatmentDrugService);
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
                .hasMessage("El veterinario '" + vet.getName() + "' no está activo:"
                        + " no puede registrar atenciones.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(drugRepository, treatmentDrugService);
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
                .hasMessage("No hay medicamentos disponibles en el inventario:"
                        + " no se pueden registrar tratamientos.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_NonExistingPet_ThrowsResourceNotFoundBeforeAnythingElse() {
        // Arrange
        Mockito.when(petService.getPetEntity(1L)).thenThrow(new ResourceNotFoundException("Pet", "id", 1L));
        TreatmentRequest request = request(null, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Pet' con id: '1' no fue encontrado");
        Mockito.verifyNoInteractions(veterinarianService, drugRepository, treatmentRepository, treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_StablePetWithEvolution_ThrowsIllegalOperation() {
        // Arrange: la mascota no tiene diagnóstico (es un ingreso), pero la petición trae evolución.
        stubPetAndVet();
        stubDrugsInStock();
        TreatmentRequest request = request(Evolution.IMPROVING, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("La mascota '" + pet.getName() + "' está estable: es un ingreso y no lleva evolución.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_RecoveredEvolution_ThrowsIllegalOperation() {
        // Arrange: RECOVERED solo lo usa el alta, nunca un tratamiento.
        pet.setDisease("Gastroenteritis");
        stubPetAndVet();
        stubDrugsInStock();
        TreatmentRequest request = request(Evolution.RECOVERED, "Gastroenteritis");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Un tratamiento siempre formula medicamentos:"
                        + " para dejar estable a la mascota dale el alta.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_StableEvolutionWithDifferentDiagnosis_ThrowsIllegalOperation() {
        // Arrange: con "sin cambios" el diagnóstico debe seguir siendo el mismo.
        pet.setDisease("Gastroenteritis");
        stubPetAndVet();
        stubDrugsInStock();
        TreatmentRequest request = request(Evolution.STABLE, "Otra cosa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Con evolución 'sin cambios' el diagnóstico debe seguir siendo 'Gastroenteritis'.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_SickPetWithoutEvolution_ThrowsIllegalOperationAndSavesNothing() {
        // Arrange: la mascota sigue en tratamiento, así que la evolución es obligatoria.
        pet.setDisease("Otitis externa");
        stubPetAndVet();
        stubDrugsInStock();
        TreatmentRequest request = request(null, "Otitis externa");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("La mascota '" + pet.getName()
                        + "' está en tratamiento por 'Otitis externa': indica su evolución.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentService_createTreatment_SickPetImprovingWithNewDiagnosis_SavesTreatmentAndUpdatesState() {
        // Arrange: la mejora permite cambiar el diagnóstico y la consulta actualiza el estado de la mascota.
        pet.setDisease("Otitis externa");
        TreatmentRequest request = request(Evolution.IMPROVING, "Dermatitis atópica");
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(101L);

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getPet()).isSameAs(pet);
        Assertions.assertThat(saved.getVeterinarian()).isSameAs(vet);
        Assertions.assertThat(saved.getDiagnosis()).isEqualTo("Dermatitis atópica");
        Assertions.assertThat(saved.getEvolution()).isEqualTo(Evolution.IMPROVING);
        Assertions.assertThat(saved.getWeight()).isEqualTo(12.4);

        Assertions.assertThat(pet.getWeight()).isEqualTo(12.4);
        Assertions.assertThat(pet.getDisease()).isEqualTo("Dermatitis atópica");

        // Un tratamiento sí es una atención del veterinario.
        Assertions.assertThat(vet.getAttentions()).isEqualTo(1);
        Mockito.verify(veterinarianRepository).save(vet);

        // Los medicamentos se administran sobre el tratamiento ya guardado y con la misma lista de la petición.
        Mockito.verify(treatmentDrugService)
                .administerDrugs(Mockito.same(saved), Mockito.same(request.drugs()));

        Assertions.assertThat(result.id()).isEqualTo(101L);
        Assertions.assertThat(result.diagnosis()).isEqualTo("Dermatitis atópica");
        Assertions.assertThat(result.evolution()).isEqualTo(Evolution.IMPROVING);
        Assertions.assertThat(result.drugs()).isEmpty();
    }

    @Test
    public void treatmentService_createTreatment_StableEvolutionWithSameDiagnosis_SavesTreatment() {
        // Arrange: con "sin cambios" el diagnóstico actual de la mascota se conserva.
        pet.setDisease("Gastroenteritis");
        TreatmentRequest request = request(Evolution.STABLE, "Gastroenteritis");
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(102L);

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getDiagnosis()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(saved.getEvolution()).isEqualTo(Evolution.STABLE);
        Assertions.assertThat(pet.getDisease()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(result.id()).isEqualTo(102L);
        Assertions.assertThat(result.evolution()).isEqualTo(Evolution.STABLE);
    }

    @Test
    public void treatmentService_createTreatment_RequestWithDate_KeepsRequestedDate() {
        // Arrange: la fecha de la consulta puede venir en la petición y no se reemplaza por "ahora".
        LocalDateTime date = LocalDateTime.of(2026, 10, 5, 9, 30);
        TreatmentRequest request = new TreatmentRequest(
                1L, 2L, date, 12.4, "Otitis externa", null, null,
                List.of(new TreatmentDrugRequest(5L, 2)));
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(103L);

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getDate()).isEqualTo(date);
        Assertions.assertThat(result.date()).isEqualTo(date);
    }

    @Test
    public void treatmentService_createTreatment_RequestWithoutDate_SetsCurrentDate() {
        // Arrange
        TreatmentRequest request = request(null, "Otitis externa");
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(104L);
        LocalDateTime before = LocalDateTime.now();

        // Act
        treatmentService.createTreatment(request);
        LocalDateTime after = LocalDateTime.now();

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getDate()).isNotNull().isBetween(before, after);
    }

    @Test
    public void treatmentService_createTreatment_UntrimmedDiagnosisAndBlankNotes_SavesTrimmedDiagnosisAndNullNotes() {
        // Arrange: el mapper real normaliza el diagnóstico y descarta las notas en blanco.
        TreatmentRequest request = new TreatmentRequest(
                1L, 2L, null, 12.4, "  Otitis externa  ", null, "   ",
                List.of(new TreatmentDrugRequest(5L, 2)));
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(105L);

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getDiagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(saved.getNotes()).isNull();
        Assertions.assertThat(pet.getDisease()).isEqualTo("Otitis externa");
        Assertions.assertThat(result.diagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(result.notes()).isNull();
    }

    @Test
    public void treatmentService_createTreatment_PaddedNotes_SavesTrimmedNotes() {
        // Arrange
        TreatmentRequest request = new TreatmentRequest(
                1L, 2L, null, 12.4, "Otitis externa", null, "  Control en 8 días  ",
                List.of(new TreatmentDrugRequest(5L, 2)));
        stubPetAndVet();
        stubDrugsInStock();
        stubTreatmentSaveAssigningId(106L);

        // Act
        TreatmentResponse result = treatmentService.createTreatment(request);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getNotes()).isEqualTo("Control en 8 días");
        Assertions.assertThat(result.notes()).isEqualTo("Control en 8 días");
    }

    // ---------- dischargePet ----------

    @Test
    public void treatmentService_dischargePet_SickPet_SavesRecoveredRecordAndClearsDisease() {
        // Arrange
        pet.setDisease("Gastroenteritis");
        stubPetAndVet();
        stubTreatmentSaveAssigningId(110L);

        // Act
        TreatmentResponse result = treatmentService.dischargePet(1L, 2L);

        // Assert
        Treatment saved = savedTreatment();
        Assertions.assertThat(saved.getPet()).isSameAs(pet);
        Assertions.assertThat(saved.getVeterinarian()).isSameAs(vet);
        Assertions.assertThat(saved.getDate()).isNotNull();
        Assertions.assertThat(saved.getWeight()).isEqualTo(pet.getWeight());
        Assertions.assertThat(saved.getDiagnosis()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(saved.getEvolution()).isEqualTo(Evolution.RECOVERED);
        Assertions.assertThat(saved.getNotes()).isEqualTo("Alta: la mascota queda estable.");
        Assertions.assertThat(saved.getTreatmentDrugs()).isEmpty();
        // La mascota vuelve a estar estable.
        Assertions.assertThat(pet.getDisease()).isNull();

        // La respuesta se arma con el registro de alta guardado.
        Assertions.assertThat(result.id()).isEqualTo(110L);
        Assertions.assertThat(result.date()).isEqualTo(saved.getDate());
        Assertions.assertThat(result.petId()).isEqualTo(1L);
        Assertions.assertThat(result.petName()).isEqualTo(pet.getName());
        Assertions.assertThat(result.veterinarianId()).isEqualTo(2L);
        Assertions.assertThat(result.veterinarianName()).isEqualTo(vet.getName());
        Assertions.assertThat(result.weight()).isEqualTo(10.5);
        Assertions.assertThat(result.diagnosis()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(result.evolution()).isEqualTo(Evolution.RECOVERED);
        Assertions.assertThat(result.notes()).isEqualTo("Alta: la mascota queda estable.");
        Assertions.assertThat(result.drugs()).isEmpty();

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
                .hasMessage("La mascota '" + pet.getName() + "' ya está estable.");
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
                .hasMessage("El veterinario '" + vet.getName() + "' no está activo: no puede dar de alta.");
        Mockito.verify(treatmentRepository, Mockito.never()).save(ArgumentMatchers.any(Treatment.class));
    }

    // ---------- Consultas ----------

    @Test
    public void treatmentService_getTreatmentById_ExistingId_ReturnsMappedResponse() {
        // Arrange
        LocalDateTime date = LocalDateTime.of(2026, 10, 12, 10, 0);
        Treatment treatment = treatment(
                7L, pet, vet, date, 11.8, "Otitis externa", Evolution.IMPROVING, "Menos inflamación");
        Mockito.when(treatmentRepository.findById(7L)).thenReturn(Optional.of(treatment));

        // Act
        TreatmentResponse result = treatmentService.getTreatmentById(7L);

        // Assert
        Assertions.assertThat(result.id()).isEqualTo(7L);
        Assertions.assertThat(result.date()).isEqualTo(date);
        Assertions.assertThat(result.petId()).isEqualTo(1L);
        Assertions.assertThat(result.petName()).isEqualTo(pet.getName());
        Assertions.assertThat(result.veterinarianId()).isEqualTo(2L);
        Assertions.assertThat(result.veterinarianName()).isEqualTo(vet.getName());
        Assertions.assertThat(result.weight()).isEqualTo(11.8);
        Assertions.assertThat(result.diagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(result.evolution()).isEqualTo(Evolution.IMPROVING);
        Assertions.assertThat(result.notes()).isEqualTo("Menos inflamación");
        Assertions.assertThat(result.drugs()).isEmpty();
        Mockito.verify(treatmentRepository).findById(7L);
    }

    @Test
    public void treatmentService_getTreatmentById_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(treatmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.getTreatmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Treatment' con id: '99' no fue encontrado");
        Mockito.verify(treatmentRepository).findById(99L);
        // Una consulta no debe tocar ningún otro colaborador.
        Mockito.verifyNoInteractions(petService, veterinarianService, veterinarianRepository, drugRepository,
                treatmentDrugService);
    }

    @Test
    public void treatmentService_getAllTreatments_TwoTreatments_ReturnsTwoMappedResponses() {
        // Arrange
        LocalDateTime firstDate = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime secondDate = LocalDateTime.of(2026, 10, 8, 9, 0);
        Treatment first = treatment(1L, pet, vet, firstDate, 10.5, "Otitis externa", null, null);
        Treatment second = treatment(2L, pet, vet, secondDate, 11.2, "Otitis externa", Evolution.IMPROVING, "Mejoría");
        Mockito.when(treatmentRepository.findAll()).thenReturn(List.of(first, second));

        // Act
        List<TreatmentResponse> result = treatmentService.getAllTreatments();

        // Assert
        Assertions.assertThat(result).containsExactly(
                new TreatmentResponse(1L, firstDate, 1L, pet.getName(), 2L, vet.getName(),
                        10.5, "Otitis externa", null, null, List.of()),
                new TreatmentResponse(2L, secondDate, 1L, pet.getName(), 2L, vet.getName(),
                        11.2, "Otitis externa", Evolution.IMPROVING, "Mejoría", List.of()));
        Mockito.verify(treatmentRepository).findAll();
    }

    @Test
    public void treatmentService_getTreatmentsByPetId_PetWithTreatment_ReturnsMappedResponses() {
        // Arrange
        LocalDateTime date = LocalDateTime.of(2026, 10, 1, 9, 0);
        Treatment treatment = treatment(1L, pet, vet, date, 10.5, "Otitis externa", null, null);
        Mockito.when(treatmentRepository.findByPetId(1L)).thenReturn(List.of(treatment));

        // Act
        List<TreatmentResponse> result = treatmentService.getTreatmentsByPetId(1L);

        // Assert
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).id()).isEqualTo(1L);
        Assertions.assertThat(result.get(0).date()).isEqualTo(date);
        Assertions.assertThat(result.get(0).petId()).isEqualTo(1L);
        Assertions.assertThat(result.get(0).petName()).isEqualTo(pet.getName());
        Assertions.assertThat(result.get(0).veterinarianId()).isEqualTo(2L);
        Assertions.assertThat(result.get(0).diagnosis()).isEqualTo("Otitis externa");
        Mockito.verify(treatmentRepository).findByPetId(1L);
    }

    @Test
    public void treatmentService_getTreatmentsByPetId_PetWithoutTreatments_ReturnsEmptyList() {
        // Arrange
        Mockito.when(treatmentRepository.findByPetId(3L)).thenReturn(List.of());

        // Act
        List<TreatmentResponse> result = treatmentService.getTreatmentsByPetId(3L);

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(treatmentRepository).findByPetId(3L);
    }

    @Test
    public void treatmentService_getTreatmentsByVeterinarianId_VeterinarianWithTreatments_ReturnsMappedResponses() {
        // Arrange: dos filas con mascotas distintas para comprobar que cada una se mapea con la suya.
        Pet otherPet = RepositoryTestData.pet(RepositoryTestData.owner());
        otherPet.setId(7L);
        LocalDateTime firstDate = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime secondDate = LocalDateTime.of(2026, 10, 3, 15, 30);
        Treatment first = treatment(1L, pet, vet, firstDate, 10.5, "Otitis externa", null, null);
        Treatment second = treatment(
                2L, otherPet, vet, secondDate, 4.2, "Dermatitis", Evolution.WORSENING, "Sin mejoría");
        Mockito.when(treatmentRepository.findByVeterinarianId(2L)).thenReturn(List.of(first, second));

        // Act
        List<TreatmentResponse> result = treatmentService.getTreatmentsByVeterinarianId(2L);

        // Assert
        Assertions.assertThat(result).containsExactly(
                new TreatmentResponse(1L, firstDate, 1L, pet.getName(), 2L, vet.getName(),
                        10.5, "Otitis externa", null, null, List.of()),
                new TreatmentResponse(2L, secondDate, 7L, otherPet.getName(), 2L, vet.getName(),
                        4.2, "Dermatitis", Evolution.WORSENING, "Sin mejoría", List.of()));
        Mockito.verify(treatmentRepository).findByVeterinarianId(2L);
    }

    @Test
    public void treatmentService_getTreatmentsByVeterinarianId_NoTreatments_ReturnsEmptyList() {
        // Arrange
        Mockito.when(treatmentRepository.findByVeterinarianId(9L)).thenReturn(List.of());

        // Act
        List<TreatmentResponse> result = treatmentService.getTreatmentsByVeterinarianId(9L);

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(treatmentRepository).findByVeterinarianId(9L);
    }
}
