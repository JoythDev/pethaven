package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.DrugMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
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
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el DrugMapper es real y los repositorios son simulados, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class DrugServiceMockTest {

    @Mock
    private DrugRepository drugRepository;
    @Mock
    private TreatmentDrugRepository treatmentDrugRepository;

    private DrugServiceImpl drugService;

    @BeforeEach
    public void setUp() {
        drugService = new DrugServiceImpl(drugRepository, new DrugMapper(), treatmentDrugRepository);
    }

    // ---------- Helpers ----------

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

    private DrugRequest createRequest(String name) {
        return createRequest(name, "8000.00", "15000.00", 200);
    }

    private DrugRequest createRequest(String name, String purchasePrice, String salePrice, Integer unitsAvailable) {
        return new DrugRequest(name, new BigDecimal(purchasePrice), new BigDecimal(salePrice), unitsAvailable);
    }

    // ---------- getDrugEntity ----------

    @Test
    public void drugService_getDrugEntity_ExistingId_ReturnsEntity() {
        // Arrange
        Drug drug = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 5);
        Mockito.when(drugRepository.findById(1L)).thenReturn(Optional.of(drug));

        // Act
        Drug result = drugService.getDrugEntity(1L);

        // Assert
        Assertions.assertThat(result).isSameAs(drug);
        Mockito.verify(drugRepository).findById(1L);
    }

    @Test
    public void drugService_getDrugEntity_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.getDrugEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Drug' con id: '99' no fue encontrado");
    }

    // ---------- getAllDrugs ----------

    @Test
    public void drugService_getAllDrugs_TwoDrugs_ReturnsMappedResponsesInOrder() {
        // Arrange
        Drug first = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 5);
        Drug second = drug(2L, "Ibuprofeno 400mg", "3000.00", "6000.00", 40, 12);
        Mockito.when(drugRepository.findAll()).thenReturn(List.of(first, second));

        // Act
        List<DrugResponse> result = drugService.getAllDrugs();

        // Assert
        Assertions.assertThat(result).containsExactly(
                new DrugResponse(
                        1L, "Amoxicilina 250mg", new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200, 5),
                new DrugResponse(
                        2L, "Ibuprofeno 400mg", new BigDecimal("3000.00"), new BigDecimal("6000.00"), 40, 12));
        Mockito.verify(drugRepository).findAll();
    }

    @Test
    public void drugService_getAllDrugs_NoDrugs_ReturnsEmptyList() {
        // Arrange
        Mockito.when(drugRepository.findAll()).thenReturn(List.of());

        // Act
        List<DrugResponse> result = drugService.getAllDrugs();

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    // ---------- getAvailableDrugs ----------

    @Test
    public void drugService_getAvailableDrugs_DrugsWithStock_ReturnsMappedResponses() {
        // Arrange
        Drug first = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 5);
        Drug second = drug(2L, "Ibuprofeno 400mg", "3000.00", "6000.00", 40, 12);
        Mockito.when(drugRepository.findByUnitsAvailableGreaterThan(0)).thenReturn(List.of(first, second));

        // Act
        List<DrugResponse> result = drugService.getAvailableDrugs();

        // Assert
        Assertions.assertThat(result).containsExactly(
                new DrugResponse(
                        1L, "Amoxicilina 250mg", new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200, 5),
                new DrugResponse(
                        2L, "Ibuprofeno 400mg", new BigDecimal("3000.00"), new BigDecimal("6000.00"), 40, 12));
        // Se consulta el stock disponible con el umbral exacto 0, no el catálogo completo.
        Mockito.verify(drugRepository).findByUnitsAvailableGreaterThan(0);
        Mockito.verify(drugRepository, Mockito.never()).findAll();
    }

    @Test
    public void drugService_getAvailableDrugs_NoDrugsWithStock_ReturnsEmptyList() {
        // Arrange
        Mockito.when(drugRepository.findByUnitsAvailableGreaterThan(0)).thenReturn(List.of());

        // Act
        List<DrugResponse> result = drugService.getAvailableDrugs();

        // Assert
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(drugRepository).findByUnitsAvailableGreaterThan(0);
        Mockito.verify(drugRepository, Mockito.never()).findAll();
    }

    // ---------- getDrugById ----------

    @Test
    public void drugService_getDrugById_ExistingId_ReturnsMappedResponse() {
        // Arrange
        Drug drug = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 5);
        Mockito.when(drugRepository.findById(1L)).thenReturn(Optional.of(drug));

        // Act
        DrugResponse result = drugService.getDrugById(1L);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new DrugResponse(
                        1L, "Amoxicilina 250mg", new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200, 5));
    }

    @Test
    public void drugService_getDrugById_NonExistingId_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.getDrugById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Drug' con id: '99' no fue encontrado");
    }

    // ---------- getDrugByName ----------

    @Test
    public void drugService_getDrugByName_ExistingName_ReturnsMappedResponse() {
        // Arrange
        Drug drug = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 5);
        Mockito.when(drugRepository.findByName("Amoxicilina 250mg")).thenReturn(Optional.of(drug));

        // Act
        DrugResponse result = drugService.getDrugByName("Amoxicilina 250mg");

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new DrugResponse(
                        1L, "Amoxicilina 250mg", new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200, 5));
        Mockito.verify(drugRepository).findByName("Amoxicilina 250mg");
    }

    @Test
    public void drugService_getDrugByName_NonExistingName_ThrowsResourceNotFound() {
        // Arrange
        Mockito.when(drugRepository.findByName("Ibuprofeno 400mg")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.getDrugByName("Ibuprofeno 400mg"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Drug' con name: 'Ibuprofeno 400mg' no fue encontrado");
    }

    // ---------- createDrug ----------

    @Test
    public void drugService_createDrug_ValidRequest_SavesEntityWithZeroUnitsSoldAndReturnsPersistedResponse() {
        // Arrange
        DrugRequest request = createRequest("Amoxicilina 250mg");
        Mockito.when(drugRepository.existsByName("Amoxicilina 250mg")).thenReturn(false);
        // Simula la persistencia: la base de datos asigna el id y devuelve una instancia distinta de la entidad.
        Mockito.when(drugRepository.save(ArgumentMatchers.any(Drug.class)))
                .thenAnswer(invocation -> {
                    Drug toSave = invocation.getArgument(0);
                    return Drug.builder()
                            .id(10L)
                            .name(toSave.getName())
                            .purchasePrice(toSave.getPurchasePrice())
                            .salePrice(toSave.getSalePrice())
                            .unitsAvailable(toSave.getUnitsAvailable())
                            .unitsSold(toSave.getUnitsSold())
                            .build();
                });

        // Act
        DrugResponse result = drugService.createDrug(request);

        // Assert
        // La respuesta se construye desde la entidad guardada (id 10), no desde la entidad previa al guardado.
        Assertions.assertThat(result)
                .isEqualTo(new DrugResponse(
                        10L, "Amoxicilina 250mg", new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200, 0));

        ArgumentCaptor<Drug> captor = ArgumentCaptor.forClass(Drug.class);
        Mockito.verify(drugRepository).save(captor.capture());
        Drug saved = captor.getValue();
        Assertions.assertThat(saved.getName()).isEqualTo("Amoxicilina 250mg");
        Assertions.assertThat(saved.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(saved.getSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));
        Assertions.assertThat(saved.getUnitsAvailable()).isEqualTo(200);
        // El mapper fija las unidades vendidas en cero: las calcula el sistema.
        Assertions.assertThat(saved.getUnitsSold()).isZero();

        // La creación comprueba unicidad por nombre sin excluir ningún id.
        Mockito.verify(drugRepository).existsByName("Amoxicilina 250mg");
        Mockito.verify(drugRepository, Mockito.never())
                .existsByNameAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    @Test
    public void drugService_createDrug_DuplicateName_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        DrugRequest request = createRequest("Amoxicilina 250mg");
        Mockito.when(drugRepository.existsByName("Amoxicilina 250mg")).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.createDrug(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un medicamento con el nombre 'Amoxicilina 250mg'.");
        Mockito.verify(drugRepository, Mockito.never()).save(ArgumentMatchers.any(Drug.class));
        Mockito.verify(drugRepository, Mockito.never())
                .existsByNameAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
    }

    // ---------- updateDrug ----------

    @Test
    public void drugService_updateDrug_ValidRequest_AppliesFieldsOnSameEntityAndPreservesUnitsSold() {
        // Arrange
        Drug existing = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 12);
        DrugRequest request = createRequest("Amoxicilina 500mg", "9000.00", "17000.00", 150);
        Mockito.when(drugRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(drugRepository.existsByNameAndIdNot("Amoxicilina 500mg", 1L)).thenReturn(false);
        Mockito.when(drugRepository.save(ArgumentMatchers.any(Drug.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DrugResponse result = drugService.updateDrug(1L, request);

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new DrugResponse(
                        1L, "Amoxicilina 500mg", new BigDecimal("9000.00"), new BigDecimal("17000.00"), 150, 12));

        ArgumentCaptor<Drug> captor = ArgumentCaptor.forClass(Drug.class);
        Mockito.verify(drugRepository).save(captor.capture());
        Drug saved = captor.getValue();
        // Se actualiza la misma instancia persistida, sin crear una entidad nueva.
        Assertions.assertThat(saved).isSameAs(existing);
        Assertions.assertThat(saved.getName()).isEqualTo("Amoxicilina 500mg");
        Assertions.assertThat(saved.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("9000.00"));
        Assertions.assertThat(saved.getSalePrice()).isEqualByComparingTo(new BigDecimal("17000.00"));
        Assertions.assertThat(saved.getUnitsAvailable()).isEqualTo(150);
        // El request no incluye unitsSold: se conservan las unidades ya vendidas.
        Assertions.assertThat(saved.getUnitsSold()).isEqualTo(12);

        // La actualización excluye al propio medicamento de la comprobación de unicidad.
        Mockito.verify(drugRepository).existsByNameAndIdNot("Amoxicilina 500mg", 1L);
        Mockito.verify(drugRepository, Mockito.never()).existsByName(ArgumentMatchers.anyString());
    }

    @Test
    public void drugService_updateDrug_NonExistingId_ThrowsResourceNotFoundAndDoesNotSave() {
        // Arrange
        DrugRequest request = createRequest("Amoxicilina 500mg");
        Mockito.when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.updateDrug(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Drug' con id: '99' no fue encontrado");
        Mockito.verify(drugRepository, Mockito.never())
                .existsByNameAndIdNot(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
        Mockito.verify(drugRepository, Mockito.never()).existsByName(ArgumentMatchers.anyString());
        Mockito.verify(drugRepository, Mockito.never()).save(ArgumentMatchers.any(Drug.class));
    }

    @Test
    public void drugService_updateDrug_DuplicateName_ThrowsIllegalOperationAndDoesNotSave() {
        // Arrange
        Drug existing = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 12);
        DrugRequest request = createRequest("Ibuprofeno 400mg");
        Mockito.when(drugRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(drugRepository.existsByNameAndIdNot("Ibuprofeno 400mg", 1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.updateDrug(1L, request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe un medicamento con el nombre 'Ibuprofeno 400mg'.");
        Mockito.verify(drugRepository, Mockito.never()).save(ArgumentMatchers.any(Drug.class));
        // La validación ocurre antes de aplicar los cambios: la entidad conserva sus valores.
        Assertions.assertThat(existing.getName()).isEqualTo("Amoxicilina 250mg");
        Assertions.assertThat(existing.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(existing.getSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));
        Assertions.assertThat(existing.getUnitsAvailable()).isEqualTo(200);
    }

    // ---------- deleteDrugById ----------

    @Test
    public void drugService_deleteDrugById_NonExistingId_ThrowsResourceNotFoundAndDoesNotDelete() {
        // Arrange
        Mockito.when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.deleteDrugById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Drug' con id: '99' no fue encontrado");
        Mockito.verify(drugRepository, Mockito.never()).deleteById(ArgumentMatchers.anyLong());
        Mockito.verifyNoInteractions(treatmentDrugRepository);
    }

    @Test
    public void drugService_deleteDrugById_DrugUsedInTreatments_ThrowsIllegalOperationAndDoesNotDelete() {
        // Arrange
        Drug drug = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 12);
        Mockito.when(drugRepository.findById(1L)).thenReturn(Optional.of(drug));
        Mockito.when(treatmentDrugRepository.existsByDrugId(1L)).thenReturn(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugService.deleteDrugById(1L))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No se puede eliminar el medicamento 'Amoxicilina 250mg' porque está asociado a "
                        + "uno o más tratamientos existentes.");
        Mockito.verify(drugRepository, Mockito.never()).deleteById(ArgumentMatchers.anyLong());
    }

    @Test
    public void drugService_deleteDrugById_DrugWithoutTreatments_DeletesDrug() {
        // Arrange
        Drug drug = drug(1L, "Amoxicilina 250mg", "8000.00", "15000.00", 200, 0);
        Mockito.when(drugRepository.findById(1L)).thenReturn(Optional.of(drug));
        Mockito.when(treatmentDrugRepository.existsByDrugId(1L)).thenReturn(false);

        // Act
        drugService.deleteDrugById(1L);

        // Assert
        Mockito.verify(treatmentDrugRepository).existsByDrugId(1L);
        Mockito.verify(drugRepository).deleteById(1L);
    }
}
