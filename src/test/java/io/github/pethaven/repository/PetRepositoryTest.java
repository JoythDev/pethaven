package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
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
public class PetRepositoryTest {

    private static final LocalDateTime BASE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final TreatmentRepository treatmentRepository;
    private final EntityManager entityManager;

    private Owner owner1;
    private Owner owner2;
    private Owner owner3;
    private Pet pet1;
    private Pet pet2;
    private Pet pet3;
    private Pet pet4;
    private Veterinarian vet1;
    private Treatment t1;
    private Treatment t2;

    @Autowired
    public PetRepositoryTest(
            PetRepository petRepository,
            OwnerRepository ownerRepository,
            VeterinarianRepository veterinarianRepository,
            TreatmentRepository treatmentRepository,
            EntityManager entityManager
    ) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.treatmentRepository = treatmentRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        owner1 = saveOwner();
        owner2 = saveOwner();
        // owner3 no tiene mascotas.
        owner3 = saveOwner();

        // Los nombres se fijan antes de guardar para que las búsquedas derivadas dependan de las fixtures.
        pet1 = savePet(owner1, "Luna", true);
        pet2 = savePet(owner1, "Max", true);
        // pet3 es la única mascota inactiva y su nombre comparte la raíz "luna" con pet1 y pet4.
        pet3 = savePet(owner2, "Moonluna", false);
        pet4 = savePet(owner2, "LUNARES", true);

        vet1 = saveVeterinarian();
        // Los tratamientos solo existen para cubrir la cascada física al borrar pet1.
        t1 = saveTreatment(pet1, vet1, BASE_DATE);
        t2 = saveTreatment(pet1, vet1, BASE_DATE.plusDays(2));

        entityManager.flush();
    }

    @Test
    public void petRepository_save_ValidPet_ReturnsPersistedPetWithGeneratedId() {
        // Arrange
        // El builder aplica los valores por defecto de active y treatments.
        Pet pet = RepositoryTestData.pet(owner1);
        String name = pet.getName();
        Species species = pet.getSpecies();
        String breed = pet.getBreed();
        Integer age = pet.getAge();
        Double weight = pet.getWeight();

        // Act
        Pet savedPet = petRepository.save(pet);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedPet.getId()).isNotNull();
        Pet persistedPet = petRepository.findById(savedPet.getId()).orElseThrow();
        Assertions.assertThat(persistedPet.getName()).isEqualTo(name);
        Assertions.assertThat(persistedPet.getSpecies()).isEqualTo(species);
        Assertions.assertThat(persistedPet.getBreed()).isEqualTo(breed);
        Assertions.assertThat(persistedPet.getAge()).isEqualTo(age);
        Assertions.assertThat(persistedPet.getWeight()).isEqualTo(weight);
        Assertions.assertThat(persistedPet.getOwner().getId()).isEqualTo(owner1.getId());
        Assertions.assertThat(persistedPet.isActive()).isTrue();
        Assertions.assertThat(persistedPet.getDisease()).isNull();
        Assertions.assertThat(persistedPet.getPhotoUrl()).isNull();
        Assertions.assertThat(persistedPet.getTreatments()).isEmpty();
    }

    @Test
    public void petRepository_save_ExistingPet_UpdatesPersistedValues() {
        // Arrange
        entityManager.flush();
        entityManager.clear();
        Pet petToUpdate = petRepository.findById(pet1.getId()).orElseThrow();
        petToUpdate.setName("Updated Pet");
        petToUpdate.setBreed("Beagle");
        petToUpdate.setAge(5);
        petToUpdate.setWeight(12.25);
        petToUpdate.setDisease("Otitis externa");
        petToUpdate.setPhotoUrl("https://pethaven.test/pet.png");
        petToUpdate.setSpecies(Species.CAT);
        petToUpdate.setActive(false);

        // Act
        petRepository.save(petToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Pet persistedPet = petRepository.findById(pet1.getId()).orElseThrow();
        Assertions.assertThat(persistedPet.getId()).isEqualTo(pet1.getId());
        Assertions.assertThat(persistedPet.getName()).isEqualTo("Updated Pet");
        Assertions.assertThat(persistedPet.getBreed()).isEqualTo("Beagle");
        Assertions.assertThat(persistedPet.getAge()).isEqualTo(5);
        Assertions.assertThat(persistedPet.getWeight()).isEqualTo(12.25);
        Assertions.assertThat(persistedPet.getDisease()).isEqualTo("Otitis externa");
        Assertions.assertThat(persistedPet.getPhotoUrl()).isEqualTo("https://pethaven.test/pet.png");
        Assertions.assertThat(persistedPet.getSpecies()).isEqualTo(Species.CAT);
        Assertions.assertThat(persistedPet.isActive()).isFalse();
        Assertions.assertThat(persistedPet.getOwner().getId()).isEqualTo(owner1.getId());
        Assertions.assertThat(petRepository.count()).isEqualTo(4L);
    }

    @Test
    public void petRepository_save_NullName_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna name es obligatoria en la base de datos.
        Pet petWithNullName = RepositoryTestData.pet(owner1);
        petWithNullName.setName(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> petRepository.saveAndFlush(petWithNullName));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void petRepository_save_NullSpecies_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna species es obligatoria en la base de datos.
        Pet petWithNullSpecies = RepositoryTestData.pet(owner1);
        petWithNullSpecies.setSpecies(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> petRepository.saveAndFlush(petWithNullSpecies));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void petRepository_save_NullBreed_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna breed es obligatoria en la base de datos.
        Pet petWithNullBreed = RepositoryTestData.pet(owner1);
        petWithNullBreed.setBreed(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> petRepository.saveAndFlush(petWithNullBreed));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void petRepository_save_NullAge_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna age es obligatoria en la base de datos.
        Pet petWithNullAge = RepositoryTestData.pet(owner1);
        petWithNullAge.setAge(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> petRepository.saveAndFlush(petWithNullAge));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void petRepository_save_NullWeight_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna weight es obligatoria en la base de datos.
        Pet petWithNullWeight = RepositoryTestData.pet(owner1);
        petWithNullWeight.setWeight(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> petRepository.saveAndFlush(petWithNullWeight));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void petRepository_save_NullOwner_ThrowsDataIntegrityViolation() {
        // Arrange
        // La relación con Owner es obligatoria en la base de datos.
        Pet petWithNullOwner = RepositoryTestData.pet(owner1);
        petWithNullOwner.setOwner(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> petRepository.saveAndFlush(petWithNullOwner));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void petRepository_findById_ExistingId_ReturnsPet() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Pet> result = petRepository.findById(pet1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Pet foundPet = result.orElseThrow();
        Assertions.assertThat(foundPet.getId()).isEqualTo(pet1.getId());
        Assertions.assertThat(foundPet.getName()).isEqualTo("Luna");
        Assertions.assertThat(foundPet.getSpecies()).isEqualTo(Species.DOG);
        Assertions.assertThat(foundPet.getBreed()).isEqualTo(pet1.getBreed());
        Assertions.assertThat(foundPet.getAge()).isEqualTo(pet1.getAge());
        Assertions.assertThat(foundPet.getWeight()).isEqualTo(pet1.getWeight());
        Assertions.assertThat(foundPet.getOwner().getId()).isEqualTo(owner1.getId());
    }

    @Test
    public void petRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<Pet> result = petRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void petRepository_findAll_ExistingPets_ReturnsAllFixturePets() {
        // Arrange
        // setUp creó exactamente cuatro mascotas: pet1, pet2, pet3 y pet4.

        // Act
        List<Pet> pets = petRepository.findAll();

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactlyInAnyOrder(pet1.getId(), pet2.getId(), pet3.getId(), pet4.getId());
    }

    @Test
    public void petRepository_deleteById_PetWithoutTreatments_RemovesOnlyThatPet() {
        // Arrange
        // pet2 no tiene tratamientos, así que el borrado no depende de ninguna cascada.
        Long deletedId = pet2.getId();
        long countBeforeDeletion = petRepository.count();

        // Act
        petRepository.deleteById(deletedId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(petRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(petRepository.count()).isEqualTo(countBeforeDeletion - 1);
        Assertions.assertThat(petRepository.findById(pet1.getId())).isPresent();
        Assertions.assertThat(petRepository.findById(pet3.getId())).isPresent();
        Assertions.assertThat(petRepository.findById(pet4.getId())).isPresent();
    }

    @Test
    public void petRepository_deleteById_PetWithTreatments_CascadesToTreatmentsAtDatabaseLevel() {
        // Arrange
        // La cascada es física, definida por @OnDelete en Pet.treatments: no existe cascada JPA.
        // Se limpia el contexto de persistencia para que los tratamientos no queden gestionados
        // apuntando a una mascota eliminada, igual que en una transacción nueva del servicio.
        entityManager.flush();
        entityManager.clear();
        Long deletedPetId = pet1.getId();

        // Act
        petRepository.deleteById(deletedPetId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(petRepository.findById(deletedPetId)).isEmpty();
        Assertions.assertThat(treatmentRepository.findById(t1.getId())).isEmpty();
        Assertions.assertThat(treatmentRepository.findById(t2.getId())).isEmpty();
        Assertions.assertThat(veterinarianRepository.findById(vet1.getId())).isPresent();
        Assertions.assertThat(ownerRepository.findById(owner1.getId())).isPresent();
    }

    @Test
    public void petRepository_findByOwnerId_OwnerWithPets_ReturnsOnlyThatOwnersPets() {
        // Arrange
        // owner1 posee pet1 y pet2.

        // Act
        List<Pet> pets = petRepository.findByOwnerId(owner1.getId());

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactlyInAnyOrder(pet1.getId(), pet2.getId());
    }

    @Test
    public void petRepository_findByOwnerId_AnotherOwnerWithPets_ReturnsOnlyThatOwnersPets() {
        // Arrange
        // owner2 posee pet3 y pet4.

        // Act
        List<Pet> pets = petRepository.findByOwnerId(owner2.getId());

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactlyInAnyOrder(pet3.getId(), pet4.getId());
    }

    @Test
    public void petRepository_findByOwnerId_OwnerWithoutPets_ReturnsEmptyList() {
        // Arrange
        // owner3 no tiene mascotas.

        // Act
        List<Pet> pets = petRepository.findByOwnerId(owner3.getId());

        // Assert
        Assertions.assertThat(pets).isEmpty();
    }

    @Test
    public void petRepository_findByOwnerId_NonExistingOwnerId_ReturnsEmptyList() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id de dueño generado.

        // Act
        List<Pet> pets = petRepository.findByOwnerId(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(pets).isEmpty();
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_FullName_ReturnsAllPetsContainingThatText() {
        // Arrange
        // "Luna" también está contenido en "Moonluna" y en "LUNARES".

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("Luna");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactlyInAnyOrder(pet1.getId(), pet3.getId(), pet4.getId());
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_LowercaseText_ReturnsSamePetsAsOriginalCase() {
        // Arrange
        // "luna" debe encontrar los mismos nombres que "Luna", sin distinguir mayúsculas.

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("luna");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactlyInAnyOrder(pet1.getId(), pet3.getId(), pet4.getId());
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_UppercaseText_ReturnsSamePetsAsOriginalCase() {
        // Arrange
        // "LUN" debe encontrar los mismos nombres que "Luna", sin distinguir mayúsculas.

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("LUN");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactlyInAnyOrder(pet1.getId(), pet3.getId(), pet4.getId());
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_MiddleSubstring_ReturnsMatchingPet() {
        // Arrange
        // "oon" solo aparece en el interior de "Moonluna".

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("oon");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactly(pet3.getId());
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_LowercaseSuffixOfUppercaseName_ReturnsMatchingPet() {
        // Arrange
        // "ares" es el sufijo en minúsculas del nombre en mayúsculas "LUNARES".

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("ares");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactly(pet4.getId());
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_UniqueName_ReturnsSinglePet() {
        // Arrange
        // "Max" solo coincide con pet2.

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("Max");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactly(pet2.getId());
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_UnknownText_ReturnsEmptyList() {
        // Arrange
        // "Zzz" no está contenido en ningún nombre de las fixtures.

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("Zzz");

        // Assert
        Assertions.assertThat(pets).isEmpty();
    }

    @Test
    public void petRepository_findByNameContainingIgnoreCase_InactivePetName_ReturnsInactivePet() {
        // Arrange
        // pet3 es inactiva y la búsqueda no filtra por estado.

        // Act
        List<Pet> pets = petRepository.findByNameContainingIgnoreCase("Moonluna");

        // Assert
        Assertions.assertThat(pets)
                .extracting(Pet::getId)
                .containsExactly(pet3.getId());
        Assertions.assertThat(pets.get(0).isActive()).isFalse();
    }

    @Test
    public void petRepository_countByActiveTrue_FixtureWithThreeActivePets_ReturnsThree() {
        // Arrange
        // pet1, pet2 y pet4 son activas; pet3 es la única inactiva.

        // Act
        long count = petRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(3L);
    }

    @Test
    public void petRepository_countByActiveTrue_AfterDeactivatingActivePet_ReturnsTwo() {
        // Arrange
        // pet1 pasa de activa a inactiva, así que solo quedan pet2 y pet4 activas.
        pet1.setActive(false);
        petRepository.saveAndFlush(pet1);

        // Act
        long count = petRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    public void petRepository_countByActiveTrue_AfterReactivatingInactivePet_ReturnsFour() {
        // Arrange
        // pet3 pasa de inactiva a activa y se suma a pet1, pet2 y pet4.
        pet3.setActive(true);
        petRepository.saveAndFlush(pet3);

        // Act
        long count = petRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(4L);
    }

    @Test
    public void petRepository_countByActiveTrue_AfterDeletingInactivePet_ReturnsThree() {
        // Arrange
        // Borrar a pet3, la única inactiva, no cambia el total de mascotas activas.
        entityManager.flush();
        entityManager.clear();
        petRepository.deleteById(pet3.getId());
        entityManager.flush();
        entityManager.clear();

        // Act
        long count = petRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(3L);
    }

    private Owner saveOwner() {
        return ownerRepository.save(RepositoryTestData.owner());
    }

    private Pet savePet(Owner owner, String name, boolean active) {
        Pet pet = RepositoryTestData.pet(owner);
        pet.setName(name);
        pet.setActive(active);
        return petRepository.save(pet);
    }

    private Veterinarian saveVeterinarian() {
        return veterinarianRepository.save(RepositoryTestData.veterinarian());
    }

    private Treatment saveTreatment(Pet pet, Veterinarian veterinarian, LocalDateTime date) {
        return treatmentRepository.save(RepositoryTestData.treatment(pet, veterinarian, date, null));
    }
}
