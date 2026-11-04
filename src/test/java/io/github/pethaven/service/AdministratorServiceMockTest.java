package io.github.pethaven.service;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

// Pruebas del servicio con mocks: el repositorio está simulado, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class AdministratorServiceMockTest {

    @Mock
    private AdministratorRepository administratorRepository;

    private AdministratorServiceImpl administratorService;

    @BeforeEach
    public void setUp() {
        administratorService = new AdministratorServiceImpl(administratorRepository);
    }

    // ---------- Helpers ----------

    private Administrator administrator(String email, String password) {
        Administrator administrator = RepositoryTestData.administrator();
        administrator.setEmail(email);
        administrator.setPassword(password);
        return administrator;
    }

    // ---------- authenticate ----------
    // Comportamiento provisional: la contraseña se compara en texto plano. Estos tests fijan
    // ese comportamiento hasta que se implemente Spring Security + BCrypt + JWT.

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
        // La comparación provisional distingue mayúsculas de minúsculas, igual que String.equals.
        Assertions.assertThatThrownBy(() -> administratorService.authenticate("admin@pethaven.test", "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }
}
