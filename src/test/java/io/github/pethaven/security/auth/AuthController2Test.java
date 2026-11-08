package io.github.pethaven.security.auth;

import io.github.pethaven.security.auth.dto.LoginRequest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

// Login con Spring Security: los fallos de autenticación responden con ApiErrorResponse, no con 500.
@WebMvcTest(AuthController2.class)
@WebMvcSecurityTestSupport
public class AuthController2Test {

    private static final String LOGIN_URL = "/api/auth/v2/login";
    private static final String LOGIN_BODY = """
            {"username": "john.doe@example.com", "password": "password123"}
            """;

    private final MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    public AuthController2Test(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    public void authController2_login_BadCredentials_Returns401WithErrorBody() throws Exception {
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
    public void authController2_login_DisabledAccount_Returns403WithErrorBody() throws Exception {
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
}
