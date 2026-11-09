package io.github.pethaven.security;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.UserAccountService;
import jakarta.validation.Validator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// Si ADMIN_EMAIL ya es el usuario de otra cuenta, el arranque falla en lugar de reutilizarla o crear un
// administrador sin cuenta propia.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SecuritySeederDuplicateAccountIntegrationTest {

    private static final String TAKEN_EMAIL = "taken.admin@pethaven.test";

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AdministratorRepository administratorRepository;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private Validator validator;
    @Autowired
    private OwnerService ownerService;

    @Test
    public void securitySeeder_run_AdminEmailAlreadyUsedByAnotherAccount_FailsStartupWithoutCreatingAdministrator() {
        // Arrange
        ownerService.createOwner(new OwnerRequest("666000666", "Dueño Existente", TAKEN_EMAIL, "password123", "3001234567"));
        administratorRepository.deleteAll();
        SecuritySeeder seeder = new SecuritySeeder(roleRepository, administratorRepository, userAccountService,
                validator, "Admin Inicial", TAKEN_EMAIL, "admin-password");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existe una cuenta con el usuario '" + TAKEN_EMAIL + "'.");
        Assertions.assertThat(administratorRepository.count()).isZero();
    }
}
