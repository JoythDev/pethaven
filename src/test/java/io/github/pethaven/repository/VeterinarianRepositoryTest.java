package io.github.pethaven.repository;

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
public class VeterinarianRepositoryTest {

    private static final LocalDateTime BASE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final VeterinarianRepository veterinarianRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final TreatmentRepository treatmentRepository;
    private final EntityManager entityManager;

    private Owner owner1;
    private Pet pet1;
    private Veterinarian vet1;
    private Veterinarian vet2;
    private Veterinarian vet3;
    private Treatment t1;
    private Treatment t2;
    private Treatment t3;

    @Autowired
    public VeterinarianRepositoryTest(
            VeterinarianRepository veterinarianRepository,
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            TreatmentRepository treatmentRepository,
            EntityManager entityManager
    ) {
        this.veterinarianRepository = veterinarianRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.treatmentRepository = treatmentRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        owner1 = saveOwner();
        pet1 = savePet(owner1);

        vet1 = saveVeterinarian();
        vet2 = saveVeterinarian();

        // vet3 se marca inactivo antes de guardarse para cubrir las consultas countByActiveFalse.
        Veterinarian inactiveVeterinarian = RepositoryTestData.veterinarian();
        inactiveVeterinarian.setActive(false);
        vet3 = veterinarianRepository.save(inactiveVeterinarian);

        t1 = saveTreatment(pet1, vet1, BASE_DATE);
        t2 = saveTreatment(pet1, vet1, BASE_DATE.plusDays(2));
        t3 = saveTreatment(pet1, vet2, BASE_DATE.plusDays(4));

        entityManager.flush();
    }

    @Test
    public void veterinarianRepository_save_ValidVeterinarian_ReturnsPersistedVeterinarianWithGeneratedId() {
        // Arrange
        // El builder aplica los valores por defecto de attentions, active y photoUrl.
        Veterinarian veterinarian = RepositoryTestData.veterinarian();
        String document = veterinarian.getDocument();
        String name = veterinarian.getName();
        String email = veterinarian.getEmail();
        String password = veterinarian.getPassword();
        String specialty = veterinarian.getSpecialty();

        // Act
        Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedVeterinarian.getId()).isNotNull();
        Veterinarian persistedVeterinarian = veterinarianRepository.findById(savedVeterinarian.getId()).orElseThrow();
        Assertions.assertThat(persistedVeterinarian.getDocument()).isEqualTo(document);
        Assertions.assertThat(persistedVeterinarian.getName()).isEqualTo(name);
        Assertions.assertThat(persistedVeterinarian.getEmail()).isEqualTo(email);
        Assertions.assertThat(persistedVeterinarian.getPassword()).isEqualTo(password);
        Assertions.assertThat(persistedVeterinarian.getSpecialty()).isEqualTo(specialty);
        Assertions.assertThat(persistedVeterinarian.getPhotoUrl()).isNull();
        Assertions.assertThat(persistedVeterinarian.getAttentions()).isZero();
        Assertions.assertThat(persistedVeterinarian.isActive()).isTrue();
    }

    @Test
    public void veterinarianRepository_save_ExistingVeterinarian_UpdatesPersistedValues() {
        // Arrange
        entityManager.flush();
        entityManager.clear();
        Veterinarian veterinarianToUpdate = veterinarianRepository.findById(vet1.getId()).orElseThrow();
        veterinarianToUpdate.setName("Updated Veterinarian");
        veterinarianToUpdate.setSpecialty("Dermatology");
        veterinarianToUpdate.setEmail("updated.vet@pethaven.test");
        veterinarianToUpdate.setPhotoUrl("https://pethaven.test/vet.png");
        veterinarianToUpdate.setAttentions(42);
        veterinarianToUpdate.setActive(false);

        // Act
        veterinarianRepository.save(veterinarianToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Veterinarian persistedVeterinarian = veterinarianRepository.findById(vet1.getId()).orElseThrow();
        Assertions.assertThat(persistedVeterinarian.getId()).isEqualTo(vet1.getId());
        Assertions.assertThat(persistedVeterinarian.getName()).isEqualTo("Updated Veterinarian");
        Assertions.assertThat(persistedVeterinarian.getSpecialty()).isEqualTo("Dermatology");
        Assertions.assertThat(persistedVeterinarian.getEmail()).isEqualTo("updated.vet@pethaven.test");
        Assertions.assertThat(persistedVeterinarian.getPhotoUrl()).isEqualTo("https://pethaven.test/vet.png");
        Assertions.assertThat(persistedVeterinarian.getAttentions()).isEqualTo(42);
        Assertions.assertThat(persistedVeterinarian.isActive()).isFalse();
        Assertions.assertThat(veterinarianRepository.count()).isEqualTo(3L);
    }

    @Test
    public void veterinarianRepository_save_DuplicateDocument_ThrowsDataIntegrityViolation() {
        // Arrange
        // vet1 ya ocupa su documento, protegido por la restricción única de la columna document.
        Veterinarian duplicateVeterinarian = RepositoryTestData.veterinarian();
        duplicateVeterinarian.setDocument(vet1.getDocument());

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(duplicateVeterinarian));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_save_DuplicateEmail_ThrowsDataIntegrityViolation() {
        // Arrange
        // vet1 ya ocupa su correo, protegido por la restricción única de la columna email.
        Veterinarian duplicateVeterinarian = RepositoryTestData.veterinarian();
        duplicateVeterinarian.setEmail(vet1.getEmail());

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(duplicateVeterinarian));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_save_NullName_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna name es obligatoria en la base de datos.
        Veterinarian veterinarianWithNullName = RepositoryTestData.veterinarian();
        veterinarianWithNullName.setName(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(veterinarianWithNullName));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_save_NullSpecialty_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna specialty es obligatoria en la base de datos.
        Veterinarian veterinarianWithNullSpecialty = RepositoryTestData.veterinarian();
        veterinarianWithNullSpecialty.setSpecialty(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(veterinarianWithNullSpecialty));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_save_NullDocument_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna document es obligatoria en la base de datos.
        Veterinarian veterinarianWithNullDocument = RepositoryTestData.veterinarian();
        veterinarianWithNullDocument.setDocument(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(veterinarianWithNullDocument));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_save_NullEmail_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna email es obligatoria en la base de datos.
        Veterinarian veterinarianWithNullEmail = RepositoryTestData.veterinarian();
        veterinarianWithNullEmail.setEmail(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(veterinarianWithNullEmail));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_save_NullPassword_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna password es obligatoria en la base de datos.
        Veterinarian veterinarianWithNullPassword = RepositoryTestData.veterinarian();
        veterinarianWithNullPassword.setPassword(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> veterinarianRepository.saveAndFlush(veterinarianWithNullPassword));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void veterinarianRepository_findById_ExistingId_ReturnsVeterinarian() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findById(vet1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Veterinarian foundVeterinarian = result.orElseThrow();
        Assertions.assertThat(foundVeterinarian.getId()).isEqualTo(vet1.getId());
        Assertions.assertThat(foundVeterinarian.getDocument()).isEqualTo(vet1.getDocument());
        Assertions.assertThat(foundVeterinarian.getName()).isEqualTo(vet1.getName());
        Assertions.assertThat(foundVeterinarian.getEmail()).isEqualTo(vet1.getEmail());
        Assertions.assertThat(foundVeterinarian.getSpecialty()).isEqualTo(vet1.getSpecialty());
    }

    @Test
    public void veterinarianRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void veterinarianRepository_findAll_ExistingVeterinarians_ReturnsAllFixtureVeterinarians() {
        // Arrange
        // setUp creó exactamente tres veterinarios: vet1, vet2 y vet3.

        // Act
        List<Veterinarian> veterinarians = veterinarianRepository.findAll();

        // Assert
        Assertions.assertThat(veterinarians)
                .extracting(Veterinarian::getId)
                .containsExactlyInAnyOrder(vet1.getId(), vet2.getId(), vet3.getId());
    }

    @Test
    public void veterinarianRepository_deleteById_VeterinarianWithoutTreatments_RemovesOnlyThatVeterinarian() {
        // Arrange
        // vet3 no tiene tratamientos, así que el borrado no depende de ninguna cascada.
        Long deletedId = vet3.getId();
        long countBeforeDeletion = veterinarianRepository.count();

        // Act
        veterinarianRepository.deleteById(deletedId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(veterinarianRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(veterinarianRepository.count()).isEqualTo(countBeforeDeletion - 1);
        Assertions.assertThat(veterinarianRepository.findById(vet1.getId())).isPresent();
        Assertions.assertThat(veterinarianRepository.findById(vet2.getId())).isPresent();
    }

    @Test
    public void veterinarianRepository_deleteById_VeterinarianWithTreatments_CascadesToTreatmentsAtDatabaseLevel() {
        // Arrange
        // La cascada es física, definida por @OnDelete en Veterinarian.treatments: no existe cascada JPA.
        // Se limpia el contexto de persistencia para que los tratamientos no queden gestionados
        // apuntando a un veterinario eliminado, igual que en una transacción nueva del servicio.
        entityManager.flush();
        entityManager.clear();
        Long deletedVeterinarianId = vet1.getId();

        // Act
        veterinarianRepository.deleteById(deletedVeterinarianId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(veterinarianRepository.findById(deletedVeterinarianId)).isEmpty();
        Assertions.assertThat(treatmentRepository.findById(t1.getId())).isEmpty();
        Assertions.assertThat(treatmentRepository.findById(t2.getId())).isEmpty();
        Assertions.assertThat(treatmentRepository.findById(t3.getId())).isPresent();
    }

    @Test
    public void veterinarianRepository_findByDocument_ExistingDocument_ReturnsVeterinarian() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findByDocument(vet1.getDocument());

        // Assert
        Assertions.assertThat(result).isPresent();
        Veterinarian foundVeterinarian = result.orElseThrow();
        Assertions.assertThat(foundVeterinarian.getId()).isEqualTo(vet1.getId());
        Assertions.assertThat(foundVeterinarian.getDocument()).isEqualTo(vet1.getDocument());
        Assertions.assertThat(foundVeterinarian.getEmail()).isEqualTo(vet1.getEmail());
    }

    @Test
    public void veterinarianRepository_findByDocument_UnknownDocument_ReturnsEmptyOptional() {
        // Arrange
        // VET-UNKNOWN no existe en las fixtures.

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findByDocument("VET-UNKNOWN");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void veterinarianRepository_findByDocument_EmailOfExistingVeterinarian_ReturnsEmptyOptional() {
        // Arrange
        // El correo de vet1 existe, pero no es el documento de ningún veterinario.

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findByDocument(vet1.getEmail());

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void veterinarianRepository_findByEmail_ExistingEmail_ReturnsVeterinarian() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findByEmail(vet1.getEmail());

        // Assert
        Assertions.assertThat(result).isPresent();
        Veterinarian foundVeterinarian = result.orElseThrow();
        Assertions.assertThat(foundVeterinarian.getId()).isEqualTo(vet1.getId());
        Assertions.assertThat(foundVeterinarian.getEmail()).isEqualTo(vet1.getEmail());
        Assertions.assertThat(foundVeterinarian.getDocument()).isEqualTo(vet1.getDocument());
    }

    @Test
    public void veterinarianRepository_findByEmail_UnknownEmail_ReturnsEmptyOptional() {
        // Arrange
        // unknown.vet@pethaven.test no existe en las fixtures.

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findByEmail("unknown.vet@pethaven.test");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void veterinarianRepository_findByEmail_DocumentOfExistingVeterinarian_ReturnsEmptyOptional() {
        // Arrange
        // El documento de vet1 existe, pero no es el correo de ningún veterinario.

        // Act
        Optional<Veterinarian> result = veterinarianRepository.findByEmail(vet1.getDocument());

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void veterinarianRepository_existsByDocument_ExistingDocument_ReturnsTrue() {
        // Arrange
        // vet1 ocupa su propio documento.

        // Act
        boolean exists = veterinarianRepository.existsByDocument(vet1.getDocument());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void veterinarianRepository_existsByDocument_UnknownDocument_ReturnsFalse() {
        // Arrange
        // VET-UNKNOWN no existe en las fixtures.

        // Act
        boolean exists = veterinarianRepository.existsByDocument("VET-UNKNOWN");

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByDocument_EmailOfExistingVeterinarian_ReturnsFalse() {
        // Arrange
        // El correo de vet1 existe, pero no como documento de ningún veterinario.

        // Act
        boolean exists = veterinarianRepository.existsByDocument(vet1.getEmail());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByEmail_ExistingEmail_ReturnsTrue() {
        // Arrange
        // vet1 ocupa su propio correo.

        // Act
        boolean exists = veterinarianRepository.existsByEmail(vet1.getEmail());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void veterinarianRepository_existsByEmail_UnknownEmail_ReturnsFalse() {
        // Arrange
        // unknown.vet@pethaven.test no existe en las fixtures.

        // Act
        boolean exists = veterinarianRepository.existsByEmail("unknown.vet@pethaven.test");

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByEmail_DocumentOfExistingVeterinarian_ReturnsFalse() {
        // Arrange
        // El documento de vet1 existe, pero no como correo de ningún veterinario.

        // Act
        boolean exists = veterinarianRepository.existsByEmail(vet1.getDocument());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByDocumentAndIdNot_SameDocumentWithOwnId_ReturnsFalse() {
        // Arrange
        // El documento pertenece a vet1 y se excluye su propio id, así que ningún otro veterinario lo tiene.

        // Act
        boolean exists = veterinarianRepository.existsByDocumentAndIdNot(vet1.getDocument(), vet1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByDocumentAndIdNot_SameDocumentWithAnotherVeterinarianId_ReturnsTrue() {
        // Arrange
        // El documento pertenece a vet1 y se consulta excluyendo el id de vet2.

        // Act
        boolean exists = veterinarianRepository.existsByDocumentAndIdNot(vet1.getDocument(), vet2.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void veterinarianRepository_existsByDocumentAndIdNot_UnknownDocument_ReturnsFalse() {
        // Arrange
        // VET-UNKNOWN no existe en las fixtures, sea cual sea el id excluido.

        // Act
        boolean exists = veterinarianRepository.existsByDocumentAndIdNot("VET-UNKNOWN", vet1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByEmailAndIdNot_SameEmailWithOwnId_ReturnsFalse() {
        // Arrange
        // El correo pertenece a vet1 y se excluye su propio id, así que ningún otro veterinario lo tiene.

        // Act
        boolean exists = veterinarianRepository.existsByEmailAndIdNot(vet1.getEmail(), vet1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_existsByEmailAndIdNot_SameEmailWithAnotherVeterinarianId_ReturnsTrue() {
        // Arrange
        // El correo pertenece a vet1 y se consulta excluyendo el id de vet2.

        // Act
        boolean exists = veterinarianRepository.existsByEmailAndIdNot(vet1.getEmail(), vet2.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void veterinarianRepository_existsByEmailAndIdNot_UnknownEmail_ReturnsFalse() {
        // Arrange
        // unknown.vet@pethaven.test no existe en las fixtures, sea cual sea el id excluido.

        // Act
        boolean exists = veterinarianRepository.existsByEmailAndIdNot("unknown.vet@pethaven.test", vet1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void veterinarianRepository_countByActiveTrue_FixtureWithTwoActiveVeterinarians_ReturnsTwo() {
        // Arrange
        // vet1 y vet2 son activos; vet3 es el único inactivo.

        // Act
        long count = veterinarianRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    public void veterinarianRepository_countByActiveFalse_FixtureWithOneInactiveVeterinarian_ReturnsOne() {
        // Arrange
        // vet3 es el único veterinario inactivo de las fixtures.

        // Act
        long count = veterinarianRepository.countByActiveFalse();

        // Assert
        Assertions.assertThat(count).isEqualTo(1L);
    }

    @Test
    public void veterinarianRepository_countByActiveTrue_AfterDeactivatingVeterinarian_ReturnsOne() {
        // Arrange
        // vet2 pasa de activo a inactivo, así que solo queda vet1 activo.
        vet2.setActive(false);
        veterinarianRepository.saveAndFlush(vet2);

        // Act
        long count = veterinarianRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(1L);
    }

    @Test
    public void veterinarianRepository_countByActiveFalse_AfterDeactivatingVeterinarian_ReturnsTwo() {
        // Arrange
        // vet2 pasa de activo a inactivo y se suma a vet3.
        vet2.setActive(false);
        veterinarianRepository.saveAndFlush(vet2);

        // Act
        long count = veterinarianRepository.countByActiveFalse();

        // Assert
        Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    public void veterinarianRepository_countByActiveTrue_AfterDeletingInactiveVeterinarian_ReturnsTwo() {
        // Arrange
        // Borrar a vet3, inactivo, no cambia el total de veterinarios activos.
        veterinarianRepository.deleteById(vet3.getId());
        entityManager.flush();
        entityManager.clear();

        // Act
        long count = veterinarianRepository.countByActiveTrue();

        // Assert
        Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    public void veterinarianRepository_countByActiveFalse_AfterDeletingInactiveVeterinarian_ReturnsZero() {
        // Arrange
        // vet3 era el único veterinario inactivo de las fixtures.
        veterinarianRepository.deleteById(vet3.getId());
        entityManager.flush();
        entityManager.clear();

        // Act
        long count = veterinarianRepository.countByActiveFalse();

        // Assert
        Assertions.assertThat(count).isZero();
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

    private Treatment saveTreatment(Pet pet, Veterinarian veterinarian, LocalDateTime date) {
        return treatmentRepository.save(RepositoryTestData.treatment(pet, veterinarian, date, null));
    }
}
