package io.github.pethaven.security;

import io.github.pethaven.security.jwt.JwtAccessDeniedHandler;
import io.github.pethaven.security.jwt.JwtAuthEntryPoint;
import io.github.pethaven.security.jwt.JwtFilter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

// Login con el AuthenticationManager real de la aplicación (BCrypt incluido). El estado de la cuenta
// solo se revela después de validar la contraseña: sin ella, siempre "credenciales incorrectas".
public class AuthenticationManagerTest {

    private static final String EMAIL = "julian.restrepo@pethaven.com";
    private static final String PASSWORD = "vet12345";

    private final SecurityConfig securityConfig = new SecurityConfig(
            Mockito.mock(JwtFilter.class), Mockito.mock(JwtAuthEntryPoint.class), Mockito.mock(JwtAccessDeniedHandler.class));
    private final PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
    private final UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
    private final AuthenticationManager authenticationManager =
            securityConfig.authenticationManager(userDetailsService, passwordEncoder);

    @Test
    public void authenticationManager_EnabledAccountCorrectPassword_Authenticates() {
        // Arrange
        givenAccount(true);

        // Act
        Authentication result = authenticationManager.authenticate(credentials(PASSWORD));

        // Assert
        Assertions.assertThat(result.isAuthenticated()).isTrue();
        Assertions.assertThat(result.getName()).isEqualTo(EMAIL);
        // Spring Security 7 añade además la autoridad FACTOR_PASSWORD (factor de autenticación usado).
        Assertions.assertThat(result.getAuthorities()).extracting("authority")
                .contains("ROLE_VETERINARIAN", "FACTOR_PASSWORD");
    }

    @Test
    public void authenticationManager_EnabledAccountWrongPassword_ThrowsBadCredentials() {
        // Arrange
        givenAccount(true);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> authenticationManager.authenticate(credentials("incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticationManager_DisabledAccountCorrectPassword_ThrowsDisabled() {
        // Arrange
        givenAccount(false);

        // Act + Assert
        // Quien conoce la contraseña sí recibe el motivo real (403 "La cuenta está desactivada.").
        Assertions.assertThatThrownBy(() -> authenticationManager.authenticate(credentials(PASSWORD)))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    public void authenticationManager_DisabledAccountWrongPassword_ThrowsBadCredentialsNotDisabled() {
        // Arrange
        givenAccount(false);

        // Act + Assert
        // Sin la contraseña no se puede averiguar que la cuenta existe y está desactivada.
        Assertions.assertThatThrownBy(() -> authenticationManager.authenticate(credentials("incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticationManager_UnknownEmail_ThrowsBadCredentials() {
        // Arrange
        Mockito.when(userDetailsService.loadUserByUsername(EMAIL)).thenThrow(new UsernameNotFoundException("No existe"));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> authenticationManager.authenticate(credentials(PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    private void givenAccount(boolean enabled) {
        Mockito.when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(User.withUsername(EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .disabled(!enabled)
                .roles("VETERINARIAN")
                .build());
    }

    private static UsernamePasswordAuthenticationToken credentials(String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, password);
    }
}
