package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Pruebas de integración de TreatmentDrugService: el servicio, los repositorios y la base H2 son reales
// (sin mocks). La clase se divide en dos grupos según el manejo transaccional:
//   (a) Escrituras exitosas y validaciones: se anotan @Transactional en el método, así que el servicio se
//       une a la transacción de la prueba y todo se revierte al terminar; la base compartida queda limpia.
//       Antes de volver a leer se hace flush() y clear() para leer estado persistido, no la caché de sesión.
//   (b) Rollback y commit reales: NO llevan @Transactional, así que el servicio abre y cierra su propia
//       transacción y el rollback de la aplicación sí se observa. Sus fixtures se confirman, se leen con
//       consultas nuevas al repositorio y se borran en @AfterEach en orden de claves foráneas.
@SpringBootTest
@ActiveProfiles("test")
public class TreatmentDrugServiceIntegrationTest {

    private final TreatmentDrugService treatmentDrugService;
    private final TreatmentService treatmentService;
    private final TreatmentDrugRepository treatmentDrugRepository;
    private final TreatmentRepository treatmentRepository;
    private final DrugRepository drugRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final EntityManager entityManager;

    // Fixtures del grupo (a): viven dentro de la transacción de la prueba que se revierte.
    private Pet pet;
    private Veterinarian vet;
    private Treatment treatment;

    // Ids de las fixtures confirmadas del grupo (b): se usan para el aseo final.
    private Long committedOwnerId;
    private Long committedPetId;
    private Long committedVeterinarianId;
    private Long committedDrugAId;
    private Long committedDrugBId;

    @Autowired
    public TreatmentDrugServiceIntegrationTest(
            TreatmentDrugService treatmentDrugService,
            TreatmentService treatmentService,
            TreatmentDrugRepository treatmentDrugRepository,
            TreatmentRepository treatmentRepository,
            DrugRepository drugRepository,
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VeterinarianRepository veterinarianRepository,
            EntityManager entityManager
    ) {
        this.treatmentDrugService = treatmentDrugService;
        this.treatmentService = treatmentService;
        this.treatmentDrugRepository = treatmentDrugRepository;
        this.treatmentRepository = treatmentRepository;
        this.drugRepository = drugRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.entityManager = entityManager;
    }

    // ---------- Utilidades de fixtures ----------

    // Construye un medicamento válido; los precios se expresan con escala 2.
    private Drug buildDrug(
            String name,
            String purchasePrice,
            String salePrice,
            int unitsAvailable,
            int unitsSold
    ) {
        return Drug.builder()
                .name(name)
                .purchasePrice(new BigDecimal(purchasePrice))
                .salePrice(new BigDecimal(salePrice))
                .unitsAvailable(unitsAvailable)
                .unitsSold(unitsSold)
                .build();
    }

    // Fixtures del grupo (a): mascota estable, veterinario, tratamiento y dueño guardados dentro de la
    // transacción de la prueba. No se usan en el grupo (b) porque allí se confirmarían de verdad.
    private void persistRolledBackFixtures() {
        Owner owner = ownerRepository.save(RepositoryTestData.owner());
        pet = petRepository.save(RepositoryTestData.pet(owner));
        vet = veterinarianRepository.save(RepositoryTestData.veterinarian());
        treatment = treatmentRepository.save(RepositoryTestData.treatment(
                pet, vet, LocalDateTime.of(2026, 11, 2, 10, 0), Evolution.IMPROVING));
        entityManager.flush();
    }

    // Crea y CONFIRMA (cada llamada al repositorio abre su propia transacción) las fixtures del grupo (b).
    // La mascota queda estable y con un peso conocido distinto al de la consulta, para que el rollback sea
    // observable tanto en el peso como en el diagnóstico.
    private void commitFixtures(String drugAName, int drugAAvailable, String drugBName, int drugBAvailable) {
        resetCommittedFixtureIds();

        Owner owner = ownerRepository.save(RepositoryTestData.owner());
        committedOwnerId = owner.getId();

        Pet newPet = RepositoryTestData.pet(owner);
        newPet.setWeight(9.8);
        committedPetId = petRepository.save(newPet).getId();

        Veterinarian newVet = RepositoryTestData.veterinarian();
        newVet.setAttentions(5);
        committedVeterinarianId = veterinarianRepository.save(newVet).getId();

        committedDrugAId = drugRepository
                .save(buildDrug(drugAName, "8000.00", "15000.00", drugAAvailable, 0))
                .getId();
        committedDrugBId = drugRepository
                .save(buildDrug(drugBName, "3000.00", "6000.00", drugBAvailable, 0))
                .getId();
    }

    private void resetCommittedFixtureIds() {
        committedOwnerId = null;
        committedPetId = null;
        committedVeterinarianId = null;
        committedDrugAId = null;
        committedDrugBId = null;
    }

    // Aseo del grupo (b): borra solo lo que creó el test, en orden de claves foráneas
    // (renglones, tratamientos, mascotas, dueños, veterinarios y medicamentos).
    @AfterEach
    public void cleanUpCommittedFixtures() {
        try {
            deleteCommittedFixtures();
        } finally {
            resetCommittedFixtureIds();
        }
    }

    private void deleteCommittedFixtures() {
        if (committedPetId != null) {
            List<Treatment> committedTreatments = treatmentRepository.findByPetId(committedPetId);
            committedTreatments.forEach(committedTreatment -> {
                treatmentDrugRepository.findByTreatmentId(committedTreatment.getId())
                        .forEach(treatmentDrugRepository::delete);
                treatmentRepository.delete(committedTreatment);
            });
            petRepository.findById(committedPetId).ifPresent(petRepository::delete);
        }
        if (committedOwnerId != null) {
            ownerRepository.findById(committedOwnerId).ifPresent(ownerRepository::delete);
        }
        if (committedVeterinarianId != null) {
            veterinarianRepository.findById(committedVeterinarianId).ifPresent(veterinarianRepository::delete);
        }
        if (committedDrugAId != null) {
            drugRepository.findById(committedDrugAId).ifPresent(drugRepository::delete);
        }
        if (committedDrugBId != null) {
            drugRepository.findById(committedDrugBId).ifPresent(drugRepository::delete);
        }
    }

    // ---------- Grupo (a): transacción de prueba con rollback automático ----------

    @Test
    @Transactional
    public void treatmentDrugService_createTreatmentDrug_ValidRequest_PersistsRowAndMovesStock() {
        // Arrange
        persistRolledBackFixtures();
        Drug drug = drugRepository.save(buildDrug("Amoxicilina 250mg", "8000.00", "15000.00", 10, 3));
        entityManager.flush();

        // Act
        TreatmentDrugResponse response = treatmentDrugService.createTreatmentDrug(
                treatment.getId(), new TreatmentDrugRequest(drug.getId(), 4));
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(response.id()).isNotNull();

        TreatmentDrug persisted = treatmentDrugRepository.findById(response.id()).orElseThrow();
        Assertions.assertThat(persisted.getId()).isEqualTo(response.id());
        Assertions.assertThat(persisted.getTreatment().getId()).isEqualTo(treatment.getId());
        Assertions.assertThat(persisted.getDrug().getId()).isEqualTo(drug.getId());
        Assertions.assertThat(persisted.getUnits()).isEqualTo(4);
        Assertions.assertThat(persisted.getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(persisted.getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));

        // El inventario se mueve en el medicamento real, no en una copia.
        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(reloadedDrug.getUnitsAvailable()).isEqualTo(6);
        Assertions.assertThat(reloadedDrug.getUnitsSold()).isEqualTo(7);

        // La respuesta se construye desde el renglón persistido y coincide con sus seis campos.
        Assertions.assertThat(response.drugId()).isEqualTo(drug.getId());
        Assertions.assertThat(response.drugName()).isEqualTo("Amoxicilina 250mg");
        Assertions.assertThat(response.units()).isEqualTo(4);
        Assertions.assertThat(response.unitPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(response.unitSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @Transactional
    public void treatmentDrugService_createTreatmentDrug_AfterCatalogPriceChange_KeepsStoredPriceSnapshot() {
        // Arrange
        // Se administra el medicamento con sus precios de catálogo actuales.
        persistRolledBackFixtures();
        Drug drug = drugRepository.save(buildDrug("Meloxicam 5mg", "8000.00", "15000.00", 10, 0));
        entityManager.flush();
        TreatmentDrugResponse response = treatmentDrugService.createTreatmentDrug(
                treatment.getId(), new TreatmentDrugRequest(drug.getId(), 2));
        entityManager.flush();
        entityManager.clear();

        // Act
        // El catálogo cambia después de administrar el medicamento.
        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        reloadedDrug.setPurchasePrice(new BigDecimal("9900.00"));
        reloadedDrug.setSalePrice(new BigDecimal("21000.00"));
        drugRepository.saveAndFlush(reloadedDrug);
        entityManager.clear();

        // Assert
        // El renglón del tratamiento conserva el precio histórico, no el precio nuevo.
        TreatmentDrug persisted = treatmentDrugRepository.findById(response.id()).orElseThrow();
        Assertions.assertThat(persisted.getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(persisted.getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));

        // El cambio de catálogo sí quedó guardado: los precios viejos son una copia, no una referencia.
        Drug drugAfterChange = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(drugAfterChange.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("9900.00"));
        Assertions.assertThat(drugAfterChange.getSalePrice()).isEqualByComparingTo(new BigDecimal("21000.00"));
    }

    @Test
    @Transactional
    public void treatmentDrugService_createTreatmentDrug_UnitsEqualToAvailableStock_PersistsRowAndLeavesZeroAvailable() {
        // Arrange
        persistRolledBackFixtures();
        Drug drug = drugRepository.save(buildDrug("Enrofloxacina boundary", "5000.00", "9000.00", 10, 1));
        entityManager.flush();

        // Act
        // Se piden exactamente las unidades disponibles.
        TreatmentDrugResponse response = treatmentDrugService.createTreatmentDrug(
                treatment.getId(), new TreatmentDrugRequest(drug.getId(), 10));
        entityManager.flush();
        entityManager.clear();

        // Assert
        // El límite es estricto (falla solo cuando disponible < solicitadas).
        TreatmentDrug persisted = treatmentDrugRepository.findById(response.id()).orElseThrow();
        Assertions.assertThat(persisted.getUnits()).isEqualTo(10);
        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(reloadedDrug.getUnitsAvailable()).isZero();
        Assertions.assertThat(reloadedDrug.getUnitsSold()).isEqualTo(11);
    }

    @Test
    @Transactional
    public void treatmentDrugService_createTreatmentDrug_UnitsGreaterThanAvailableStock_ThrowsIllegalOperationAndKeepsStockAndHistory() {
        // Arrange
        // Hay 10 unidades y se piden 11; el tratamiento ya tiene un renglón de otro medicamento.
        persistRolledBackFixtures();
        Drug drug = drugRepository.save(buildDrug("Dexametasona stock", "2000.00", "4500.00", 10, 4));
        Drug otherDrug = drugRepository.save(buildDrug("Ketamina preexistente", "6000.00", "11000.00", 20, 0));
        treatmentDrugRepository.save(RepositoryTestData.treatmentDrug(
                treatment, otherDrug, 2, new BigDecimal("6000.00"), new BigDecimal("11000.00")));
        entityManager.flush();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentDrugService.createTreatmentDrug(
                        treatment.getId(), new TreatmentDrugRequest(drug.getId(), 11)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No hay suficientes unidades disponibles de 'Dexametasona stock'. "
                        + "Disponibles: 10, solicitadas: 11.");

        // No hay cambios pendientes (la operación falló antes de escribir), solo se limpia la caché de sesión.
        entityManager.clear();

        // El stock del medicamento y el historial del tratamiento quedan intactos.
        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(reloadedDrug.getUnitsAvailable()).isEqualTo(10);
        Assertions.assertThat(reloadedDrug.getUnitsSold()).isEqualTo(4);

        List<TreatmentDrug> rows = treatmentDrugRepository.findByTreatmentId(treatment.getId());
        Assertions.assertThat(rows).hasSize(1);
        Assertions.assertThat(rows.get(0).getDrug().getId()).isEqualTo(otherDrug.getId());
        Assertions.assertThat(rows.get(0).getUnits()).isEqualTo(2);
    }

    @Test
    @Transactional
    public void treatmentDrugService_createTreatmentDrug_SameDrugTwiceInTreatment_ThrowsIllegalOperationAndKeepsSingleRow() {
        // Arrange
        // El medicamento ya se administró una vez en este tratamiento.
        persistRolledBackFixtures();
        Drug drug = drugRepository.save(buildDrug("Ceftriaxona 1g", "7000.00", "13000.00", 10, 0));
        entityManager.flush();
        treatmentDrugService.createTreatmentDrug(treatment.getId(), new TreatmentDrugRequest(drug.getId(), 3));
        entityManager.flush();

        // Act + Assert
        // El segundo intento choca con la regla única (treatment_id, drug_id).
        Assertions.assertThatThrownBy(() -> treatmentDrugService.createTreatmentDrug(
                        treatment.getId(), new TreatmentDrugRequest(drug.getId(), 1)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("La droga 'Ceftriaxona 1g' ya fue administrada en este tratamiento.");

        entityManager.clear();

        // Existe un solo renglón para ese par y el segundo intento no movió el inventario.
        List<TreatmentDrug> rows = treatmentDrugRepository.findByTreatmentId(treatment.getId());
        Assertions.assertThat(rows).hasSize(1);
        Assertions.assertThat(rows.get(0).getDrug().getId()).isEqualTo(drug.getId());
        Assertions.assertThat(rows.get(0).getUnits()).isEqualTo(3);

        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(reloadedDrug.getUnitsAvailable()).isEqualTo(7);
        Assertions.assertThat(reloadedDrug.getUnitsSold()).isEqualTo(3);
    }

    @Test
    @Transactional
    public void treatmentDrugService_getTreatmentDrugsByTreatmentId_TwoTreatments_ReturnsOnlyRequestedTreatmentRows() {
        // Arrange
        // Dos tratamientos de la misma mascota, cada uno con su propio medicamento.
        persistRolledBackFixtures();
        Treatment secondTreatment = treatmentRepository.save(RepositoryTestData.treatment(
                pet, vet, LocalDateTime.of(2026, 11, 3, 9, 0), Evolution.IMPROVING));
        Drug firstDrug = drugRepository.save(buildDrug("Ivermectina 1%", "4000.00", "8000.00", 10, 0));
        Drug secondDrug = drugRepository.save(buildDrug("Praziquantel 50mg", "3500.00", "7000.00", 10, 0));
        entityManager.flush();

        TreatmentDrugResponse firstRow = treatmentDrugService.createTreatmentDrug(
                treatment.getId(), new TreatmentDrugRequest(firstDrug.getId(), 2));
        treatmentDrugService.createTreatmentDrug(
                secondTreatment.getId(), new TreatmentDrugRequest(secondDrug.getId(), 5));
        entityManager.flush();
        entityManager.clear();

        // Act
        List<TreatmentDrugResponse> result =
                treatmentDrugService.getTreatmentDrugsByTreatmentId(treatment.getId());

        // Assert
        // Solo los renglones del tratamiento consultado, con sus datos históricos.
        Assertions.assertThat(result).hasSize(1);
        TreatmentDrugResponse row = result.get(0);
        Assertions.assertThat(row.id()).isEqualTo(firstRow.id());
        Assertions.assertThat(row.drugId()).isEqualTo(firstDrug.getId());
        Assertions.assertThat(row.drugName()).isEqualTo("Ivermectina 1%");
        Assertions.assertThat(row.units()).isEqualTo(2);
        Assertions.assertThat(row.unitPurchasePrice()).isEqualByComparingTo(new BigDecimal("4000.00"));
        Assertions.assertThat(row.unitSalePrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        Assertions.assertThat(result)
                .extracting(TreatmentDrugResponse::drugId)
                .doesNotContain(secondDrug.getId());
    }

    // ---------- Grupo (b): rollback y commit reales, sin transacción de prueba ----------

    @Test
    public void treatmentService_createTreatment_SecondDrugWithoutStock_RollsBackWholeTreatment() {
        // Arrange
        // Fixtures confirmadas. El medicamento A sí alcanza y se administra primero; el B no alcanza.
        commitFixtures("Amoxicilina rollback", 10, "Ibuprofeno rollback", 3);
        long treatmentDrugsBefore = treatmentDrugRepository.count();
        TreatmentRequest request = new TreatmentRequest(
                committedPetId, committedVeterinarianId, null, 12.4, "Otitis externa", null, "Consulta de prueba",
                List.of(
                        new TreatmentDrugRequest(committedDrugAId, 4),
                        new TreatmentDrugRequest(committedDrugBId, 5)));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("No hay suficientes unidades disponibles de 'Ibuprofeno rollback'. "
                        + "Disponibles: 3, solicitadas: 5.");

        // Assert
        // Lecturas nuevas: cada llamada al repositorio abre su propia transacción.
        Assertions.assertThat(treatmentRepository.findByPetId(committedPetId)).isEmpty();
        Assertions.assertThat(treatmentDrugRepository.count()).isEqualTo(treatmentDrugsBefore);
        Assertions.assertThat(treatmentDrugRepository.existsByDrugId(committedDrugAId)).isFalse();
        Assertions.assertThat(treatmentDrugRepository.existsByDrugId(committedDrugBId)).isFalse();

        // El stock del primer medicamento también se revierte: el tratamiento es atómico.
        Drug drugAAfterRollback = drugRepository.findById(committedDrugAId).orElseThrow();
        Assertions.assertThat(drugAAfterRollback.getUnitsAvailable()).isEqualTo(10);
        Assertions.assertThat(drugAAfterRollback.getUnitsSold()).isZero();

        Drug drugBAfterRollback = drugRepository.findById(committedDrugBId).orElseThrow();
        Assertions.assertThat(drugBAfterRollback.getUnitsAvailable()).isEqualTo(3);
        Assertions.assertThat(drugBAfterRollback.getUnitsSold()).isZero();

        // Las atenciones del veterinario y los datos clínicos de la mascota tampoco cambian.
        Assertions.assertThat(veterinarianRepository.findById(committedVeterinarianId).orElseThrow().getAttentions())
                .isEqualTo(5);

        Pet petAfterRollback = petRepository.findById(committedPetId).orElseThrow();
        Assertions.assertThat(petAfterRollback.getWeight()).isEqualTo(9.8);
        Assertions.assertThat(petAfterRollback.getDisease()).isNull();
    }

    @Test
    public void treatmentService_createTreatment_EnoughStockForEveryDrug_CommitsTreatmentAndStockMovements() {
        // Arrange
        // El mismo escenario, pero con stock suficiente para los dos medicamentos.
        commitFixtures("Amoxicilina control", 10, "Ibuprofeno control", 6);
        TreatmentRequest request = new TreatmentRequest(
                committedPetId, committedVeterinarianId, null, 12.4, "Otitis externa", null, "Consulta de prueba",
                List.of(
                        new TreatmentDrugRequest(committedDrugAId, 4),
                        new TreatmentDrugRequest(committedDrugBId, 5)));

        // Act
        TreatmentResponse response = treatmentService.createTreatment(request);

        // Assert
        // El tratamiento y sus dos renglones quedaron confirmados.
        Assertions.assertThat(response.id()).isNotNull();
        List<Treatment> treatments = treatmentRepository.findByPetId(committedPetId);
        Assertions.assertThat(treatments).hasSize(1);
        Assertions.assertThat(treatments.get(0).getId()).isEqualTo(response.id());

        List<TreatmentDrug> rows = treatmentDrugRepository.findByTreatmentId(response.id());
        Assertions.assertThat(rows).hasSize(2);
        Assertions.assertThat(rows).extracting(TreatmentDrug::getUnits).containsExactlyInAnyOrder(4, 5);
        Assertions.assertThat(treatmentDrugRepository.existsByTreatmentIdAndDrugId(response.id(), committedDrugAId))
                .isTrue();
        Assertions.assertThat(treatmentDrugRepository.existsByTreatmentIdAndDrugId(response.id(), committedDrugBId))
                .isTrue();

        // Ambos inventarios se descontaron y la venta quedó registrada.
        Drug drugAAfterCommit = drugRepository.findById(committedDrugAId).orElseThrow();
        Assertions.assertThat(drugAAfterCommit.getUnitsAvailable()).isEqualTo(6);
        Assertions.assertThat(drugAAfterCommit.getUnitsSold()).isEqualTo(4);

        Drug drugBAfterCommit = drugRepository.findById(committedDrugBId).orElseThrow();
        Assertions.assertThat(drugBAfterCommit.getUnitsAvailable()).isEqualTo(1);
        Assertions.assertThat(drugBAfterCommit.getUnitsSold()).isEqualTo(5);

        Assertions.assertThat(veterinarianRepository.findById(committedVeterinarianId).orElseThrow().getAttentions())
                .isEqualTo(6);
    }
}
