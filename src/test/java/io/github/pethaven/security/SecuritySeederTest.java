package io.github.pethaven.security;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.service.UserAccountService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Optional;

// Datos de seguridad mínimos en cualquier entorno: los tres roles y, si no hay ninguno, el primer administrador.
public class SecuritySeederTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
    private final AdministratorRepository administratorRepository = Mockito.mock(AdministratorRepository.class);
    private final UserAccountService userAccountService = Mockito.mock(UserAccountService.class);

    private SecuritySeeder seeder(String name, String email, String password) {
        return new SecuritySeeder(roleRepository, administratorRepository, userAccountService, VALIDATOR,
                name, email, password);
    }

    private void givenAllRolesExist() {
        Mockito.when(roleRepository.findByName(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.of(new Role(invocation.getArgument(0, String.class))));
    }

    // ---------- roles ----------

    @Test
    public void securitySeeder_run_EmptyDatabase_CreatesTheThreeRoles() {
        // Arrange
        Mockito.when(roleRepository.findByName(ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        Mockito.when(administratorRepository.count()).thenReturn(1L);

        // Act
        seeder("Administrador", "", "").run(null);

        // Assert
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        Mockito.verify(roleRepository, Mockito.times(3)).save(captor.capture());
        Assertions.assertThat(captor.getAllValues()).extracting(Role::getName)
                .containsExactlyInAnyOrder(Role.ADMIN, Role.VETERINARIAN, Role.OWNER);
    }

    @Test
    public void securitySeeder_run_SomeRolesExist_CreatesOnlyTheMissingOnes() {
        // Arrange
        Mockito.when(roleRepository.findByName(ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        Mockito.when(roleRepository.findByName(Role.ADMIN)).thenReturn(Optional.of(new Role(Role.ADMIN)));
        Mockito.when(administratorRepository.count()).thenReturn(1L);

        // Act
        seeder("Administrador", "", "").run(null);

        // Assert
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        Mockito.verify(roleRepository, Mockito.times(2)).save(captor.capture());
        Assertions.assertThat(captor.getAllValues()).extracting(Role::getName)
                .containsExactlyInAnyOrder(Role.VETERINARIAN, Role.OWNER);
    }

    @Test
    public void securitySeeder_run_EverythingAlreadySeeded_WritesNothing() {
        // Arrange
        // Segundo arranque contra la misma base: idempotente aunque las variables sigan definidas.
        givenAllRolesExist();
        Mockito.when(administratorRepository.count()).thenReturn(1L);

        // Act
        seeder("Administrador", "admin@pethaven.test", "admin-password").run(null);

        // Assert
        Mockito.verify(roleRepository, Mockito.never()).save(ArgumentMatchers.any());
        Mockito.verify(administratorRepository, Mockito.never()).save(ArgumentMatchers.any());
        Mockito.verifyNoInteractions(userAccountService);
    }

    // ---------- primer administrador ----------

    @Test
    public void securitySeeder_run_NoAdministratorAndCredentialsConfigured_CreatesAdministratorWithAccount() {
        // Arrange
        givenAllRolesExist();
        Mockito.when(administratorRepository.count()).thenReturn(0L);
        UserEntity account = new UserEntity();
        Mockito.when(userAccountService.createAccount("admin@pethaven.test", "admin-password", Role.ADMIN))
                .thenReturn(account);

        // Act
        seeder("Admin Producción", "admin@pethaven.test", "admin-password").run(null);

        // Assert
        ArgumentCaptor<Administrator> captor = ArgumentCaptor.forClass(Administrator.class);
        Mockito.verify(administratorRepository).save(captor.capture());
        Administrator saved = captor.getValue();
        Assertions.assertThat(saved.getName()).isEqualTo("Admin Producción");
        Assertions.assertThat(saved.getEmail()).isEqualTo("admin@pethaven.test");
        Assertions.assertThat(saved.getUser()).isSameAs(account);
    }

    @Test
    public void securitySeeder_run_NoAdministratorAndNoCredentials_CreatesNothing() {
        // Arrange
        // Desarrollo y pruebas: sin ADMIN_EMAIL ni ADMIN_PASSWORD no se crea nadie (solo un aviso en el log).
        givenAllRolesExist();
        Mockito.when(administratorRepository.count()).thenReturn(0L);

        // Act
        seeder("Administrador", "", "").run(null);

        // Assert
        Mockito.verify(administratorRepository, Mockito.never()).save(ArgumentMatchers.any());
        Mockito.verifyNoInteractions(userAccountService);
    }

    @Test
    public void securitySeeder_run_InvalidEmail_FailsStartupWithoutCreatingAdministrator() {
        assertInvalidConfiguration("Administrador", "no-es-un-correo", "admin-password", "ADMIN_EMAIL");
    }

    @Test
    public void securitySeeder_run_ShortPassword_FailsStartupWithoutCreatingAdministrator() {
        assertInvalidConfiguration("Administrador", "admin@pethaven.test", "corta", "ADMIN_PASSWORD");
    }

    @Test
    public void securitySeeder_run_EmailWithoutPassword_FailsStartupWithoutCreatingAdministrator() {
        assertInvalidConfiguration("Administrador", "admin@pethaven.test", "", "ADMIN_PASSWORD");
    }

    @Test
    public void securitySeeder_run_PasswordWithoutEmail_FailsStartupWithoutCreatingAdministrator() {
        assertInvalidConfiguration("Administrador", "", "admin-password", "ADMIN_EMAIL");
    }

    @Test
    public void securitySeeder_run_InvalidConfiguration_NeverExposesThePassword() {
        // Arrange
        givenAllRolesExist();
        Mockito.when(administratorRepository.count()).thenReturn(0L);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> seeder("Administrador", "no-es-un-correo", "super-secreta-123").run(null))
                .isInstanceOf(IllegalStateException.class)
                .message().doesNotContain("super-secreta-123");
    }

    private void assertInvalidConfiguration(String name, String email, String password, String expectedVariable) {
        // Arrange
        givenAllRolesExist();
        Mockito.when(administratorRepository.count()).thenReturn(0L);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> seeder(name, email, password).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedVariable);
        Mockito.verify(administratorRepository, Mockito.never()).save(ArgumentMatchers.any());
        Mockito.verifyNoInteractions(userAccountService);
    }
}
