package io.github.pethaven.service;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.repository.UserRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Pruebas del servicio con mocks: el UserAccountService es real (con BCrypt) y los repositorios
// están simulados, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class AdministratorServiceMockTest {

    // Coste bajo de BCrypt para que los tests sean rápidos; el algoritmo es el mismo.
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private AdministratorRepository administratorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    private AdministratorServiceImpl administratorService;

    @BeforeEach
    public void setUp() {
        UserAccountService userAccountService = new UserAccountServiceImpl(userRepository, roleRepository, PASSWORD_ENCODER);
        administratorService = new AdministratorServiceImpl(administratorRepository, userAccountService);
    }

    // ---------- Helpers ----------

    private Administrator administrator(String email, String password) {
        Administrator administrator = RepositoryTestData.administrator();
        administrator.setEmail(email);
        UserEntity account = new UserEntity();
        account.setUsername(email);
        account.setPassword(PASSWORD_ENCODER.encode(password));
        account.setRoles(new ArrayList<>(List.of(new Role(Role.ADMIN))));
        administrator.setUser(account);
        return administrator;
    }

    // ---------- authenticate ----------
    // Comportamiento provisional: la contraseña se valida con BCrypt contra la cuenta del administrador.
    // Estos tests fijan ese comportamiento hasta que se implemente el AuthenticationProvider + JWT.

    @Test
    public void administratorService_authenticate_CorrectPassword_ReturnsSameAdministrator() {
        // Arrange
        Administrator administrator = administrator("admin@pethaven.test", "secret-password");
        Mockito.when(administratorRepository.findByEmail("admin@pethaven.test"))
                .thenReturn(Optional.of(administrator));

        // Act
        Administrator result = administratorService.authenticate("admin@pethaven.test", "secret-password");

        // Assert
        Assertions.assertThat(result).isSameAs(administrator);
        Mockito.verify(administratorRepository).findByEmail("admin@pethaven.test");
    }

    @Test
    public void administratorService_authenticate_WrongPassword_ThrowsInvalidCredentials() {
        // Arrange
        Administrator administrator = administrator("admin@pethaven.test", "secret-password");
        Mockito.when(administratorRepository.findByEmail("admin@pethaven.test"))
                .thenReturn(Optional.of(administrator));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> administratorService.authenticate("admin@pethaven.test", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    public void administratorService_authenticate_UnknownEmail_ThrowsInvalidCredentials() {
        // Arrange
        Mockito.when(administratorRepository.findByEmail("nadie@pethaven.test")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> administratorService.authenticate("nadie@pethaven.test", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    public void administratorService_authenticate_PasswordWithDifferentCase_ThrowsInvalidCredentials() {
        // Arrange
        Administrator administrator = administrator("admin@pethaven.test", "Secret-Password");
        Mockito.when(administratorRepository.findByEmail("admin@pethaven.test"))
                .thenReturn(Optional.of(administrator));

        // Act + Assert
        // BCrypt distingue mayúsculas de minúsculas.
        Assertions.assertThatThrownBy(() -> administratorService.authenticate("admin@pethaven.test", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    public void administratorService_authenticate_ProfileWithoutAccount_ThrowsInvalidCredentials() {
        // Arrange
        // Un perfil sin cuenta de login no tiene credenciales que validar.
        Administrator administrator = RepositoryTestData.administrator();
        administrator.setEmail("admin@pethaven.test");
        Mockito.when(administratorRepository.findByEmail("admin@pethaven.test"))
                .thenReturn(Optional.of(administrator));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> administratorService.authenticate("admin@pethaven.test", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }
}
