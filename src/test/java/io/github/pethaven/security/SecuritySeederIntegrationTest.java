package io.github.pethaven.security;

import io.github.pethaven.entity.Role;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.repository.UserRepository;
import io.github.pethaven.security.auth.AuthService;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// Arranque real contra una base vacía (perfil test: sin DataLoader) con las variables del primer administrador.
// Usa su propia base H2 en memoria para no recrear la que comparten los demás tests de integración.
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-seeder-test;DB_CLOSE_DELAY=-1",
        "app.seed.admin.name=Admin Inicial",
        "app.seed.admin.email=first.admin@pethaven.test",
        "app.seed.admin.password=first-admin-password"
})
public class SecuritySeederIntegrationTest {

    private final SecuritySeeder securitySeeder;
    private final RoleRepository roleRepository;
    private final AdministratorRepository administratorRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Autowired
    public SecuritySeederIntegrationTest(
            SecuritySeeder securitySeeder,
            RoleRepository roleRepository,
            AdministratorRepository administratorRepository,
            UserRepository userRepository,
            AuthService authService
    ) {
        this.securitySeeder = securitySeeder;
        this.roleRepository = roleRepository;
        this.administratorRepository = administratorRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Test
    public void securitySeeder_Startup_SeedsRolesAndAFirstAdministratorWhoCanLogIn() {
        // Assert
        Assertions.assertThat(roleRepository.findAll()).extracting(Role::getName)
                .containsExactlyInAnyOrder(Role.ADMIN, Role.VETERINARIAN, Role.OWNER);
        Assertions.assertThat(administratorRepository.count()).isEqualTo(1);

        AuthResponse login = authService.login(new LoginRequest("first.admin@pethaven.test", "first-admin-password"));
        Assertions.assertThat(login.role()).isEqualTo(Role.ADMIN);
        Assertions.assertThat(login.name()).isEqualTo("Admin Inicial");
        Assertions.assertThat(login.token()).isNotBlank();
        // La contraseña se guarda con hash BCrypt, nunca en claro.
        Assertions.assertThat(userRepository.findByUsername("first.admin@pethaven.test").orElseThrow().getPassword())
                .startsWith("$2").isNotEqualTo("first-admin-password");
    }

    @Test
    public void securitySeeder_RunAgainstSeededDatabase_IsIdempotent() {
        // Arrange
        long roles = roleRepository.count();
        long administrators = administratorRepository.count();
        long accounts = userRepository.count();

        // Act
        // Equivale a un reinicio contra la misma base persistente.
        securitySeeder.run(null);
        securitySeeder.run(null);

        // Assert
        Assertions.assertThat(roleRepository.count()).isEqualTo(roles).isEqualTo(3);
        Assertions.assertThat(administratorRepository.count()).isEqualTo(administrators).isEqualTo(1);
        Assertions.assertThat(userRepository.count()).isEqualTo(accounts);
    }
}
