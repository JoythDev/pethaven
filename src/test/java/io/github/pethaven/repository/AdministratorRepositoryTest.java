package io.github.pethaven.repository;

import io.github.pethaven.entity.Administrator;
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
public class AdministratorRepositoryTest {

    private final AdministratorRepository administratorRepository;
    private final EntityManager entityManager;

    private Administrator admin1;
    private Administrator admin2;
    private Administrator admin3;

    @Autowired
    public AdministratorRepositoryTest(
            AdministratorRepository administratorRepository,
            EntityManager entityManager
    ) {
        this.administratorRepository = administratorRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        admin1 = saveAdministrator();
        admin2 = saveAdministrator();
        admin3 = saveAdministrator();

        entityManager.flush();
    }

    @Test
    public void administratorRepository_save_ValidAdministrator_ReturnsPersistedAdministratorWithGeneratedId() {
        // Arrange
        Administrator administrator = RepositoryTestData.administrator();
        String name = administrator.getName();
        String email = administrator.getEmail();
        String password = administrator.getPassword();

        // Act
        Administrator savedAdministrator = administratorRepository.save(administrator);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(savedAdministrator.getId()).isNotNull();
        Administrator persistedAdministrator = administratorRepository.findById(savedAdministrator.getId()).orElseThrow();
        Assertions.assertThat(persistedAdministrator.getName()).isEqualTo(name);
        Assertions.assertThat(persistedAdministrator.getEmail()).isEqualTo(email);
        Assertions.assertThat(persistedAdministrator.getPassword()).isEqualTo(password);
    }

    @Test
    public void administratorRepository_save_ExistingAdministrator_UpdatesPersistedValues() {
        // Arrange
        entityManager.flush();
        entityManager.clear();
        Administrator administratorToUpdate = administratorRepository.findById(admin1.getId()).orElseThrow();
        administratorToUpdate.setName("Updated Administrator");
        administratorToUpdate.setEmail("updated.administrator@pethaven.test");
        administratorToUpdate.setPassword("updated-secret-password");

        // Act
        administratorRepository.save(administratorToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Administrator persistedAdministrator = administratorRepository.findById(admin1.getId()).orElseThrow();
        Assertions.assertThat(persistedAdministrator.getId()).isEqualTo(admin1.getId());
        Assertions.assertThat(persistedAdministrator.getName()).isEqualTo("Updated Administrator");
        Assertions.assertThat(persistedAdministrator.getEmail()).isEqualTo("updated.administrator@pethaven.test");
        Assertions.assertThat(persistedAdministrator.getPassword()).isEqualTo("updated-secret-password");
        Assertions.assertThat(administratorRepository.count()).isEqualTo(3L);
    }

    @Test
    public void administratorRepository_save_DuplicateEmail_ThrowsDataIntegrityViolation() {
        // Arrange
        // admin1 ya ocupa su correo, protegido por la restricción única de la columna email.
        Administrator duplicateAdministrator = RepositoryTestData.administrator();
        duplicateAdministrator.setEmail(admin1.getEmail());

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> administratorRepository.saveAndFlush(duplicateAdministrator));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void administratorRepository_save_NullName_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna name es obligatoria en la base de datos.
        Administrator administratorWithNullName = RepositoryTestData.administrator();
        administratorWithNullName.setName(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> administratorRepository.saveAndFlush(administratorWithNullName));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void administratorRepository_save_NullEmail_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna email es obligatoria en la base de datos.
        Administrator administratorWithNullEmail = RepositoryTestData.administrator();
        administratorWithNullEmail.setEmail(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> administratorRepository.saveAndFlush(administratorWithNullEmail));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void administratorRepository_save_NullPassword_ThrowsDataIntegrityViolation() {
        // Arrange
        // La columna password es obligatoria en la base de datos.
        Administrator administratorWithNullPassword = RepositoryTestData.administrator();
        administratorWithNullPassword.setPassword(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> administratorRepository.saveAndFlush(administratorWithNullPassword));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void administratorRepository_findById_ExistingId_ReturnsAdministrator() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Administrator> result = administratorRepository.findById(admin1.getId());

        // Assert
        Assertions.assertThat(result).isPresent();
        Administrator foundAdministrator = result.orElseThrow();
        Assertions.assertThat(foundAdministrator.getId()).isEqualTo(admin1.getId());
        Assertions.assertThat(foundAdministrator.getName()).isEqualTo(admin1.getName());
        Assertions.assertThat(foundAdministrator.getEmail()).isEqualTo(admin1.getEmail());
        Assertions.assertThat(foundAdministrator.getPassword()).isEqualTo(admin1.getPassword());
    }

    @Test
    public void administratorRepository_findById_NonExistingId_ReturnsEmptyOptional() {
        // Arrange
        // Long.MAX_VALUE no puede ser un valor de id generado.

        // Act
        Optional<Administrator> result = administratorRepository.findById(Long.MAX_VALUE);

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void administratorRepository_findAll_ExistingAdministrators_ReturnsAllFixtureAdministrators() {
        // Arrange
        // setUp creó exactamente tres administradores: admin1, admin2 y admin3.

        // Act
        List<Administrator> administrators = administratorRepository.findAll();

        // Assert
        Assertions.assertThat(administrators)
                .extracting(Administrator::getId)
                .containsExactlyInAnyOrder(admin1.getId(), admin2.getId(), admin3.getId());
    }

    @Test
    public void administratorRepository_deleteById_ExistingId_RemovesOnlyThatAdministrator() {
        // Arrange
        // admin3 no tiene relaciones, así que el borrado no depende de ninguna clave foránea.
        Long deletedId = admin3.getId();
        long countBeforeDeletion = administratorRepository.count();

        // Act
        administratorRepository.deleteById(deletedId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(administratorRepository.findById(deletedId)).isEmpty();
        Assertions.assertThat(administratorRepository.count()).isEqualTo(countBeforeDeletion - 1);
        Assertions.assertThat(administratorRepository.findById(admin1.getId())).isPresent();
        Assertions.assertThat(administratorRepository.findById(admin2.getId())).isPresent();
    }

    @Test
    public void administratorRepository_findByEmail_ExistingEmail_ReturnsAdministrator() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Administrator> result = administratorRepository.findByEmail(admin2.getEmail());

        // Assert
        Assertions.assertThat(result).isPresent();
        Administrator foundAdministrator = result.orElseThrow();
        Assertions.assertThat(foundAdministrator.getId()).isEqualTo(admin2.getId());
        Assertions.assertThat(foundAdministrator.getName()).isEqualTo(admin2.getName());
        Assertions.assertThat(foundAdministrator.getEmail()).isEqualTo(admin2.getEmail());
    }

    @Test
    public void administratorRepository_findByEmail_UnknownEmail_ReturnsEmptyOptional() {
        // Arrange
        // unknown.administrator@pethaven.test no existe en las fixtures.

        // Act
        Optional<Administrator> result = administratorRepository.findByEmail("unknown.administrator@pethaven.test");

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void administratorRepository_findByEmail_NameOfExistingAdministrator_ReturnsEmptyOptional() {
        // Arrange
        // El nombre de admin1 existe, pero no es el correo de ningún administrador.

        // Act
        Optional<Administrator> result = administratorRepository.findByEmail(admin1.getName());

        // Assert
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void administratorRepository_findByEmail_UpdatedEmail_ReturnsEmptyForOldEmailAndAdministratorForNewEmail() {
        // Arrange
        // Se reemplaza el correo de admin1 por otro distinto de todos los de las fixtures.
        String oldEmail = admin1.getEmail();
        String newEmail = "moved.administrator@pethaven.test";
        entityManager.flush();
        entityManager.clear();
        Administrator administratorToUpdate = administratorRepository.findById(admin1.getId()).orElseThrow();
        administratorToUpdate.setEmail(newEmail);
        administratorRepository.save(administratorToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<Administrator> resultForOldEmail = administratorRepository.findByEmail(oldEmail);
        Optional<Administrator> resultForNewEmail = administratorRepository.findByEmail(newEmail);

        // Assert
        Assertions.assertThat(resultForOldEmail).isEmpty();
        Assertions.assertThat(resultForNewEmail).isPresent();
        Assertions.assertThat(resultForNewEmail.orElseThrow().getId()).isEqualTo(admin1.getId());
    }

    private Administrator saveAdministrator() {
        return administratorRepository.saveAndFlush(RepositoryTestData.administrator());
    }
}
