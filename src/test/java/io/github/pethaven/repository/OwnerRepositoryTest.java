package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

@DataJpaTest
public class OwnerRepositoryTest {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final EntityManager entityManager;

    private Owner owner1;
    private Pet pet1;
    private Pet pet2;
    private Owner owner2;
    private Pet pet3;
    private Owner owner3;

    @Autowired
    public OwnerRepositoryTest(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            EntityManager entityManager
    ) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
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

        entityManager.flush();
    }

    @Test
    public void ownerRepository_save_ValidOwner_ReturnsPersistedOwnerWithGeneratedId() {
        // Arrange
        Owner owner = RepositoryTestData.owner();
        String document = owner.getDocument();
        String name = owner.getName();
        String email = owner.getEmail();
        String phone = owner.getPhone();

        // Act
        Owner savedOwner = ownerRepository.save(owner);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedOwner.getId()).isNotNull();
        Owner persistedOwner = ownerRepository.findById(savedOwner.getId()).orElseThrow();
        Assertions.assertThat(persistedOwner.getDocument()).isEqualTo(document);
        Assertions.assertThat(persistedOwner.getName()).isEqualTo(name);
        Assertions.assertThat(persistedOwner.getEmail()).isEqualTo(email);
        Assertions.assertThat(persistedOwner.getPhone()).isEqualTo(phone);
    }

    @Test
    public void ownerRepository_save_ExistingOwner_UpdatesPersistedValues() {
        // Arrange
        entityManager.flush();
        entityManager.clear();
        Owner ownerToUpdate = ownerRepository.findById(owner1.getId()).orElseThrow();
        ownerToUpdate.setName("Updated Owner");
        ownerToUpdate.setEmail("updated.owner@pethaven.test");
        ownerToUpdate.setPhone("3109998877");

        // Act
        ownerRepository.save(ownerToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Owner persistedOwner = ownerRepository.findById(owner1.getId()).orElseThrow();
        Assertions.assertThat(persistedOwner.getId()).isEqualTo(owner1.getId());
        Assertions.assertThat(persistedOwner.getName()).isEqualTo("Updated Owner");
        Assertions.assertThat(persistedOwner.getEmail()).isEqualTo("updated.owner@pethaven.test");
        Assertions.assertThat(persistedOwner.getPhone()).isEqualTo("3109998877");
        Assertions.assertThat(ownerRepository.count()).isEqualTo(3L);
    }

    @Test
    public void ownerRepository_save_DuplicateDocument_ThrowsDataIntegrityViolation() {
        // Arrange
        // owner1 ya ocupa su documento, protegido por la restricción única de la columna document.
        Owner duplicateOwner = RepositoryTestData.owner();
        duplicateOwner.setDocument(owner1.getDocument());

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> ownerRepository.saveAndFlush(duplicateOwner));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void ownerRepository_save_DuplicateEmail_ThrowsDataIntegrityViolation() {
        // Arrange
        // owner1 ya ocupa su correo, protegido por la restricción única de la columna email.
        Owner duplicateOwner = RepositoryTestData.owner();
        duplicateOwner.setEmail(owner1.getEmail());

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> ownerRepository.saveAndFlush(duplicateOwner));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void ownerRepository_save_NullName_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna name es obligatoria en la base de datos.
        Owner ownerWithNullName = RepositoryTestData.owner();
        ownerWithNullName.setName(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> ownerRepository.saveAndFlush(ownerWithNullName));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void ownerRepository_save_NullDocument_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna document es obligatoria en la base de datos.
        Owner ownerWithNullDocument = RepositoryTestData.owner();
        ownerWithNullDocument.setDocument(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> ownerRepository.saveAndFlush(ownerWithNullDocument));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void ownerRepository_save_NullEmail_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna email es obligatoria en la base de datos.
        Owner ownerWithNullEmail = RepositoryTestData.owner();
        ownerWithNullEmail.setEmail(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> ownerRepository.saveAndFlush(ownerWithNullEmail));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void ownerRepository_save_NullPhone_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna phone es obligatoria en la base de datos.
        Owner ownerWithNullPhone = RepositoryTestData.owner();
        ownerWithNullPhone.setPhone(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> ownerRepository.saveAndFlush(ownerWithNullPhone));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void ownerRepository_findById_ExistingId_ReturnsOwner() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Owner> result = ownerRepository.findById(owner1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Owner foundOwner = result.orElseThrow();
        Assertions.assertThat(foundOwner.getId()).isEqualTo(owner1.getId());
        Assertions.assertThat(foundOwner.getDocument()).isEqualTo(owner1.getDocument());
        Assertions.assertThat(foundOwner.getName()).isEqualTo(owner1.getName());
        Assertions.assertThat(foundOwner.getEmail()).isEqualTo(owner1.getEmail());
        Assertions.assertThat(foundOwner.getPhone()).isEqualTo(owner1.getPhone());
    }

    @Test
    public void ownerRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<Owner> result = ownerRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void ownerRepository_findAll_ExistingOwners_ReturnsAllFixtureOwners() {
        // Arrange
        // setUp creó exactamente tres dueños: owner1, owner2 y owner3.

        // Act
        List<Owner> owners = ownerRepository.findAll();

        // Assert
        Assertions.assertThat(owners)
                .extracting(Owner::getId)
                .containsExactlyInAnyOrder(owner1.getId(), owner2.getId(), owner3.getId());
    }

    @Test
    public void ownerRepository_deleteById_ExistingId_RemovesOnlyThatOwner() {
        // Arrange
        // owner3 no tiene mascotas, así que el borrado no depende de ninguna cascada.
        Long deletedId = owner3.getId();
        long countBeforeDeletion = ownerRepository.count();

        // Act
        ownerRepository.deleteById(deletedId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(ownerRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(ownerRepository.count()).isEqualTo(countBeforeDeletion - 1);
        Assertions.assertThat(ownerRepository.findById(owner1.getId())).isPresent();
        Assertions.assertThat(ownerRepository.findById(owner2.getId())).isPresent();
    }

    @Test
    public void ownerRepository_deleteById_OwnerWithPets_CascadesToPetsAtDatabaseLevel() {
        // Arrange
        // La cascada es física, definida por @OnDelete en Owner.pets: no existe cascada JPA.
        // Se limpia el contexto de persistencia para que las mascotas no queden gestionadas
        // apuntando a un dueño eliminado, igual que en una transacción nueva del servicio.
        entityManager.flush();
        entityManager.clear();
        Long deletedOwnerId = owner1.getId();

        // Act
        ownerRepository.deleteById(deletedOwnerId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(ownerRepository.findById(deletedOwnerId)).isEmpty();
        Assertions.assertThat(petRepository.findById(pet1.getId())).isEmpty();
        Assertions.assertThat(petRepository.findById(pet2.getId())).isEmpty();
        Assertions.assertThat(petRepository.findById(pet3.getId())).isPresent();
    }

    @Test
    public void ownerRepository_findByDocument_ExistingDocument_ReturnsOwner() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Owner> result = ownerRepository.findByDocument(owner1.getDocument());

        // Assert
        Assertions.assertThat(result).isPresent();
        Owner foundOwner = result.orElseThrow();
        Assertions.assertThat(foundOwner.getId()).isEqualTo(owner1.getId());
        Assertions.assertThat(foundOwner.getDocument()).isEqualTo(owner1.getDocument());
        Assertions.assertThat(foundOwner.getEmail()).isEqualTo(owner1.getEmail());
    }

    @Test
    public void ownerRepository_findByDocument_UnknownDocument_ReturnsEmptyOptional() {
        // Arrange
        // DOC-UNKNOWN no existe en las fixtures.

        // Act
        Optional<Owner> result = ownerRepository.findByDocument("DOC-UNKNOWN");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void ownerRepository_findByDocument_EmailOfExistingOwner_ReturnsEmptyOptional() {
        // Arrange
        // El correo de owner1 existe, pero no es el documento de ningún dueño.

        // Act
        Optional<Owner> result = ownerRepository.findByDocument(owner1.getEmail());

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void ownerRepository_findByEmail_ExistingEmail_ReturnsOwner() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Owner> result = ownerRepository.findByEmail(owner1.getEmail());

        // Assert
        Assertions.assertThat(result).isPresent();
        Owner foundOwner = result.orElseThrow();
        Assertions.assertThat(foundOwner.getId()).isEqualTo(owner1.getId());
        Assertions.assertThat(foundOwner.getEmail()).isEqualTo(owner1.getEmail());
        Assertions.assertThat(foundOwner.getDocument()).isEqualTo(owner1.getDocument());
    }

    @Test
    public void ownerRepository_findByEmail_UnknownEmail_ReturnsEmptyOptional() {
        // Arrange
        // unknown.owner@pethaven.test no existe en las fixtures.

        // Act
        Optional<Owner> result = ownerRepository.findByEmail("unknown.owner@pethaven.test");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void ownerRepository_findByEmail_DocumentOfExistingOwner_ReturnsEmptyOptional() {
        // Arrange
        // El documento de owner1 existe, pero no es el correo de ningún dueño.

        // Act
        Optional<Owner> result = ownerRepository.findByEmail(owner1.getDocument());

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void ownerRepository_existsByDocument_ExistingDocument_ReturnsTrue() {
        // Arrange
        // owner1 ocupa su propio documento.

        // Act
        boolean exists = ownerRepository.existsByDocument(owner1.getDocument());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void ownerRepository_existsByDocument_UnknownDocument_ReturnsFalse() {
        // Arrange
        // DOC-UNKNOWN no existe en las fixtures.

        // Act
        boolean exists = ownerRepository.existsByDocument("DOC-UNKNOWN");

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByDocument_EmailOfExistingOwner_ReturnsFalse() {
        // Arrange
        // El correo de owner1 existe, pero no como documento de ningún dueño.

        // Act
        boolean exists = ownerRepository.existsByDocument(owner1.getEmail());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByEmail_ExistingEmail_ReturnsTrue() {
        // Arrange
        // owner1 ocupa su propio correo.

        // Act
        boolean exists = ownerRepository.existsByEmail(owner1.getEmail());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void ownerRepository_existsByEmail_UnknownEmail_ReturnsFalse() {
        // Arrange
        // unknown.owner@pethaven.test no existe en las fixtures.

        // Act
        boolean exists = ownerRepository.existsByEmail("unknown.owner@pethaven.test");

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByEmail_DocumentOfExistingOwner_ReturnsFalse() {
        // Arrange
        // El documento de owner1 existe, pero no como correo de ningún dueño.

        // Act
        boolean exists = ownerRepository.existsByEmail(owner1.getDocument());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByDocumentAndIdNot_SameDocumentWithOwnId_ReturnsFalse() {
        // Arrange
        // El documento pertenece a owner1 y se excluye su propio id, así que ningún otro dueño lo tiene.

        // Act
        boolean exists = ownerRepository.existsByDocumentAndIdNot(owner1.getDocument(), owner1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByDocumentAndIdNot_SameDocumentWithAnotherOwnerId_ReturnsTrue() {
        // Arrange
        // El documento pertenece a owner1 y se consulta excluyendo el id de owner2.

        // Act
        boolean exists = ownerRepository.existsByDocumentAndIdNot(owner1.getDocument(), owner2.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void ownerRepository_existsByDocumentAndIdNot_UnknownDocument_ReturnsFalse() {
        // Arrange
        // DOC-UNKNOWN no existe en las fixtures, sea cual sea el id excluido.

        // Act
        boolean exists = ownerRepository.existsByDocumentAndIdNot("DOC-UNKNOWN", owner1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByEmailAndIdNot_SameEmailWithOwnId_ReturnsFalse() {
        // Arrange
        // El correo pertenece a owner1 y se excluye su propio id, así que ningún otro dueño lo tiene.

        // Act
        boolean exists = ownerRepository.existsByEmailAndIdNot(owner1.getEmail(), owner1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void ownerRepository_existsByEmailAndIdNot_SameEmailWithAnotherOwnerId_ReturnsTrue() {
        // Arrange
        // El correo pertenece a owner1 y se consulta excluyendo el id de owner2.

        // Act
        boolean exists = ownerRepository.existsByEmailAndIdNot(owner1.getEmail(), owner2.getId());

        // Assert
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void ownerRepository_existsByEmailAndIdNot_UnknownEmail_ReturnsFalse() {
        // Arrange
        // unknown.owner@pethaven.test no existe en las fixtures, sea cual sea el id excluido.

        // Act
        boolean exists = ownerRepository.existsByEmailAndIdNot("unknown.owner@pethaven.test", owner1.getId());

        // Assert
        Assertions.assertThat(exists).isFalse();
    }

    private Owner saveOwner() {
        return ownerRepository.saveAndFlush(RepositoryTestData.owner());
    }

    private Pet savePet(Owner owner) {
        return petRepository.saveAndFlush(RepositoryTestData.pet(owner));
    }
}
