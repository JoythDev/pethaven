package io.github.pethaven.repository;

import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DataJpaTest
public class DrugRepositoryTest {

    private static final LocalDateTime BASE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final DrugRepository drugRepository;
    private final TreatmentDrugRepository treatmentDrugRepository;
    private final TreatmentRepository treatmentRepository;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final EntityManager entityManager;

    private Owner owner1;
    private Pet pet1;
    private Veterinarian vet1;
    private Treatment t1;
    private Drug d1;
    private Drug d2;
    private Drug d3;
    private Drug d4;

    @Autowired
    public DrugRepositoryTest(
            DrugRepository drugRepository,
            TreatmentDrugRepository treatmentDrugRepository,
            TreatmentRepository treatmentRepository,
            PetRepository petRepository,
            OwnerRepository ownerRepository,
            VeterinarianRepository veterinarianRepository,
            EntityManager entityManager
    ) {
        this.drugRepository = drugRepository;
        this.treatmentDrugRepository = treatmentDrugRepository;
        this.treatmentRepository = treatmentRepository;
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        owner1 = ownerRepository.save(RepositoryTestData.owner());
        pet1 = petRepository.save(RepositoryTestData.pet(owner1));
        vet1 = veterinarianRepository.save(RepositoryTestData.veterinarian());

        // Las unidades disponibles se fijan antes de guardar para que los umbrales de las consultas
        // derivadas dependan de las fixtures y no del valor por defecto del builder.
        d1 = saveDrug("Amoxicilina 250mg", 10);
        d2 = saveDrug("Ibuprofeno 100mg", 1);
        d3 = saveDrug("Paracetamol 500mg", 0);
        d4 = saveDrug("Vitamina B", 5);

        // d1 es el único medicamento con una fila de venta, así que su clave foránea impide borrarlo.
        t1 = saveTreatment();
        saveTreatmentDrug(t1, d1);

        entityManager.flush();
    }

    @Test
    public void drugRepository_save_ValidDrug_ReturnsPersistedDrugWithGeneratedId() {
        // Arrange
        // El builder aplica los valores de catálogo y deja la colección treatmentDrugs vacía.
        Drug drug = RepositoryTestData.drug();
        String name = drug.getName();
        BigDecimal purchasePrice = drug.getPurchasePrice();
        BigDecimal salePrice = drug.getSalePrice();
        Integer unitsAvailable = drug.getUnitsAvailable();
        Integer unitsSold = drug.getUnitsSold();

        // Act
        Drug savedDrug = drugRepository.save(drug);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedDrug.getId()).isNotNull();
        Drug persistedDrug = drugRepository.findById(savedDrug.getId()).orElseThrow();
        Assertions.assertThat(persistedDrug.getName()).isEqualTo(name);
        Assertions.assertThat(persistedDrug.getPurchasePrice()).isEqualByComparingTo(purchasePrice);
        Assertions.assertThat(persistedDrug.getSalePrice()).isEqualByComparingTo(salePrice);
        Assertions.assertThat(persistedDrug.getUnitsAvailable()).isEqualTo(unitsAvailable);
        Assertions.assertThat(persistedDrug.getUnitsSold()).isEqualTo(unitsSold);
        // Un medicamento sin filas de venta recupera una colección vacía.
        Assertions.assertThat(persistedDrug.getTreatmentDrugs()).isEmpty();
    }

    @Test
    public void drugRepository_save_ExistingDrug_UpdatesPersistedValues() {
        // Arrange
        entityManager.flush();
        entityManager.clear();
        Drug drugToUpdate = drugRepository.findById(d1.getId()).orElseThrow();
        drugToUpdate.setName("Amoxicilina 500mg");
        drugToUpdate.setPurchasePrice(new BigDecimal("3.25"));
        drugToUpdate.setSalePrice(new BigDecimal("5.50"));
        drugToUpdate.setUnitsAvailable(25);
        drugToUpdate.setUnitsSold(7);

        // Act
        drugRepository.save(drugToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Drug persistedDrug = drugRepository.findById(d1.getId()).orElseThrow();
        Assertions.assertThat(persistedDrug.getId()).isEqualTo(d1.getId());
        Assertions.assertThat(persistedDrug.getName()).isEqualTo("Amoxicilina 500mg");
        Assertions.assertThat(persistedDrug.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("3.25"));
        Assertions.assertThat(persistedDrug.getSalePrice()).isEqualByComparingTo(new BigDecimal("5.50"));
        Assertions.assertThat(persistedDrug.getUnitsAvailable()).isEqualTo(25);
        Assertions.assertThat(persistedDrug.getUnitsSold()).isEqualTo(7);
        Assertions.assertThat(drugRepository.count()).isEqualTo(4L);
    }

    @Test
    public void drugRepository_save_DuplicateName_ThrowsDataIntegrityViolation() {
        // Arrange
        // d1 ya ocupa su nombre, protegido por la restricción única de la columna name.
        Drug duplicateDrug = RepositoryTestData.drug(d1.getName());

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> drugRepository.saveAndFlush(duplicateDrug));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_save_NullName_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna name es obligatoria en la base de datos.
        Drug drugWithNullName = RepositoryTestData.drug();
        drugWithNullName.setName(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> drugRepository.saveAndFlush(drugWithNullName));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_save_NullPurchasePrice_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna purchase_price es obligatoria en la base de datos.
        Drug drugWithNullPurchasePrice = RepositoryTestData.drug();
        drugWithNullPurchasePrice.setPurchasePrice(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> drugRepository.saveAndFlush(drugWithNullPurchasePrice));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_save_NullSalePrice_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna sale_price es obligatoria en la base de datos.
        Drug drugWithNullSalePrice = RepositoryTestData.drug();
        drugWithNullSalePrice.setSalePrice(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> drugRepository.saveAndFlush(drugWithNullSalePrice));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_save_NullUnitsAvailable_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna units_available es obligatoria en la base de datos.
        Drug drugWithNullUnitsAvailable = RepositoryTestData.drug();
        drugWithNullUnitsAvailable.setUnitsAvailable(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> drugRepository.saveAndFlush(drugWithNullUnitsAvailable));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_save_NullUnitsSold_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna units_sold es obligatoria en la base de datos.
        Drug drugWithNullUnitsSold = RepositoryTestData.drug();
        drugWithNullUnitsSold.setUnitsSold(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> drugRepository.saveAndFlush(drugWithNullUnitsSold));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_findById_ExistingId_ReturnsDrug() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Drug> result = drugRepository.findById(d1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Drug foundDrug = result.orElseThrow();
        Assertions.assertThat(foundDrug.getId()).isEqualTo(d1.getId());
        Assertions.assertThat(foundDrug.getName()).isEqualTo(d1.getName());
        Assertions.assertThat(foundDrug.getPurchasePrice()).isEqualByComparingTo(d1.getPurchasePrice());
        Assertions.assertThat(foundDrug.getSalePrice()).isEqualByComparingTo(d1.getSalePrice());
        Assertions.assertThat(foundDrug.getUnitsAvailable()).isEqualTo(d1.getUnitsAvailable());
        Assertions.assertThat(foundDrug.getUnitsSold()).isEqualTo(d1.getUnitsSold());
    }

    @Test
    public void drugRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<Drug> result = drugRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void drugRepository_findAll_ExistingDrugs_ReturnsAllFixtureDrugs() {
        // Arrange
        // setUp creó exactamente cuatro medicamentos: d1, d2, d3 y d4.

        // Act
        List<Drug> drugs = drugRepository.findAll();

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactlyInAnyOrder(d1.getId(), d2.getId(), d3.getId(), d4.getId());
    }

    @Test
    public void drugRepository_deleteById_DrugWithoutTreatmentRows_RemovesOnlyThatDrug() {
        // Arrange
        // d4 no tiene filas de venta, así que el borrado no depende de ninguna clave foránea.
        Long deletedId = d4.getId();
        long countBeforeDeletion = drugRepository.count();

        // Act
        drugRepository.deleteById(deletedId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(drugRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(drugRepository.count()).isEqualTo(countBeforeDeletion - 1);
        Assertions.assertThat(drugRepository.findById(d1.getId())).isPresent();
        Assertions.assertThat(drugRepository.findById(d2.getId())).isPresent();
        Assertions.assertThat(drugRepository.findById(d3.getId())).isPresent();
    }

    @Test
    public void drugRepository_deleteById_DrugReferencedByTreatmentDrug_ThrowsDataIntegrityViolation() {
        // Arrange
        // La clave foránea de treatment_drugs protege el historial de ventas del medicamento.
        entityManager.flush();
        entityManager.clear();

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> {
            drugRepository.deleteById(d1.getId());
            drugRepository.flush();
        });

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void drugRepository_findByName_ExistingName_ReturnsDrug() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Drug> result = drugRepository.findByName("Ibuprofeno 100mg");

        // Assert
        Assertions.assertThat(result).isPresent();
        Drug foundDrug = result.orElseThrow();
        Assertions.assertThat(foundDrug.getId()).isEqualTo(d2.getId());
        Assertions.assertThat(foundDrug.getName()).isEqualTo(d2.getName());
        Assertions.assertThat(foundDrug.getUnitsAvailable()).isEqualTo(d2.getUnitsAvailable());
    }

    @Test
    public void drugRepository_findByName_UnknownName_ReturnsEmptyOptional() {
        // Arrange
        // "Unknown drug" no existe en las fixtures.

        // Act
        Optional<Drug> result = drugRepository.findByName("Unknown drug");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void drugRepository_existsByName_ExistingName_ReturnsTrue() {
        // Arrange
        // d1 ocupa su propio nombre.

        // Act
        boolean exists = drugRepository.existsByName("Amoxicilina 250mg");

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void drugRepository_existsByName_UnknownName_ReturnsFalse() {
        // Arrange
        // "Unknown drug" no existe en las fixtures.

        // Act
        boolean exists = drugRepository.existsByName("Unknown drug");

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void drugRepository_findByNameIn_TwoExistingNames_ReturnsThoseDrugs() {
        // Arrange
        // Los dos nombres pertenecen a d1 y d3.

        // Act
        List<Drug> drugs = drugRepository.findByNameIn(List.of("Amoxicilina 250mg", "Paracetamol 500mg"));

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactlyInAnyOrder(d1.getId(), d3.getId());
    }

    @Test
    public void drugRepository_findByNameIn_OneExistingAndOneUnknownName_ReturnsOnlyExistingDrug() {
        // Arrange
        // El primer nombre pertenece a d2 y el segundo no existe.

        // Act
        List<Drug> drugs = drugRepository.findByNameIn(List.of("Ibuprofeno 100mg", "Unknown drug"));

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactly(d2.getId());
    }

    @Test
    public void drugRepository_findByNameIn_UnknownNames_ReturnsEmptyList() {
        // Arrange
        // Ninguno de los dos nombres existe en las fixtures.

        // Act
        List<Drug> drugs = drugRepository.findByNameIn(List.of("Unknown drug A", "Unknown drug B"));

        // Assert
        Assertions.assertThat(drugs).isEmpty();
    }

    @Test
    public void drugRepository_findByNameIn_EmptyCollection_ReturnsEmptyList() {
        // Arrange
        // Una colección vacía no contiene ningún nombre que pueda coincidir.

        // Act
        List<Drug> drugs = drugRepository.findByNameIn(List.of());

        // Assert
        Assertions.assertThat(drugs).isEmpty();
    }

    @Test
    public void drugRepository_findByNameIn_DuplicatedExistingName_ReturnsDrugOnce() {
        // Arrange
        // El nombre de d4 se repite dos veces en la colección de entrada.

        // Act
        List<Drug> drugs = drugRepository.findByNameIn(List.of("Vitamina B", "Vitamina B"));

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactly(d4.getId());
    }

    @Test
    public void drugRepository_existsByNameAndIdNot_SameNameWithOwnId_ReturnsFalse() {
        // Arrange
        // El nombre pertenece a d2 y se excluye su propio id, así que ningún otro medicamento lo tiene.

        // Act
        boolean exists = drugRepository.existsByNameAndIdNot(d2.getName(), d2.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void drugRepository_existsByNameAndIdNot_SameNameWithAnotherDrugId_ReturnsTrue() {
        // Arrange
        // El nombre pertenece a d2 y se consulta excluyendo el id de d3.

        // Act
        boolean exists = drugRepository.existsByNameAndIdNot(d2.getName(), d3.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void drugRepository_existsByNameAndIdNot_UnknownName_ReturnsFalse() {
        // Arrange
        // "Unknown drug" no existe en las fixtures, sea cual sea el id excluido.

        // Act
        boolean exists = drugRepository.existsByNameAndIdNot("Unknown drug", d1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void drugRepository_findByUnitsAvailableGreaterThan_Zero_ReturnsDrugsAboveZero() {
        // Arrange
        // d3 tiene exactamente 0 unidades y queda excluido por el límite estricto.

        // Act
        List<Drug> drugs = drugRepository.findByUnitsAvailableGreaterThan(0);

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactlyInAnyOrder(d1.getId(), d2.getId(), d4.getId());
    }

    @Test
    public void drugRepository_findByUnitsAvailableGreaterThan_One_ReturnsDrugsStrictlyAboveOne() {
        // Arrange
        // d2 tiene exactamente 1 unidad y queda excluido por el límite estricto.

        // Act
        List<Drug> drugs = drugRepository.findByUnitsAvailableGreaterThan(1);

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactlyInAnyOrder(d1.getId(), d4.getId());
    }

    @Test
    public void drugRepository_findByUnitsAvailableGreaterThan_MaximumAvailable_ReturnsEmptyList() {
        // Arrange
        // d1 tiene exactamente 10 unidades, el máximo de las fixtures, y el límite es estricto.

        // Act
        List<Drug> drugs = drugRepository.findByUnitsAvailableGreaterThan(10);

        // Assert
        Assertions.assertThat(drugs).isEmpty();
    }

    @Test
    public void drugRepository_findByUnitsAvailableGreaterThan_NegativeBound_ReturnsAllDrugs() {
        // Arrange
        // Las unidades de los cuatro medicamentos son mayores que -1.

        // Act
        List<Drug> drugs = drugRepository.findByUnitsAvailableGreaterThan(-1);

        // Assert
        Assertions.assertThat(drugs)
                .extracting(Drug::getId)
                .containsExactlyInAnyOrder(d1.getId(), d2.getId(), d3.getId(), d4.getId());
    }

    @Test
    public void drugRepository_existsByUnitsAvailableGreaterThan_Zero_ReturnsTrue() {
        // Arrange
        // d2 tiene 1 unidad disponible, que es mayor que 0.

        // Act
        boolean exists = drugRepository.existsByUnitsAvailableGreaterThan(0);

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void drugRepository_existsByUnitsAvailableGreaterThan_ValueBelowMaximum_ReturnsTrue() {
        // Arrange
        // d1 tiene 10 unidades, que es mayor que 9.

        // Act
        boolean exists = drugRepository.existsByUnitsAvailableGreaterThan(9);

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void drugRepository_existsByUnitsAvailableGreaterThan_MaximumAvailable_ReturnsFalse() {
        // Arrange
        // Ningún medicamento supera las 10 unidades de d1, el máximo de las fixtures.

        // Act
        boolean exists = drugRepository.existsByUnitsAvailableGreaterThan(10);

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void drugRepository_existsByUnitsAvailableGreaterThan_AllDrugsWithoutUnits_ReturnsFalse() {
        // Arrange
        // Se agotan las unidades de los cuatro medicamentos antes de consultar el umbral.
        d1.setUnitsAvailable(0);
        drugRepository.saveAndFlush(d1);
        d2.setUnitsAvailable(0);
        drugRepository.saveAndFlush(d2);
        d3.setUnitsAvailable(0);
        drugRepository.saveAndFlush(d3);
        d4.setUnitsAvailable(0);
        drugRepository.saveAndFlush(d4);

        // Act
        boolean exists = drugRepository.existsByUnitsAvailableGreaterThan(0);

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    private Drug saveDrug(String name, int unitsAvailable) {
        Drug drug = RepositoryTestData.drug(name);
        drug.setUnitsAvailable(unitsAvailable);
        return drugRepository.save(drug);
    }

    private Treatment saveTreatment() {
        return treatmentRepository.save(RepositoryTestData.treatment(pet1, vet1, BASE_DATE, null));
    }

    private TreatmentDrug saveTreatmentDrug(Treatment treatment, Drug drug) {
        TreatmentDrug treatmentDrug = RepositoryTestData.treatmentDrug(
                treatment,
                drug,
                2,
                new BigDecimal("1.00"),
                new BigDecimal("2.00")
        );
        return treatmentDrugRepository.save(treatmentDrug);
    }
}
