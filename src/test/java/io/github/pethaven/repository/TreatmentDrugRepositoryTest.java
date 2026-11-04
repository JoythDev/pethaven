package io.github.pethaven.repository;

import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.projection.MedicationTreatmentCountProjection;
import io.github.pethaven.repository.projection.TopMedicationUnitsProjection;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DataJpaTest
public class TreatmentDrugRepositoryTest {

    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 10, 1, 0, 0);

    private final TreatmentDrugRepository treatmentDrugRepository;
    private final TreatmentRepository treatmentRepository;
    private final DrugRepository drugRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final EntityManager entityManager;

    private Owner owner1;
    private Pet pet1;
    private Pet pet2;
    private Veterinarian vet1;
    private Veterinarian vet2;
    private Treatment t1;
    private Treatment t2;
    private Treatment t3;
    private Treatment t4;
    private Treatment t5;
    private Treatment t6;
    private Treatment t7;
    private Treatment t8;
    private Drug dA;
    private Drug dB;
    private Drug dC;
    private Drug dD;
    private Drug dE;
    private TreatmentDrug r1;
    private TreatmentDrug r2;
    private TreatmentDrug r3;
    private TreatmentDrug r4;
    private TreatmentDrug r5;
    private TreatmentDrug r6;
    private TreatmentDrug r7;
    private TreatmentDrug r8;
    private TreatmentDrug r9;

    @Autowired
    public TreatmentDrugRepositoryTest(
            TreatmentDrugRepository treatmentDrugRepository,
            TreatmentRepository treatmentRepository,
            DrugRepository drugRepository,
            VeterinarianRepository veterinarianRepository,
            PetRepository petRepository,
            OwnerRepository ownerRepository,
            EntityManager entityManager
    ) {
        this.treatmentDrugRepository = treatmentDrugRepository;
        this.treatmentRepository = treatmentRepository;
        this.drugRepository = drugRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        owner1 = saveOwner();
        pet1 = savePet(owner1);
        pet2 = savePet(owner1);

        vet1 = saveVeterinarian();
        vet2 = saveVeterinarian();

        t1 = saveTreatment(pet1, vet1, WINDOW_START, null);
        t2 = saveTreatment(pet1, vet1, WINDOW_START.plusDays(10), Evolution.IMPROVING);
        t3 = saveTreatment(pet2, vet2, WINDOW_END.minusSeconds(1), Evolution.WORSENING);
        t4 = saveTreatment(pet2, vet2, WINDOW_END, Evolution.STABLE);
        t5 = saveTreatment(pet1, vet1, WINDOW_START.minusSeconds(1), Evolution.IMPROVING);
        t6 = saveTreatment(pet1, vet2, WINDOW_START.plusDays(20), Evolution.RECOVERED);
        t7 = saveTreatment(pet2, vet1, WINDOW_START.minusDays(40), Evolution.IMPROVING);
        t8 = saveTreatment(pet1, vet1, WINDOW_START.plusDays(5), Evolution.IMPROVING);

        // dC se guarda primero a propósito: su id menor que el de dA evita que el desempate por id
        // coincida con el desempate por nombre en el ranking de unidades.
        dC = saveDrug("Zinc");
        dA = saveDrug("Amoxicilina 250mg");
        dB = saveDrug("Ibuprofeno 100mg");
        dD = saveDrug("Vitamina B", 999); // Sin filas de tratamiento y con unitsSold alto a propósito.
        dE = saveDrug("Amoxicilina 100mg");

        r1 = saveTreatmentDrug(t1, dA, 2, new BigDecimal("10.00"), new BigDecimal("15.00"));
        r2 = saveTreatmentDrug(t1, dB, 1, new BigDecimal("5.00"), new BigDecimal("8.00"));
        r3 = saveTreatmentDrug(t2, dA, 3, new BigDecimal("10.00"), new BigDecimal("15.00"));
        r4 = saveTreatmentDrug(t2, dE, 1, new BigDecimal("4.00"), new BigDecimal("6.00"));
        r5 = saveTreatmentDrug(t3, dB, 4, new BigDecimal("5.00"), new BigDecimal("7.50"));
        r6 = saveTreatmentDrug(t4, dA, 1, new BigDecimal("10.00"), new BigDecimal("15.00"));
        r7 = saveTreatmentDrug(t5, dC, 2, new BigDecimal("3.00"), new BigDecimal("5.00"));
        // t6 está RECOVERED y aun así tiene fila, para probar el filtro por evolución.
        r8 = saveTreatmentDrug(t6, dB, 2, new BigDecimal("5.00"), new BigDecimal("8.00"));
        r9 = saveTreatmentDrug(t7, dC, 4, new BigDecimal("9.00"), new BigDecimal("6.00")); // Margen negativo a propósito.

        entityManager.flush();
    }

    @Test
    public void treatmentDrugRepository_save_ValidTreatmentDrug_ReturnsPersistedWithGeneratedId() {
        // Arrange
        // t8 no tiene filas previas y dD no se usa en ninguna otra fixture.
        TreatmentDrug treatmentDrug = unsavedTreatmentDrug();

        // Act
        TreatmentDrug savedTreatmentDrug = treatmentDrugRepository.save(treatmentDrug);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedTreatmentDrug.getId()).isNotNull();
        TreatmentDrug persistedTreatmentDrug = treatmentDrugRepository.findById(savedTreatmentDrug.getId()).orElseThrow();
        Assertions.assertThat(persistedTreatmentDrug.getTreatment().getId()).isEqualTo(t8.getId());
        Assertions.assertThat(persistedTreatmentDrug.getDrug().getId()).isEqualTo(dD.getId());
        Assertions.assertThat(persistedTreatmentDrug.getUnits()).isEqualTo(2);
        Assertions.assertThat(persistedTreatmentDrug.getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("7.00"));
        Assertions.assertThat(persistedTreatmentDrug.getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("9.50"));
    }

    @Test
    public void treatmentDrugRepository_save_DuplicateTreatmentAndDrugPair_ThrowsDataIntegrityViolation() {
        // Arrange
        // r1 ya ocupa el par (t1, dA), protegido por la restricción única (treatment_id, drug_id).
        TreatmentDrug duplicateTreatmentDrug = RepositoryTestData.treatmentDrug(
                t1,
                dA,
                1,
                new BigDecimal("10.00"),
                new BigDecimal("15.00")
        );

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentDrugRepository.saveAndFlush(duplicateTreatmentDrug));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentDrugRepository_save_NullTreatment_ThrowsDataIntegrityViolation() {
        // Arrange
        TreatmentDrug treatmentDrug = unsavedTreatmentDrug();
        treatmentDrug.setTreatment(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentDrugRepository.saveAndFlush(treatmentDrug));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentDrugRepository_save_NullUnitSalePrice_ThrowsDataIntegrityViolation() {
        // Arrange
        TreatmentDrug treatmentDrug = unsavedTreatmentDrug();
        treatmentDrug.setUnitSalePrice(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentDrugRepository.saveAndFlush(treatmentDrug));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentDrugRepository_save_NullUnits_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna units es obligatoria en la base de datos.
        TreatmentDrug treatmentDrug = unsavedTreatmentDrug();
        treatmentDrug.setUnits(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentDrugRepository.saveAndFlush(treatmentDrug));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentDrugRepository_save_NullUnitPurchasePrice_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna unit_purchase_price es obligatoria en la base de datos.
        TreatmentDrug treatmentDrug = unsavedTreatmentDrug();
        treatmentDrug.setUnitPurchasePrice(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentDrugRepository.saveAndFlush(treatmentDrug));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentDrugRepository_findAll_ExistingRows_ReturnsAllFixtureRows() {
        // Arrange
        // setUp creó exactamente nueve filas: r1, r2, r3, r4, r5, r6, r7, r8 y r9.

        // Act
        List<TreatmentDrug> treatmentDrugs = treatmentDrugRepository.findAll();

        // Assert
        Assertions.assertThat(treatmentDrugs)
                .extracting(TreatmentDrug::getId)
                .containsExactlyInAnyOrder(
                        r1.getId(),
                        r2.getId(),
                        r3.getId(),
                        r4.getId(),
                        r5.getId(),
                        r6.getId(),
                        r7.getId(),
                        r8.getId(),
                        r9.getId()
                );
    }

    @Test
    public void treatmentDrugRepository_findAll_NoRows_ReturnsEmptyList() {
        // Arrange
        treatmentDrugRepository.deleteAll();
        treatmentDrugRepository.flush();
        entityManager.clear();

        // Act
        List<TreatmentDrug> treatmentDrugs = treatmentDrugRepository.findAll();

        // Assert
        Assertions.assertThat(treatmentDrugs).isEmpty();
    }

    @Test
    public void treatmentDrugRepository_findById_ExistingId_ReturnsTreatmentDrug() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<TreatmentDrug> result = treatmentDrugRepository.findById(r1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        TreatmentDrug foundTreatmentDrug = result.orElseThrow();
        Assertions.assertThat(foundTreatmentDrug.getTreatment().getId()).isEqualTo(t1.getId());
        Assertions.assertThat(foundTreatmentDrug.getDrug().getId()).isEqualTo(dA.getId());
        Assertions.assertThat(foundTreatmentDrug.getUnits()).isEqualTo(2);
        Assertions.assertThat(foundTreatmentDrug.getUnitPurchasePrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        Assertions.assertThat(foundTreatmentDrug.getUnitSalePrice()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    public void treatmentDrugRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<TreatmentDrug> result = treatmentDrugRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void treatmentDrugRepository_deleteById_ExistingId_RemovesOnlyThatRowAndKeepsTreatmentAndDrug() {
        // Arrange
        Long deletedId = r1.getId();

        // Act
        treatmentDrugRepository.deleteById(deletedId);
        treatmentDrugRepository.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(treatmentDrugRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(treatmentDrugRepository.findAll())
                .extracting(TreatmentDrug::getId)
                .containsExactlyInAnyOrder(
                        r2.getId(),
                        r3.getId(),
                        r4.getId(),
                        r5.getId(),
                        r6.getId(),
                        r7.getId(),
                        r8.getId(),
                        r9.getId()
                );
        Assertions.assertThat(treatmentRepository.findById(t1.getId())).isPresent();
        Assertions.assertThat(drugRepository.findById(dA.getId())).isPresent();
    }

    @Test
    public void treatmentDrugRepository_deleteTreatment_RemovesItsTreatmentDrugRows() {
        // Arrange
        // Las filas se guardaron a través de su propio repositorio, por lo que la colección en
        // memoria del tratamiento gestionado está vacía: se recarga desde la base para que la
        // cascada de borrado actúe sobre las filas reales de t1.
        entityManager.flush();
        entityManager.clear();
        Treatment treatmentToDelete = treatmentRepository.findById(t1.getId()).orElseThrow();

        // Act
        treatmentRepository.delete(treatmentToDelete);
        treatmentRepository.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(treatmentDrugRepository.findByTreatmentId(t1.getId())).isEmpty();
        // t2 conserva sus dos filas (r3 y r4).
        Assertions.assertThat(treatmentDrugRepository.findByTreatmentId(t2.getId()))
                .extracting(TreatmentDrug::getId)
                .containsExactlyInAnyOrder(r3.getId(), r4.getId());
        Assertions.assertThat(drugRepository.findById(dA.getId())).isPresent();
        Assertions.assertThat(drugRepository.findById(dB.getId())).isPresent();
    }

    @Test
    public void treatmentDrugRepository_findByTreatmentId_TreatmentWithDrugs_ReturnsOnlyThatTreatmentRows() {
        // Arrange
        // t1 posee r1 y r2.

        // Act
        List<TreatmentDrug> treatmentDrugs = treatmentDrugRepository.findByTreatmentId(t1.getId());

        // Assert
        Assertions.assertThat(treatmentDrugs)
                .extracting(TreatmentDrug::getId)
                .containsExactlyInAnyOrder(r1.getId(), r2.getId());
    }

    @Test
    public void treatmentDrugRepository_findByTreatmentId_TreatmentWithoutDrugs_ReturnsEmptyList() {
        // Arrange
        // t8 existe pero no tiene filas de medicamentos.

        // Act
        List<TreatmentDrug> treatmentDrugs = treatmentDrugRepository.findByTreatmentId(t8.getId());

        // Assert
        Assertions.assertThat(treatmentDrugs).isEmpty();
    }

    @Test
    public void treatmentDrugRepository_findByTreatmentId_NonExistingTreatmentId_ReturnsEmptyList() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id de tratamiento generado.

        // Act
        List<TreatmentDrug> treatmentDrugs = treatmentDrugRepository.findByTreatmentId(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(treatmentDrugs).isEmpty();
    }

    @Test
    public void treatmentDrugRepository_existsByTreatmentIdAndDrugId_ExistingPair_ReturnsTrue() {
        // Arrange
        // r1 relaciona t1 con dA.

        // Act
        boolean exists = treatmentDrugRepository.existsByTreatmentIdAndDrugId(t1.getId(), dA.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void treatmentDrugRepository_existsByTreatmentIdAndDrugId_ExistingTreatmentAndDrugButNotRelated_ReturnsFalse() {
        // Arrange
        // t1 y dC existen, pero ninguna fila los relaciona.

        // Act
        boolean exists = treatmentDrugRepository.existsByTreatmentIdAndDrugId(t1.getId(), dC.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void treatmentDrugRepository_existsByTreatmentIdAndDrugId_NonExistingIds_ReturnsFalse() {
        // Arrange
        // Long.MAX_VALUE no puede ser un id generado para ninguna de las dos entidades.

        // Act
        boolean existsWithUnknownTreatment = treatmentDrugRepository.existsByTreatmentIdAndDrugId(Long.MAX_VALUE, dA.getId());
        boolean existsWithUnknownDrug = treatmentDrugRepository.existsByTreatmentIdAndDrugId(t1.getId(), Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(existsWithUnknownTreatment).isFalse();
        Assertions.assertThat(existsWithUnknownDrug).isFalse();
    }

    @Test
    public void treatmentDrugRepository_existsByDrugId_UsedDrug_ReturnsTrue() {
        // Arrange
        // dA aparece en r1, r3 y r6.

        // Act
        boolean exists = treatmentDrugRepository.existsByDrugId(dA.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void treatmentDrugRepository_existsByDrugId_UnusedDrug_ReturnsFalse() {
        // Arrange
        // dD no tiene filas, aunque su unitsSold sea 999.

        // Act
        boolean exists = treatmentDrugRepository.existsByDrugId(dD.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void treatmentDrugRepository_existsByDrugId_NonExistingDrugId_ReturnsFalse() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id de medicamento generado.

        // Act
        boolean exists = treatmentDrugRepository.existsByDrugId(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void treatmentDrugRepository_findMedicationTreatmentCountsBetween_FullWindowExcludingRecovered_ReturnsCountsOrderedByName() {
        // Arrange
        // Dentro de [WINDOW_START, WINDOW_END) quedan t1, t2, t3 y t8; se excluye RECOVERED (t6).
        // t4 cae justo en el fin, t5 justo antes del inicio y t7 muy anterior al inicio.

        // Act
        List<MedicationTreatmentCountProjection> counts = treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                WINDOW_START,
                WINDOW_END,
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(counts)
                .extracting(
                        MedicationTreatmentCountProjection::getMedicationName,
                        MedicationTreatmentCountProjection::getTreatmentCount
                )
                .containsExactly(
                        Assertions.tuple("Amoxicilina 100mg", 1L),
                        Assertions.tuple("Amoxicilina 250mg", 2L),
                        Assertions.tuple("Ibuprofeno 100mg", 2L)
                );
    }

    @Test
    public void treatmentDrugRepository_findMedicationTreatmentCountsBetween_TreatmentExactlyAtStart_IncludesIt() {
        // Arrange
        // t1 tiene fecha exactamente en WINDOW_START y es el único tratamiento del rango.

        // Act
        List<MedicationTreatmentCountProjection> counts = treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                t1.getDate(),
                t1.getDate().plusSeconds(1),
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(counts)
                .extracting(
                        MedicationTreatmentCountProjection::getMedicationName,
                        MedicationTreatmentCountProjection::getTreatmentCount
                )
                .containsExactly(
                        Assertions.tuple("Amoxicilina 250mg", 1L),
                        Assertions.tuple("Ibuprofeno 100mg", 1L)
                );
    }

    @Test
    public void treatmentDrugRepository_findMedicationTreatmentCountsBetween_TreatmentExactlyAtEnd_ExcludesIt() {
        // Arrange
        // t3 está justo en WINDOW_END.minusSeconds(1) y t4 justo en WINDOW_END, que queda fuera.

        // Act
        List<MedicationTreatmentCountProjection> counts = treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                WINDOW_END.minusSeconds(1),
                WINDOW_END,
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(counts)
                .extracting(
                        MedicationTreatmentCountProjection::getMedicationName,
                        MedicationTreatmentCountProjection::getTreatmentCount
                )
                .containsExactly(Assertions.tuple("Ibuprofeno 100mg", 1L));
    }

    @Test
    public void treatmentDrugRepository_findMedicationTreatmentCountsBetween_WindowWithoutTreatments_ReturnsEmptyList() {
        // Arrange
        // La ventana empieza después de WINDOW_END, donde no cae ningún tratamiento de las fixtures.

        // Act
        List<MedicationTreatmentCountProjection> counts = treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                WINDOW_END.plusDays(1),
                WINDOW_END.plusDays(2),
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(counts).isEmpty();
    }

    @Test
    public void treatmentDrugRepository_findMedicationTreatmentCountsBetween_ExcludedEvolutionIsImproving_ExcludesOnlyThatEvolution() {
        // Arrange
        // Con IMPROVING excluido sobreviven t1, t3 y t6; t2 (IMPROVING) queda fuera.
        // t5 y t7 también son IMPROVING y además caen fuera de la ventana.

        // Act
        List<MedicationTreatmentCountProjection> counts = treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                WINDOW_START,
                WINDOW_END,
                Evolution.IMPROVING
        );

        // Assert
        Assertions.assertThat(counts)
                .extracting(
                        MedicationTreatmentCountProjection::getMedicationName,
                        MedicationTreatmentCountProjection::getTreatmentCount
                )
                .containsExactly(
                        Assertions.tuple("Amoxicilina 250mg", 1L),
                        Assertions.tuple("Ibuprofeno 100mg", 3L)
                );
    }

    @Test
    public void treatmentDrugRepository_findMedicationTreatmentCountsBetween_NullExcludedEvolution_CountsOnlyAdmissions() {
        // Arrange
        // t1 es el único tratamiento cuya evolución es null dentro de la ventana.

        // Act
        List<MedicationTreatmentCountProjection> counts = treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                WINDOW_START,
                WINDOW_END,
                null
        );

        // Assert
        // Fija la lógica trivaluada de SQL de la consulta actual: comparar una evolución
        // contra un parámetro NULL produce UNKNOWN, por lo que solo cuentan las filas con
        // evolution IS NULL.
        Assertions.assertThat(counts)
                .extracting(
                        MedicationTreatmentCountProjection::getMedicationName,
                        MedicationTreatmentCountProjection::getTreatmentCount
                )
                .containsExactly(
                        Assertions.tuple("Amoxicilina 250mg", 1L),
                        Assertions.tuple("Ibuprofeno 100mg", 1L)
                );
    }

    @Test
    public void treatmentDrugRepository_sumAllTimeSales_ExistingRows_ReturnsSumOfSnapshotSalePriceTimesUnits() {
        // Arrange
        // La suma usa el precio de venta guardado en cada fila, no el precio de catálogo
        // del medicamento: r1 aporta 30, r2 8, r3 45, r4 6, r5 30, r6 15, r7 10, r8 16 y r9 24.

        // Act
        BigDecimal sales = treatmentDrugRepository.sumAllTimeSales();

        // Assert
        Assertions.assertThat(sales).isEqualByComparingTo(new BigDecimal("184.00"));
    }

    @Test
    public void treatmentDrugRepository_sumAllTimeSales_NoRows_ReturnsNull() {
        // Arrange
        treatmentDrugRepository.deleteAll();
        treatmentDrugRepository.flush();
        entityManager.clear();

        // Act
        BigDecimal sales = treatmentDrugRepository.sumAllTimeSales();

        // Assert
        Assertions.assertThat(sales).isNull();
    }

    @Test
    public void treatmentDrugRepository_sumAllTimeGrossProfit_ExistingRows_ReturnsSumOfMarginTimesUnits() {
        // Arrange
        // Incluye la fila r9 con margen negativo: 10 + 3 + 15 + 2 + 10 + 5 + 4 + 6 - 12.

        // Act
        BigDecimal grossProfit = treatmentDrugRepository.sumAllTimeGrossProfit();

        // Assert
        Assertions.assertThat(grossProfit).isEqualByComparingTo(new BigDecimal("43.00"));
    }

    @Test
    public void treatmentDrugRepository_sumAllTimeGrossProfit_NoRows_ReturnsNull() {
        // Arrange
        treatmentDrugRepository.deleteAll();
        treatmentDrugRepository.flush();
        entityManager.clear();

        // Act
        BigDecimal grossProfit = treatmentDrugRepository.sumAllTimeGrossProfit();

        // Assert
        Assertions.assertThat(grossProfit).isNull();
    }

    @Test
    public void treatmentDrugRepository_findTopMedicationsByAllTimeUnitsSold_ExistingRows_ReturnsOrderedByUnitsDescThenName() {
        // Arrange
        // Unidades por medicamento: dB 7, dA 6, dC 6 y dE 1; dA y dC empatan y se ordenan por nombre.

        // Act
        List<TopMedicationUnitsProjection> topMedications = treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(
                PageRequest.of(0, 10)
        );

        // Assert
        Assertions.assertThat(topMedications)
                .extracting(
                        TopMedicationUnitsProjection::getMedicationName,
                        TopMedicationUnitsProjection::getUnitsSold
                )
                .containsExactly(
                        Assertions.tuple("Ibuprofeno 100mg", 7L),
                        Assertions.tuple("Amoxicilina 250mg", 6L),
                        Assertions.tuple("Zinc", 6L),
                        Assertions.tuple("Amoxicilina 100mg", 1L)
                );
        // dD queda fuera aunque su unitsSold sea 999: la consulta suma filas de tratamiento.
        Assertions.assertThat(topMedications)
                .extracting(TopMedicationUnitsProjection::getMedicationName)
                .doesNotContain("Vitamina B");
    }

    @Test
    public void treatmentDrugRepository_findTopMedicationsByAllTimeUnitsSold_FirstPageOfSizeOne_ReturnsOnlyTopMedication() {
        // Arrange
        // dB acumula 7 unidades, el mayor total de las fixtures.

        // Act
        List<TopMedicationUnitsProjection> topMedications = treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(
                PageRequest.of(0, 1)
        );

        // Assert
        Assertions.assertThat(topMedications)
                .extracting(TopMedicationUnitsProjection::getMedicationName)
                .containsExactly("Ibuprofeno 100mg");
    }

    @Test
    public void treatmentDrugRepository_findTopMedicationsByAllTimeUnitsSold_SecondPageOfSizeTwo_ReturnsRemainingMedications() {
        // Arrange
        // El orden global es dB 7, dA 6, dC 6 y dE 1; la segunda página de tamaño dos devuelve los dos últimos.

        // Act
        List<TopMedicationUnitsProjection> topMedications = treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(
                PageRequest.of(1, 2)
        );

        // Assert
        Assertions.assertThat(topMedications)
                .extracting(TopMedicationUnitsProjection::getMedicationName)
                .containsExactly("Zinc", "Amoxicilina 100mg");
    }

    @Test
    public void treatmentDrugRepository_findTopMedicationsByAllTimeUnitsSold_PageBeyondLastResult_ReturnsEmptyList() {
        // Arrange
        // Solo existen cuatro medicamentos con filas, así que la página nueve está muy lejos.

        // Act
        List<TopMedicationUnitsProjection> topMedications = treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(
                PageRequest.of(9, 3)
        );

        // Assert
        Assertions.assertThat(topMedications).isEmpty();
    }

    @Test
    public void treatmentDrugRepository_findTopMedicationsByAllTimeUnitsSold_NoRows_ReturnsEmptyList() {
        // Arrange
        treatmentDrugRepository.deleteAll();
        treatmentDrugRepository.flush();
        entityManager.clear();

        // Act
        List<TopMedicationUnitsProjection> topMedications = treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(
                PageRequest.of(0, 10)
        );

        // Assert
        Assertions.assertThat(topMedications).isEmpty();
    }

    private Owner saveOwner() {
        return ownerRepository.save(RepositoryTestData.owner());
    }

    private Pet savePet(Owner owner) {
        return petRepository.save(RepositoryTestData.pet(owner));
    }

    private Veterinarian saveVeterinarian() {
        return veterinarianRepository.save(RepositoryTestData.veterinarian());
    }

    private Treatment saveTreatment(Pet pet, Veterinarian veterinarian, LocalDateTime date, Evolution evolution) {
        return treatmentRepository.save(RepositoryTestData.treatment(pet, veterinarian, date, evolution));
    }

    private TreatmentDrug unsavedTreatmentDrug() {
        // Fila válida para t8 y dD que todavía no está persistida.
        return RepositoryTestData.treatmentDrug(t8, dD, 2, new BigDecimal("7.00"), new BigDecimal("9.50"));
    }

    private Drug saveDrug(String name) {
        return drugRepository.save(RepositoryTestData.drug(name));
    }

    private Drug saveDrug(String name, int unitsSold) {
        Drug drug = RepositoryTestData.drug(name);
        drug.setUnitsSold(unitsSold);
        return drugRepository.save(drug);
    }

    private TreatmentDrug saveTreatmentDrug(
            Treatment treatment,
            Drug drug,
            int units,
            BigDecimal unitPurchasePrice,
            BigDecimal unitSalePrice
    ) {
        return treatmentDrugRepository.save(
                RepositoryTestData.treatmentDrug(treatment, drug, units, unitPurchasePrice, unitSalePrice)
        );
    }
}
