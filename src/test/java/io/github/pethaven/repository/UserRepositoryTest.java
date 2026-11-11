package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest
public class UserRepositoryTest {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OwnerRepository ownerRepository;
    private final EntityManager entityManager;

    private Role ownerRole;

    @Autowired
    public UserRepositoryTest(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OwnerRepository ownerRepository,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.ownerRepository = ownerRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        ownerRole = roleRepository.saveAndFlush(new Role(Role.OWNER));
    }

    private UserEntity account(String username) {
        UserEntity account = new UserEntity();
        account.setUsername(username);
        account.setPassword("$2a$10$hashed-value");
        account.setRoles(new ArrayList<>(List.of(ownerRole)));
        return account;
    }

    @Test
    public void userRepository_save_ValidAccount_PersistsUsernamePasswordAndRole() {
        // Act
        UserEntity saved = userRepository.saveAndFlush(account("ana@pethaven.test"));
        entityManager.clear();

        // Assert
        UserEntity persisted = userRepository.findByUsername("ana@pethaven.test").orElseThrow();
        Assertions.assertThat(persisted.getId()).isEqualTo(saved.getId());
        Assertions.assertThat(persisted.getPassword()).isEqualTo("$2a$10$hashed-value");
        Assertions.assertThat(persisted.getRoles()).extracting(Role::getName).containsExactly(Role.OWNER);
        Assertions.assertThat(persisted.isEnabled()).isTrue();
        Assertions.assertThat(userRepository.existsByUsername("ana@pethaven.test")).isTrue();
    }

    @Test
    public void userRepository_save_DisabledAccount_PersistsDisabledState() {
        // Arrange
        UserEntity account = account("ana@pethaven.test");
        account.setEnabled(false);

        // Act
        userRepository.saveAndFlush(account);
        entityManager.clear();

        // Assert
        Assertions.assertThat(userRepository.findByUsername("ana@pethaven.test").orElseThrow().isEnabled()).isFalse();
    }

    @Test
    public void userRepository_save_DuplicateUsername_ThrowsDataIntegrityViolation() {
        // Arrange
        userRepository.saveAndFlush(account("ana@pethaven.test"));

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> userRepository.saveAndFlush(account("ana@pethaven.test")));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void userRepository_save_NullUsername_ThrowsDataIntegrityViolation() {
        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> userRepository.saveAndFlush(account(null)));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void userRepository_save_NullPassword_ThrowsDataIntegrityViolation() {
        // Arrange
        UserEntity accountWithNullPassword = account("ana@pethaven.test");
        accountWithNullPassword.setPassword(null);

        // Act
        Throwable thrownException = Assertions.catchThrowable(() -> userRepository.saveAndFlush(accountWithNullPassword));

        // Assert
        Assertions.assertThat(thrownException).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void userRepository_delete_Account_KeepsSharedRole() {
        // Arrange
        // Los roles son compartidos entre cuentas: borrar una cuenta no debe borrar su rol.
        UserEntity first = userRepository.saveAndFlush(account("first@pethaven.test"));
        userRepository.saveAndFlush(account("second@pethaven.test"));

        // Act
        userRepository.delete(first);
        userRepository.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(roleRepository.findByName(Role.OWNER)).isPresent();
        Assertions.assertThat(userRepository.findByUsername("second@pethaven.test").orElseThrow().getRoles())
                .extracting(Role::getName).containsExactly(Role.OWNER);
    }

    @Test
    public void ownerRepository_save_OwnerWithAccount_CascadesAccountPersist() {
        // Arrange
        Owner owner = RepositoryTestData.owner();
        owner.setUser(account(owner.getEmail()));

        // Act
        Owner saved = ownerRepository.saveAndFlush(owner);
        entityManager.clear();

        // Assert
        Owner persisted = ownerRepository.findById(saved.getId()).orElseThrow();
        Assertions.assertThat(persisted.getUser()).isNotNull();
        Assertions.assertThat(persisted.getUser().getUsername()).isEqualTo(owner.getEmail());
    }
}
