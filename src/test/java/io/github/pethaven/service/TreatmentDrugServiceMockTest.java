package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.TreatmentDrugMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el TreatmentDrugMapper es real y los repositorios y el DrugService son simulados.
@ExtendWith(MockitoExtension.class)
public class TreatmentDrugServiceMockTest {

    @Mock
    private TreatmentDrugRepository treatmentDrugRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private DrugService drugService;
    @Mock
    private DrugRepository drugRepository;

    private TreatmentDrugServiceImpl treatmentDrugService;

    // Filas que devuelve el repositorio simulado al guardar, en el mismo orden en que se guardan.
    private final List<TreatmentDrug> persistedTreatmentDrugs = new ArrayList<>();
    private long nextSavedId = 50L;

    @BeforeEach
    public void setUp() {
        treatmentDrugService = new TreatmentDrugServiceImpl(
                treatmentDrugRepository, new TreatmentDrugMapper(), treatmentRepository, drugService, drugRepository);
    }

    // ---------- Helpers ----------

    private Pet activePet() {
        Pet pet = RepositoryTestData.pet(RepositoryTestData.owner());
        pet.setId(3L);
        pet.setName("Luna");
        return pet;
    }

    private Treatment treatment(Pet pet) {
        Treatment treatment = RepositoryTestData.treatment(
                pet, RepositoryTestData.veterinarian(), LocalDateTime.of(2026, 11, 2, 10, 0), Evolution.IMPROVING);
        treatment.setId(1L);
        return treatment;
    }

    private Drug drug(
            Long id,
            String name,
            String purchasePrice,
            String salePrice,
            Integer unitsAvailable,
            Integer unitsSold) {
        Drug drug = RepositoryTestData.drug(name);
        drug.setId(id);
        drug.setPurchasePrice(new BigDecimal(purchasePrice));
        drug.setSalePrice(new BigDecimal(salePrice));
        drug.setUnitsAvailable(unitsAvailable);
        drug.setUnitsSold(unitsSold);
        return drug;
    }

    private TreatmentDrug treatmentDrug(
            Long id,
            Treatment treatment,
            Drug drug,
            int units,
            String unitPurchasePrice,
            String unitSalePrice) {
        TreatmentDrug treatmentDrug = RepositoryTestData.treatmentDrug(
                treatment, drug, units, new BigDecimal(unitPurchasePrice), new BigDecimal(unitSalePrice));
        treatmentDrug.setId(id);
        return treatmentDrug;
    }

    // Simula la persistencia: la base de datos asigna un id y devuelve una instancia distinta de la fila guardada.
    private void stubTreatmentDrugSaveWithGeneratedIds() {
        Mockito.when(treatmentDrugRepository.save(ArgumentMatchers.any(TreatmentDrug.class)))
                .thenAnswer(invocation -> {
                    TreatmentDrug toSave = invocation.getArgument(0);
                    TreatmentDrug saved = TreatmentDrug.builder()
                            .id(nextSavedId++)
                            .treatment(toSave.getTreatment())
                            .drug(toSave.getDrug())
                            .units(toSave.getUnits())
                            .unitPurchasePrice(toSave.getUnitPurchasePrice())
                            .unitSalePrice(toSave.getUnitSalePrice())
                            .build();
                    persistedTreatmentDrugs.add(saved);
                    return saved;
                });
    }

    // ---------- getTreatmentDrugsByTreatmentId ----------

    @Test
    public void treatmentDrugService_getTreatmentDrugsByTreatmentId_TwoRows_ReturnsMappedResponsesInOrder() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug amoxicillin = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Drug ibuprofen = drug(6L, "Ibuprofeno 400mg", "3000.00", "6000.00", 40, 12);
        // Los precios de cada fila son el histórico del momento de administrar, no el catálogo actual del medicamento.
        TreatmentDrug first = treatmentDrug(50L, treatment, amoxicillin, 4, "7000.00", "14000.00");
        TreatmentDrug second = treatmentDrug(51L, treatment, ibuprofen, 2, "2500.00", "5500.00");
        Mockito.when(treatmentDrugRepository.findByTreatmentId(1L)).thenReturn(List.of(first, second));

        // Act
        List<TreatmentDrugResponse> result = treatmentDrugService.getTreatmentDrugsByTreatmentId(1L);

        // Assert
        Assertions.assertThat(result).containsExactly(
                new TreatmentDrugResponse(
                        50L, 5L, "Amoxicilina 250mg", 4, new BigDecimal("7000.00"), new BigDecimal("14000.00")),
                new TreatmentDrugResponse(
                        51L, 6L, "Ibuprofeno 400mg", 2, new BigDecimal("2500.00"), new BigDecimal("5500.00")));
        Mockito.verify(treatmentDrugRepository).findByTreatmentId(1L);
    }

    @Test
    public void treatmentDrugService_getTreatmentDrugsByTreatmentId_NoRows_ReturnsEmptyList() {
        // Arrange
        Mockito.when(treatmentDrugRepository.findByTreatmentId(99L)).thenReturn(List.of());

        // Act
        List<TreatmentDrugResponse> result = treatmentDrugService.getTreatmentDrugsByTreatmentId(99L);

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(treatmentDrugRepository).findByTreatmentId(99L);
    }

    // ---------- createTreatmentDrug ----------

    @Test
    public void treatmentDrugService_createTreatmentDrug_ValidRequest_DecrementsStockAndReturnsPersistedResponse() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug drug = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(drug);
        Mockito.when(treatmentDrugRepository.existsByTreatmentIdAndDrugId(1L, 5L)).thenReturn(false);
        stubTreatmentDrugSaveWithGeneratedIds();

        // Act
        TreatmentDrugResponse result = treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, 4));

        // Assert
        // La respuesta se construye desde la fila persistida (id 50), con los seis campos del renglón.
        Assertions.assertThat(result).isEqualTo(new TreatmentDrugResponse(
                50L, 5L, "Amoxicilina 250mg", 4, new BigDecimal("8000.00"), new BigDecimal("15000.00")));

        // El stock se descuenta y la venta se registra sobre el medicamento cargado.
        Assertions.assertThat(drug.getUnitsAvailable()).isEqualTo(6);
        Assertions.assertThat(drug.getUnitsSold()).isEqualTo(7);
        Mockito.verify(drugRepository).save(drug);

        ArgumentCaptor<TreatmentDrug> captor = ArgumentCaptor.forClass(TreatmentDrug.class);
        Mockito.verify(treatmentDrugRepository).save(captor.capture());
        TreatmentDrug saved = captor.getValue();
        Assertions.assertThat(saved.getTreatment()).isSameAs(treatment);
        Assertions.assertThat(saved.getDrug()).isSameAs(drug);
        Assertions.assertThat(saved.getUnits()).isEqualTo(4);
        // Los precios se copian del medicamento en el momento de administrarlo.
        Assertions.assertThat(saved.getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(saved.getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));

        // La lista del tratamiento guarda la fila ya persistida, no la instancia previa al guardado.
        Assertions.assertThat(treatment.getTreatmentDrugs())
                .singleElement()
                .isSameAs(persistedTreatmentDrugs.get(0));

        Mockito.verify(treatmentRepository).findById(1L);
        Mockito.verify(drugService).getDrugEntity(5L);
        Mockito.verify(treatmentDrugRepository).existsByTreatmentIdAndDrugId(1L, 5L);
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_UnitsEqualToAvailableStock_SucceedsAndLeavesZeroAvailable() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug drug = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(drug);
        Mockito.when(treatmentDrugRepository.existsByTreatmentIdAndDrugId(1L, 5L)).thenReturn(false);
        stubTreatmentDrugSaveWithGeneratedIds();

        // Act
        TreatmentDrugResponse result = treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, 10));

        // Assert
        // Administrar exactamente el stock disponible es válido: el límite es estricto (disponible < solicitadas).
        Assertions.assertThat(result).isEqualTo(new TreatmentDrugResponse(
                50L, 5L, "Amoxicilina 250mg", 10, new BigDecimal("8000.00"), new BigDecimal("15000.00")));
        Assertions.assertThat(drug.getUnitsAvailable()).isZero();
        Assertions.assertThat(drug.getUnitsSold()).isEqualTo(13);
        Mockito.verify(drugRepository).save(drug);
        Mockito.verify(treatmentDrugRepository).save(ArgumentMatchers.any(TreatmentDrug.class));
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_UnitsGreaterThanAvailableStock_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug drug = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(drug);
        Mockito.when(treatmentDrugRepository.existsByTreatmentIdAndDrugId(1L, 5L)).thenReturn(false);

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, 11)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No hay suficientes unidades disponibles de 'Amoxicilina 250mg'. "
                        + "Disponibles: 10, solicitadas: 11.");
        Mockito.verify(drugRepository, Mockito.never()).save(ArgumentMatchers.any(Drug.class));
        Mockito.verify(treatmentDrugRepository, Mockito.never()).save(ArgumentMatchers.any(TreatmentDrug.class));
        // El stock y el historial del tratamiento quedan intactos.
        Assertions.assertThat(drug.getUnitsAvailable()).isEqualTo(10);
        Assertions.assertThat(drug.getUnitsSold()).isEqualTo(3);
        Assertions.assertThat(treatment.getTreatmentDrugs()).isEmpty();
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_NonExistingTreatment_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(treatmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(99L, new TreatmentDrugRequest(5L, 4)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Treatment' con id: '99' no fue encontrado");
        Mockito.verifyNoInteractions(drugService, drugRepository);
        Mockito.verify(treatmentDrugRepository, Mockito.never()).save(ArgumentMatchers.any(TreatmentDrug.class));
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_NullUnits_ThrowsIllegalOperationAndDoesNotLoadDrug() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, null)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Las unidades administradas deben ser mayores a cero.");
        // La validación de unidades corre antes de cargar el medicamento y de tocar el historial.
        Mockito.verifyNoInteractions(drugService, drugRepository, treatmentDrugRepository);
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_ZeroUnits_ThrowsIllegalOperationAndDoesNotLoadDrug() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, 0)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Las unidades administradas deben ser mayores a cero.");
        Mockito.verifyNoInteractions(drugService, drugRepository, treatmentDrugRepository);
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_NegativeUnits_ThrowsIllegalOperationAndDoesNotLoadDrug() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, -1)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Las unidades administradas deben ser mayores a cero.");
        Mockito.verifyNoInteractions(drugService, drugRepository, treatmentDrugRepository);
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_InactivePet_ThrowsIllegalOperationAndDoesNotLoadDrug() {
        // Arrange
        Pet pet = activePet();
        pet.setActive(false);
        Treatment treatment = treatment(pet);
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, 4)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("La mascota 'Luna' no está activa: no se le pueden administrar medicamentos.");
        Mockito.verifyNoInteractions(drugService, drugRepository, treatmentDrugRepository);
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_DrugAlreadyAdministered_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug drug = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(drug);
        Mockito.when(treatmentDrugRepository.existsByTreatmentIdAndDrugId(1L, 5L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(5L, 4)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("La droga 'Amoxicilina 250mg' ya fue administrada en este tratamiento.");
        // La comprobación de duplicados usa el id del tratamiento y el drugId de la petición.
        Mockito.verify(treatmentDrugRepository).existsByTreatmentIdAndDrugId(1L, 5L);
        Mockito.verify(drugRepository, Mockito.never()).save(ArgumentMatchers.any(Drug.class));
        Mockito.verify(treatmentDrugRepository, Mockito.never()).save(ArgumentMatchers.any(TreatmentDrug.class));
        Assertions.assertThat(drug.getUnitsAvailable()).isEqualTo(10);
        Assertions.assertThat(drug.getUnitsSold()).isEqualTo(3);
        Assertions.assertThat(treatment.getTreatmentDrugs()).isEmpty();
    }

    @Test
    public void treatmentDrugService_createTreatmentDrug_NonExistingDrug_PropagatesResourceNotFound() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Mockito.when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        Mockito.when(drugService.getDrugEntity(99L)).thenThrow(new ResourceNotFoundException("Drug", "id", 99L));

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> treatmentDrugService.createTreatmentDrug(1L, new TreatmentDrugRequest(99L, 4)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Drug' con id: '99' no fue encontrado");
        // Sin medicamento no se comprueba duplicado, no se mueve stock y no se guarda nada.
        Mockito.verify(treatmentDrugRepository, Mockito.never())
                .existsByTreatmentIdAndDrugId(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());
        Mockito.verifyNoInteractions(drugRepository);
        Mockito.verify(treatmentDrugRepository, Mockito.never()).save(ArgumentMatchers.any(TreatmentDrug.class));
    }

    // ---------- administerDrugs ----------

    @Test
    public void treatmentDrugService_administerDrugs_NullRequests_DoesNothing() {
        // Arrange
        Treatment treatment = treatment(activePet());

        // Act
        treatmentDrugService.administerDrugs(treatment, null);

        // Assert
        Mockito.verifyNoInteractions(treatmentDrugRepository, treatmentRepository, drugService, drugRepository);
        Assertions.assertThat(treatment.getTreatmentDrugs()).isEmpty();
    }

    @Test
    public void treatmentDrugService_administerDrugs_EmptyRequests_DoesNothing() {
        // Arrange
        Treatment treatment = treatment(activePet());

        // Act
        treatmentDrugService.administerDrugs(treatment, List.of());

        // Assert
        Mockito.verifyNoInteractions(treatmentDrugRepository, treatmentRepository, drugService, drugRepository);
        Assertions.assertThat(treatment.getTreatmentDrugs()).isEmpty();
    }

    @Test
    public void treatmentDrugService_administerDrugs_TwoValidRequests_SavesBothRowsInRequestOrder() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug amoxicillin = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Drug ibuprofen = drug(6L, "Ibuprofeno 400mg", "3000.00", "6000.00", 40, 12);
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(amoxicillin);
        Mockito.when(drugService.getDrugEntity(6L)).thenReturn(ibuprofen);
        stubTreatmentDrugSaveWithGeneratedIds();

        // Act
        treatmentDrugService.administerDrugs(
                treatment, List.of(new TreatmentDrugRequest(5L, 4), new TreatmentDrugRequest(6L, 2)));

        // Assert
        Assertions.assertThat(amoxicillin.getUnitsAvailable()).isEqualTo(6);
        Assertions.assertThat(amoxicillin.getUnitsSold()).isEqualTo(7);
        Assertions.assertThat(ibuprofen.getUnitsAvailable()).isEqualTo(38);
        Assertions.assertThat(ibuprofen.getUnitsSold()).isEqualTo(14);
        Mockito.verify(drugRepository).save(amoxicillin);
        Mockito.verify(drugRepository).save(ibuprofen);

        ArgumentCaptor<TreatmentDrug> captor = ArgumentCaptor.forClass(TreatmentDrug.class);
        Mockito.verify(treatmentDrugRepository, Mockito.times(2)).save(captor.capture());
        List<TreatmentDrug> saved = captor.getAllValues();
        Assertions.assertThat(saved.get(0).getDrug()).isSameAs(amoxicillin);
        Assertions.assertThat(saved.get(0).getUnits()).isEqualTo(4);
        Assertions.assertThat(saved.get(0).getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(saved.get(0).getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));
        Assertions.assertThat(saved.get(1).getDrug()).isSameAs(ibuprofen);
        Assertions.assertThat(saved.get(1).getUnits()).isEqualTo(2);
        Assertions.assertThat(saved.get(1).getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("3000.00"));
        Assertions.assertThat(saved.get(1).getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("6000.00"));

        // Ambas filas persistidas se agregan a la lista del tratamiento en el orden de la petición.
        Assertions.assertThat(treatment.getTreatmentDrugs()).hasSize(2);
        Assertions.assertThat(treatment.getTreatmentDrugs().get(0)).isSameAs(persistedTreatmentDrugs.get(0));
        Assertions.assertThat(treatment.getTreatmentDrugs().get(1)).isSameAs(persistedTreatmentDrugs.get(1));
        // El tratamiento recibido ya existe: no se vuelve a consultar.
        Mockito.verifyNoInteractions(treatmentRepository);
    }

    @Test
    public void treatmentDrugService_administerDrugs_SecondRequestWithoutStock_KeepsFirstRequestApplied() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug amoxicillin = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Drug ibuprofen = drug(6L, "Ibuprofeno 400mg", "3000.00", "6000.00", 10, 0);
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(amoxicillin);
        Mockito.when(drugService.getDrugEntity(6L)).thenReturn(ibuprofen);
        stubTreatmentDrugSaveWithGeneratedIds();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentDrugService.administerDrugs(
                        treatment, List.of(new TreatmentDrugRequest(5L, 4), new TreatmentDrugRequest(6L, 11))))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No hay suficientes unidades disponibles de 'Ibuprofeno 400mg'. "
                        + "Disponibles: 10, solicitadas: 11.");

        // El primer medicamento ya se descontó y se guardó antes de fallar el segundo: el servicio no lo revierte,
        // porque el rollback es responsabilidad de la transacción del llamador.
        Assertions.assertThat(amoxicillin.getUnitsAvailable()).isEqualTo(6);
        Assertions.assertThat(amoxicillin.getUnitsSold()).isEqualTo(7);
        Mockito.verify(drugRepository).save(amoxicillin);
        Assertions.assertThat(treatment.getTreatmentDrugs())
                .singleElement()
                .isSameAs(persistedTreatmentDrugs.get(0));

        // El segundo medicamento no se toca ni se guarda.
        Assertions.assertThat(ibuprofen.getUnitsAvailable()).isEqualTo(10);
        Assertions.assertThat(ibuprofen.getUnitsSold()).isZero();
        Mockito.verify(drugRepository, Mockito.never()).save(ibuprofen);
        Mockito.verify(treatmentDrugRepository, Mockito.times(1)).save(ArgumentMatchers.any(TreatmentDrug.class));
    }

    @Test
    public void treatmentDrugService_administerDrugs_FirstRequestWithoutStock_DoesNotProcessSecondRequest() {
        // Arrange
        Treatment treatment = treatment(activePet());
        Drug amoxicillin = drug(5L, "Amoxicilina 250mg", "8000.00", "15000.00", 10, 3);
        Mockito.when(drugService.getDrugEntity(5L)).thenReturn(amoxicillin);
        // No se simula el segundo medicamento: el servicio no debe llegar a cargarlo.

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentDrugService.administerDrugs(
                        treatment, List.of(new TreatmentDrugRequest(5L, 11), new TreatmentDrugRequest(6L, 2))))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No hay suficientes unidades disponibles de 'Amoxicilina 250mg'. "
                        + "Disponibles: 10, solicitadas: 11.");
        Mockito.verify(drugService, Mockito.never()).getDrugEntity(6L);
        Mockito.verify(drugRepository, Mockito.never()).save(ArgumentMatchers.any(Drug.class));
        Mockito.verify(treatmentDrugRepository, Mockito.never()).save(ArgumentMatchers.any(TreatmentDrug.class));
        Assertions.assertThat(amoxicillin.getUnitsAvailable()).isEqualTo(10);
        Assertions.assertThat(amoxicillin.getUnitsSold()).isEqualTo(3);
        Assertions.assertThat(treatment.getTreatmentDrugs()).isEmpty();
    }
}
