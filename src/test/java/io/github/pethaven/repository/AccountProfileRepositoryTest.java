package io.github.pethaven.repository;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.ArrayList;
import java.util.List;

// Búsqueda del perfil (dueño, veterinario o administrador) a partir del username de su cuenta de login.
@DataJpaTest
public class AccountProfileRepositoryTest {

    private final OwnerRepository ownerRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final AdministratorRepository administratorRepository;
    private final RoleRepository roleRepository;
    private final EntityManager entityManager;

    private Owner owner;
    private Veterinarian veterinarian;
    private Administrator administrator;

    @Autowired
    public AccountProfileRepositoryTest(
            OwnerRepository ownerRepository,
            VeterinarianRepository veterinarianRepository,
            AdministratorRepository administratorRepository,
            RoleRepository roleRepository,
            EntityManager entityManager
    ) {
        this.ownerRepository = ownerRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.administratorRepository = administratorRepository;
        this.roleRepository = roleRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        owner = RepositoryTestData.owner();
        owner.setUser(account("owner.account@pethaven.test", Role.OWNER));
        owner = ownerRepository.save(owner);

        veterinarian = RepositoryTestData.veterinarian();
        veterinarian.setUser(account("vet.account@pethaven.test", Role.VETERINARIAN));
        veterinarian = veterinarianRepository.save(veterinarian);

        administrator = RepositoryTestData.administrator();
        administrator.setUser(account("admin.account@pethaven.test", Role.ADMIN));
        administrator = administratorRepository.save(administrator);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void ownerRepository_findByUserUsername_ExistingAccount_ReturnsItsOwner() {
        // Act + Assert
        Assertions.assertThat(ownerRepository.findByUserUsername("owner.account@pethaven.test"))
                .hasValueSatisfying(found -> Assertions.assertThat(found.getId()).isEqualTo(owner.getId()));
    }

    @Test
    public void veterinarianRepository_findByUserUsername_ExistingAccount_ReturnsItsVeterinarian() {
        // Act + Assert
        Assertions.assertThat(veterinarianRepository.findByUserUsername("vet.account@pethaven.test"))
                .hasValueSatisfying(found -> Assertions.assertThat(found.getId()).isEqualTo(veterinarian.getId()));
    }

    @Test
    public void administratorRepository_findByUserUsername_ExistingAccount_ReturnsItsAdministrator() {
        // Act + Assert
        Assertions.assertThat(administratorRepository.findByUserUsername("admin.account@pethaven.test"))
                .hasValueSatisfying(found -> Assertions.assertThat(found.getId()).isEqualTo(administrator.getId()));
    }

    @Test
    public void profileRepositories_findByUserUsername_AccountOfAnotherProfileType_ReturnEmpty() {
        // Act + Assert
        // Cada repositorio solo encuentra perfiles de su propio tipo.
        Assertions.assertThat(ownerRepository.findByUserUsername("vet.account@pethaven.test")).isEmpty();
        Assertions.assertThat(veterinarianRepository.findByUserUsername("admin.account@pethaven.test")).isEmpty();
        Assertions.assertThat(administratorRepository.findByUserUsername("owner.account@pethaven.test")).isEmpty();
    }

    @Test
    public void profileRepositories_findByUserUsername_UnknownUsername_ReturnEmpty() {
        // Act + Assert
        Assertions.assertThat(ownerRepository.findByUserUsername("nobody@pethaven.test")).isEmpty();
        Assertions.assertThat(veterinarianRepository.findByUserUsername("nobody@pethaven.test")).isEmpty();
        Assertions.assertThat(administratorRepository.findByUserUsername("nobody@pethaven.test")).isEmpty();
    }

    private UserEntity account(String username, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
        UserEntity account = new UserEntity();
        account.setUsername(username);
        account.setPassword("$2a$10$hashed-value");
        account.setRoles(new ArrayList<>(List.of(role)));
        return account;
    }
}
