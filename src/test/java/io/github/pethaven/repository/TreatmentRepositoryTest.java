package io.github.pethaven.repository;

import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DataJpaTest
public class TreatmentRepositoryTest {

    private static final LocalDateTime BASE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final TreatmentRepository treatmentRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final EntityManager entityManager;

    private Owner owner1;
    private Owner owner2;
    private Owner owner3;
    private Pet pet1;
    private Pet pet2;
    private Pet pet3;
    private Veterinarian vet1;
    private Veterinarian vet2;
    private Treatment t1;
    private Treatment t2;
    private Treatment t3;
    private Treatment t4;

    @Autowired
    public TreatmentRepositoryTest(
            TreatmentRepository treatmentRepository,
            VeterinarianRepository veterinarianRepository,
            PetRepository petRepository,
            OwnerRepository ownerRepository,
            EntityManager entityManager
    ) {
        this.treatmentRepository = treatmentRepository;
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

        owner2 = saveOwner();
        pet3 = savePet(owner2);

        owner3 = saveOwner();

        vet1 = saveVeterinarian();
        vet2 = saveVeterinarian();

        t1 = saveTreatment(pet1, vet1, BASE_DATE, null);
        t2 = saveTreatment(pet1, vet1, BASE_DATE.plusDays(2), Evolution.IMPROVING);
        t3 = saveTreatment(pet2, vet2, BASE_DATE.plusDays(4), Evolution.WORSENING);
        t4 = saveTreatment(pet2, vet2, BASE_DATE.plusDays(6), Evolution.RECOVERED);

        entityManager.flush();
    }

    @Test
    public void treatmentRepository_save_ValidTreatment_ReturnsPersistedTreatmentWithGeneratedId() {
        // Arrange
        LocalDateTime treatmentDate = BASE_DATE.plusDays(10);
        Treatment treatment = Treatment.builder()
                .pet(pet1)
                .veterinarian(vet1)
                .date(treatmentDate)
                .weight(12.75)
                .diagnosis("Otitis externa")
                .evolution(Evolution.STABLE)
                .notes("Recheck in one week")
                .build();

        // Act
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedTreatment.getId()).isNotNull();
        Treatment persistedTreatment = treatmentRepository.findById(savedTreatment.getId()).orElseThrow();
        Assertions.assertThat(persistedTreatment.getPet().getId()).isEqualTo(pet1.getId());
        Assertions.assertThat(persistedTreatment.getVeterinarian().getId()).isEqualTo(vet1.getId());
        Assertions.assertThat(persistedTreatment.getDate()).isEqualTo(treatmentDate);
        Assertions.assertThat(persistedTreatment.getWeight()).isEqualTo(12.75);
        Assertions.assertThat(persistedTreatment.getDiagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(persistedTreatment.getEvolution()).isEqualTo(Evolution.STABLE);
        Assertions.assertThat(persistedTreatment.getNotes()).isEqualTo("Recheck in one week");
    }

    @Test
    public void treatmentRepository_save_NullPet_ThrowsDataIntegrityViolation() {
        // Arrange
        Treatment treatment = RepositoryTestData.treatment(pet1, vet1, BASE_DATE.plusDays(10), Evolution.STABLE);
        treatment.setPet(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentRepository.saveAndFlush(treatment));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentRepository_save_NullDiagnosis_ThrowsDataIntegrityViolation() {
        // Arrange
        Treatment treatment = RepositoryTestData.treatment(pet1, vet1, BASE_DATE.plusDays(10), Evolution.STABLE);
        treatment.setDiagnosis(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentRepository.saveAndFlush(treatment));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentRepository_save_NullDate_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna treatment_date es obligatoria en la base de datos.
        Treatment treatment = RepositoryTestData.treatment(pet1, vet1, BASE_DATE.plusDays(10), Evolution.STABLE);
        treatment.setDate(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentRepository.saveAndFlush(treatment));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentRepository_save_NullWeight_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna weight es obligatoria en la base de datos.
        Treatment treatment = RepositoryTestData.treatment(pet1, vet1, BASE_DATE.plusDays(10), Evolution.STABLE);
        treatment.setWeight(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> treatmentRepository.saveAndFlush(treatment));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void treatmentRepository_findAll_ExistingTreatments_ReturnsAllFixtureTreatments() {
        // Arrange
        // setUp creó exactamente cuatro tratamientos: t1, t2, t3 y t4.

        // Act
        List<Treatment> treatments = treatmentRepository.findAll();

        // Assert
        Assertions.assertThat(treatments)
                .extracting(Treatment::getId)
                .containsExactlyInAnyOrder(t1.getId(), t2.getId(), t3.getId(), t4.getId());
    }

    @Test
    public void treatmentRepository_findAll_NoTreatments_ReturnsEmptyList() {
        // Arrange
        treatmentRepository.deleteAll();
        treatmentRepository.flush();
        entityManager.clear();

        // Act
        List<Treatment> treatments = treatmentRepository.findAll();

        // Assert
        Assertions.assertThat(treatments).isEmpty();
    }

    @Test
    public void treatmentRepository_findById_ExistingId_ReturnsTreatment() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Treatment> result = treatmentRepository.findById(t1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Treatment foundTreatment = result.orElseThrow();
        Assertions.assertThat(foundTreatment.getPet().getId()).isEqualTo(pet1.getId());
        Assertions.assertThat(foundTreatment.getVeterinarian().getId()).isEqualTo(vet1.getId());
        Assertions.assertThat(foundTreatment.getDate()).isEqualTo(BASE_DATE);
        Assertions.assertThat(foundTreatment.getEvolution()).isNull();
    }

    @Test
    public void treatmentRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<Treatment> result = treatmentRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void treatmentRepository_deleteById_ExistingId_RemovesOnlyThatTreatment() {
        // Arrange
        Long deletedId = t1.getId();
        long countBeforeDeletion = treatmentRepository.count();

        // Act
        treatmentRepository.deleteById(deletedId);
        treatmentRepository.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(treatmentRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(treatmentRepository.count()).isEqualTo(countBeforeDeletion - 1);
        Assertions.assertThat(treatmentRepository.findById(t2.getId())).isPresent();
        Assertions.assertThat(treatmentRepository.findById(t3.getId())).isPresent();
        Assertions.assertThat(treatmentRepository.findById(t4.getId())).isPresent();
        Assertions.assertThat(petRepository.findById(pet1.getId())).isPresent();
        Assertions.assertThat(veterinarianRepository.findById(vet1.getId())).isPresent();
    }

    @Test
    public void treatmentRepository_findByPetId_PetWithTreatments_ReturnsOnlyThatPetTreatments() {
        // Arrange
        // pet1 posee t1 y t2.

        // Act
        List<Treatment> treatments = treatmentRepository.findByPetId(pet1.getId());

        // Assert
        Assertions.assertThat(treatments)
                .extracting(Treatment::getId)
                .containsExactlyInAnyOrder(t1.getId(), t2.getId());
    }

    @Test
    public void treatmentRepository_findByPetId_PetWithoutTreatments_ReturnsEmptyList() {
        // Arrange
        // pet3 pertenece a owner2 y no tiene tratamientos.

        // Act
        List<Treatment> treatments = treatmentRepository.findByPetId(pet3.getId());

        // Assert
        Assertions.assertThat(treatments).isEmpty();
    }

    @Test
    public void treatmentRepository_findByPetId_NonExistingPetId_ReturnsEmptyList() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id de mascota generado.

        // Act
        List<Treatment> treatments = treatmentRepository.findByPetId(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(treatments).isEmpty();
    }

    @Test
    public void treatmentRepository_findByVeterinarianId_VeterinarianWithTreatments_ReturnsOnlyThatVeterinarianTreatments() {
        // Arrange
        // vet1 atendió t1 y t2; vet2 atendió t3 y t4.

        // Act
        List<Treatment> vet1Treatments = treatmentRepository.findByVeterinarianId(vet1.getId());
        List<Treatment> vet2Treatments = treatmentRepository.findByVeterinarianId(vet2.getId());

        // Assert
        Assertions.assertThat(vet1Treatments)
                .extracting(Treatment::getId)
                .containsExactlyInAnyOrder(t1.getId(), t2.getId());
        Assertions.assertThat(vet2Treatments)
                .extracting(Treatment::getId)
                .containsExactlyInAnyOrder(t3.getId(), t4.getId());
    }

    @Test
    public void treatmentRepository_findByVeterinarianId_VeterinarianWithoutTreatments_ReturnsEmptyList() {
        // Arrange
        Veterinarian veterinarianWithoutTreatments = saveVeterinarian();

        // Act
        List<Treatment> treatments = treatmentRepository.findByVeterinarianId(veterinarianWithoutTreatments.getId());

        // Assert
        Assertions.assertThat(treatments).isEmpty();
    }

    @Test
    public void treatmentRepository_findByVeterinarianId_NonExistingVeterinarianId_ReturnsEmptyList() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id de veterinario generado.

        // Act
        List<Treatment> treatments = treatmentRepository.findByVeterinarianId(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(treatments).isEmpty();
    }

    @Test
    public void treatmentRepository_existsByPetOwnerId_OwnerWithTreatedPet_ReturnsTrue() {
        // Arrange
        // pet1 y pet2, ambas propiedad de owner1, tienen tratamientos.

        // Act
        boolean exists = treatmentRepository.existsByPetOwnerId(owner1.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void treatmentRepository_existsByPetOwnerId_OnlyOnePetOfSeveralTreated_ReturnsTrue() {
        // Arrange
        Owner owner = saveOwner();
        Pet treatedPet = savePet(owner);
        savePet(owner); // Segunda mascota del mismo dueño, intencionalmente sin tratamientos.
        saveTreatment(treatedPet, vet1, BASE_DATE, null);

        // Act
        boolean exists = treatmentRepository.existsByPetOwnerId(owner.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void treatmentRepository_existsByPetOwnerId_OwnerWithPetsWithoutTreatments_ReturnsFalse() {
        // Arrange
        // owner2 posee a pet3, que no tiene tratamientos.

        // Act
        boolean exists = treatmentRepository.existsByPetOwnerId(owner2.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void treatmentRepository_existsByPetOwnerId_OwnerWithoutPets_ReturnsFalse() {
        // Arrange
        // owner3 no tiene mascotas.

        // Act
        boolean exists = treatmentRepository.existsByPetOwnerId(owner3.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void treatmentRepository_existsByPetOwnerId_NonExistingOwnerId_ReturnsFalse() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id de dueño generado.

        // Act
        boolean exists = treatmentRepository.existsByPetOwnerId(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void treatmentRepository_countAdministeredTreatmentsBetween_RangeCoveringAllExcludingRecovered_CountsAdmissionAndFollowUps() {
        // Arrange
        // Los cuatro tratamientos de las fixtures abarcan desde BASE_DATE hasta BASE_DATE más seis días.

        // Act
        long count = treatmentRepository.countAdministeredTreatmentsBetween(
                BASE_DATE,
                BASE_DATE.plusDays(30),
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(count).isEqualTo(3L);
    }

    @Test
    public void treatmentRepository_countAdministeredTreatmentsBetween_TreatmentExactlyAtStart_IncludesIt() {
        // Arrange
        // t2 tiene fecha exactamente en el inicio del rango.

        // Act
        long count = treatmentRepository.countAdministeredTreatmentsBetween(
                t2.getDate(),
                BASE_DATE.plusDays(30),
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    public void treatmentRepository_countAdministeredTreatmentsBetween_TreatmentExactlyAtEnd_ExcludesIt() {
        // Arrange
        // t3 tiene fecha exactamente en el fin del rango.

        // Act
        long count = treatmentRepository.countAdministeredTreatmentsBetween(
                BASE_DATE,
                t3.getDate(),
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    public void treatmentRepository_countAdministeredTreatmentsBetween_RangeWithoutTreatments_ReturnsZero() {
        // Arrange
        // Ningún tratamiento de las fixtures tiene fecha anterior a BASE_DATE.

        // Act
        long count = treatmentRepository.countAdministeredTreatmentsBetween(
                BASE_DATE.minusDays(30),
                BASE_DATE.minusDays(1),
                Evolution.RECOVERED
        );

        // Assert
        Assertions.assertThat(count).isZero();
    }

    @Test
    public void treatmentRepository_countAdministeredTreatmentsBetween_ExcludedEvolutionIsWorsening_ExcludesOnlyThatEvolution() {
        // Arrange
        // t3 es el único tratamiento WORSENING.

        // Act
        long count = treatmentRepository.countAdministeredTreatmentsBetween(
                BASE_DATE,
                BASE_DATE.plusDays(30),
                Evolution.WORSENING
        );

        // Assert
        Assertions.assertThat(count).isEqualTo(3L);
    }

    @Test
    public void treatmentRepository_countAdministeredTreatmentsBetween_NullExcludedEvolution_CountsOnlyAdmissions() {
        // Arrange
        // t1 es el único tratamiento cuya evolución es null.

        // Act
        long count = treatmentRepository.countAdministeredTreatmentsBetween(
                BASE_DATE,
                BASE_DATE.plusDays(30),
                null
        );

        // Assert
        // Fija la lógica trivaluada de SQL de la consulta actual: comparar una evolución
        // contra un parámetro NULL produce UNKNOWN, por lo que solo cuentan las filas con evolution IS NULL.
        Assertions.assertThat(count).isEqualTo(1L);
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
}
