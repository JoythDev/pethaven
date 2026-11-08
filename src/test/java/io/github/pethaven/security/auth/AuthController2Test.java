package io.github.pethaven.security.auth;

import io.github.pethaven.entity.Role;
import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.github.pethaven.security.jwt.JwtProvider;
import io.github.pethaven.support.WebMvcSecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;

// Login con Spring Security (POST /api/auth/login) y perfil autenticado (GET /api/auth/me),
// ejecutados a través de la cadena de seguridad real.
@WebMvcTest(AuthController2.class)
@WebMvcSecurityTestSupport
public class AuthController2Test {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String ME_URL = "/api/auth/me";
    private static final String EMAIL = "john.doe@example.com";
    private static final String LOGIN_BODY = """
            {"email": "john.doe@example.com", "password": "password123"}
            """;

    private final MockMvc mockMvc;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    @Autowired
    public AuthController2Test(MockMvc mockMvc, JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.mockMvc = mockMvc;
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    // ---------- POST /api/auth/login ----------

    @Test
    public void authController_login_ValidCredentials_Returns200WithTokenAndProfile() throws Exception {
        // Arrange
        Mockito.when(authService.login(new LoginRequest(EMAIL, "password123")))
                .thenReturn(new AuthResponse("jwt-token", 7200L, 7L, "John Doe", EMAIL, Role.OWNER));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").value("jwt-token"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.expiresIn").value(7200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.profileId").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("John Doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OWNER"));
    }

    @Test
    public void authController_login_BadCredentials_Returns401WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(authService.login(ArgumentMatchers.any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Correo o contraseña incorrectos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(LOGIN_URL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void authController_login_DisabledAccount_Returns403WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(authService.login(ArgumentMatchers.any(LoginRequest.class)))
                .thenThrow(new DisabledException("User is disabled"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(403))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.FORBIDDEN.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La cuenta está desactivada."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(LOGIN_URL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void authController_login_BlankEmail_Returns400AndDoesNotCallService() throws Exception {
        assertLoginValidationError("""
                {"email": "", "password": "password123"}
                """, "email", "El correo electrónico es obligatorio");
    }

    @Test
    public void authController_login_MalformedEmail_Returns400AndDoesNotCallService() throws Exception {
        assertLoginValidationError("""
                {"email": "no-es-un-correo", "password": "password123"}
                """, "email", "El correo electrónico debe ser válido");
    }

    @Test
    public void authController_login_EmailOver120Characters_Returns400AndDoesNotCallService() throws Exception {
        // Correo de 121 caracteres con formato válido (partes dentro de los límites de @Email): solo falla @Size.
        String longEmail = "usuario.prueba@" + "d".repeat(50) + "." + "e".repeat(50) + ".test";
        org.assertj.core.api.Assertions.assertThat(longEmail).hasSize(121);
        assertLoginValidationError("""
                {"email": "%s", "password": "password123"}
                """.formatted(longEmail), "email", "El correo electrónico no puede superar los 120 caracteres");
    }

    @Test
    public void authController_login_BlankPassword_Returns400AndDoesNotCallService() throws Exception {
        assertLoginValidationError("""
                {"email": "john.doe@example.com", "password": " "}
                """, "password", "La contraseña es obligatoria");
    }

    // ---------- GET /api/auth/me ----------

    @Test
    public void authController_me_ValidBearerToken_Returns200WithAuthenticatedProfile() throws Exception {
        // Arrange
        // JwtProvider y UserDetailsService son los mocks de @WebMvcSecurityTestSupport: el filtro real autentica.
        Mockito.when(jwtProvider.resolveUsername("valid-token")).thenReturn(Optional.of(EMAIL));
        Mockito.when(userDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(User.withUsername(EMAIL).password("{noop}x").roles(Role.OWNER).build());
        Mockito.when(authService.getProfile(EMAIL))
                .thenReturn(new AuthProfileResponse(7L, "John Doe", EMAIL, Role.OWNER));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get(ME_URL).header("Authorization", "Bearer valid-token"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.profileId").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("John Doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OWNER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
    }

    @Test
    public void authController_me_WithoutToken_Returns401AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get(ME_URL))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Se requiere autenticación para acceder a este recurso."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(ME_URL));
        Mockito.verifyNoInteractions(authService);
    }

    private void assertLoginValidationError(String body, String field, String message) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors." + field).value(message))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(LOGIN_URL));
        Mockito.verifyNoInteractions(authService);
    }
}
