package io.github.pethaven.controller;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.AccountDisabledException;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.mapper.AuthMapper;
import io.github.pethaven.service.AdministratorService;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.VeterinarianService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

// Pruebas de los endpoints de login: servicios mockeados y AuthMapper real.
@WebMvcTest(AuthController.class)
@Import(AuthMapper.class)
public class AuthControllerTest {

    private static final String OWNER_LOGIN_PATH = "/api/auth/owners/login";
    private static final String VETERINARIAN_LOGIN_PATH = "/api/auth/veterinarians/login";
    private static final String ADMINISTRATOR_LOGIN_PATH = "/api/auth/administrators/login";

    private static final String VALIDATION_MESSAGE = "La petición tiene campos inválidos.";
    private static final String MALFORMED_BODY_MESSAGE = "El cuerpo de la petición es inválido o está mal formado.";
    private static final String METHOD_NOT_ALLOWED_MESSAGE = "El método HTTP no está permitido para este recurso.";

    private static final String BLANK_EMAIL_MESSAGE = "El correo electrónico es obligatorio";
    private static final String MALFORMED_EMAIL_MESSAGE = "El correo electrónico debe ser válido";
    private static final String EMAIL_TOO_LONG_MESSAGE = "El correo electrónico no puede superar los 120 caracteres";
    private static final String BLANK_PASSWORD_MESSAGE = "La contraseña es obligatoria";

    private static final String INVALID_CREDENTIALS_MESSAGE = "Correo o contraseña incorrectos.";
    private static final String ACCOUNT_DISABLED_MESSAGE = "La cuenta está desactivada.";

    // Correos válidos de 120 y 121 caracteres: 120 es el máximo permitido y 121 debe rechazarse.
    private static final String EMAIL_120_CHARACTERS =
            "usuario.prueba@" + "d".repeat(50) + "." + "e".repeat(49) + ".test";
    private static final String EMAIL_121_CHARACTERS =
            "usuario.prueba@" + "d".repeat(50) + "." + "e".repeat(50) + ".test";

    // JSON truncado: Jackson no puede convertirlo en un LoginRequest.
    private static final String MALFORMED_JSON_BODY = """
            {"email":"carlos@pethaven.test","password":
            """;

    private final MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private VeterinarianService veterinarianService;

    @MockitoBean
    private AdministratorService administratorService;

    @Autowired
    public AuthControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static Owner owner() {
        return Owner.builder()
                .id(1L)
                .document("111222333")
                .name("Carlos Perez")
                .email("carlos@pethaven.test")
                .password("password123")
                .phone("3001234567")
                .build();
    }

    private static Veterinarian veterinarian() {
        return Veterinarian.builder()
                .id(2L)
                .document("444555666")
                .name("Laura Gomez")
                .email("laura@pethaven.test")
                .password("vet-pass-2026")
                .specialty("Medicina general")
                .build();
    }

    private static Administrator administrator() {
        return Administrator.builder()
                .id(3L)
                .name("Admin Principal")
                .email("admin@pethaven.test")
                .password("admin-pass-2026")
                .build();
    }

    private static String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    // Provisional: la contraseña se compara en texto plano. Estas pruebas fijan ese comportamiento
    // y deberán revisarse al implementar Spring Security + BCrypt + JWT antes del deploy a producción.

    // ---------- POST /api/auth/owners/login ----------

    @Test
    public void authController_loginOwner_ValidCredentials_Returns200WithOwnerRole() throws Exception {
        // Arrange
        Mockito.when(ownerService.authenticate("carlos@pethaven.test", "password123")).thenReturn(owner());

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("carlos@pethaven.test", "password123")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Carlos Perez"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("carlos@pethaven.test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OWNER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());

        // Assert
        Mockito.verify(ownerService).authenticate("carlos@pethaven.test", "password123");
        Mockito.verifyNoInteractions(veterinarianService, administratorService);
    }

    @Test
    public void authController_loginOwner_InvalidCredentials_Returns401WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(ownerService.authenticate("carlos@pethaven.test", "incorrecta"))
                .thenThrow(new InvalidCredentialsException());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("carlos@pethaven.test", "incorrecta")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Unauthorized"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(INVALID_CREDENTIALS_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(OWNER_LOGIN_PATH));
    }

    @Test
    public void authController_loginOwner_BlankEmail_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Correo vacío: @NotBlank lo rechaza y @Email lo acepta, así que solo aparece un mensaje.
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("", "password123")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(BLANK_EMAIL_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").doesNotExist());
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    @Test
    public void authController_loginOwner_MalformedEmail_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("correo-sin-arroba", "password123")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(MALFORMED_EMAIL_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").doesNotExist());
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    @Test
    public void authController_loginOwner_EmailOver120Characters_Returns400AndDoesNotCallService() throws Exception {
        // Arrange
        // Comprobación del fixture: el correo mide exactamente 121 caracteres.
        Assertions.assertThat(EMAIL_121_CHARACTERS.length()).isEqualTo(121);

        // Act + Assert
        // Correo de 121 caracteres con formato válido: solo debe fallar @Size(max = 120).
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL_121_CHARACTERS, "password123")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    @Test
    public void authController_loginOwner_EmailOf120Characters_Returns200AndCallsService() throws Exception {
        // Arrange
        // Comprobación del fixture: el correo mide exactamente 120 caracteres.
        Assertions.assertThat(EMAIL_120_CHARACTERS.length()).isEqualTo(120);
        Mockito.when(ownerService.authenticate(EMAIL_120_CHARACTERS, "password123")).thenReturn(owner());

        // Act
        // Correo de 120 caracteres: el límite es inclusivo y debe aceptarse.
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL_120_CHARACTERS, "password123")))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Assert
        Mockito.verify(ownerService).authenticate(EMAIL_120_CHARACTERS, "password123");
    }

    @Test
    public void authController_loginOwner_BlankPassword_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Contraseña solo con espacios: @NotBlank la rechaza; @NotEmpty no lo haría.
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("carlos@pethaven.test", "   ")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(BLANK_PASSWORD_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").doesNotExist());
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    // ---------- POST /api/auth/veterinarians/login ----------

    @Test
    public void authController_loginVeterinarian_ValidCredentials_Returns200WithVeterinarianRole() throws Exception {
        // Arrange
        Mockito.when(veterinarianService.authenticate("laura@pethaven.test", "vet-pass-2026"))
                .thenReturn(veterinarian());

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post(VETERINARIAN_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("laura@pethaven.test", "vet-pass-2026")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Laura Gomez"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("laura@pethaven.test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("VETERINARIAN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());

        // Assert
        Mockito.verify(veterinarianService).authenticate("laura@pethaven.test", "vet-pass-2026");
        Mockito.verifyNoInteractions(ownerService, administratorService);
    }

    @Test
    public void authController_loginVeterinarian_InvalidCredentials_Returns401WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(veterinarianService.authenticate("laura@pethaven.test", "incorrecta"))
                .thenThrow(new InvalidCredentialsException());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(VETERINARIAN_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("laura@pethaven.test", "incorrecta")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Unauthorized"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(INVALID_CREDENTIALS_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(VETERINARIAN_LOGIN_PATH));
    }

    @Test
    public void authController_loginVeterinarian_DisabledAccount_Returns403WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(veterinarianService.authenticate("laura@pethaven.test", "vet-pass-2026"))
                .thenThrow(new AccountDisabledException());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(VETERINARIAN_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("laura@pethaven.test", "vet-pass-2026")))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(403))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Forbidden"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ACCOUNT_DISABLED_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(VETERINARIAN_LOGIN_PATH));
    }

    @Test
    public void authController_loginVeterinarian_BlankEmail_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(VETERINARIAN_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("", "vet-pass-2026")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(BLANK_EMAIL_MESSAGE));
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    // ---------- POST /api/auth/administrators/login ----------

    @Test
    public void authController_loginAdministrator_ValidCredentials_Returns200WithAdminRole() throws Exception {
        // Arrange
        Mockito.when(administratorService.authenticate("admin@pethaven.test", "admin-pass-2026"))
                .thenReturn(administrator());

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post(ADMINISTRATOR_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@pethaven.test", "admin-pass-2026")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Admin Principal"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("admin@pethaven.test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());

        // Assert
        Mockito.verify(administratorService).authenticate("admin@pethaven.test", "admin-pass-2026");
        Mockito.verifyNoInteractions(ownerService, veterinarianService);
    }

    @Test
    public void authController_loginAdministrator_InvalidCredentials_Returns401WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(administratorService.authenticate("admin@pethaven.test", "incorrecta"))
                .thenThrow(new InvalidCredentialsException());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(ADMINISTRATOR_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@pethaven.test", "incorrecta")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Unauthorized"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(INVALID_CREDENTIALS_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(ADMINISTRATOR_LOGIN_PATH));
    }

    @Test
    public void authController_loginAdministrator_BlankPassword_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Contraseña solo con espacios: @NotBlank la rechaza; @NotEmpty no lo haría.
        mockMvc.perform(MockMvcRequestBuilders.post(ADMINISTRATOR_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@pethaven.test", "   ")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(BLANK_PASSWORD_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").doesNotExist());
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    // ---------- Shared request errors ----------

    @Test
    public void authController_loginOwner_MalformedJsonBody_Returns400WithBodyMessage() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON_BODY))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(MALFORMED_BODY_MESSAGE));
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    @Test
    public void authController_loginOwner_EmptyBody_Returns400WithBodyMessage() throws Exception {
        // Act + Assert
        // Sin cuerpo: Spring lanza HttpMessageNotReadableException y el handler responde 400.
        mockMvc.perform(MockMvcRequestBuilders.post(OWNER_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(MALFORMED_BODY_MESSAGE));
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }

    @Test
    public void authController_loginOwner_GetMethod_Returns405AndDoesNotCallService() throws Exception {
        // Act + Assert
        // GET sobre una ruta que solo acepta POST.
        mockMvc.perform(MockMvcRequestBuilders.get(OWNER_LOGIN_PATH))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(405))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(METHOD_NOT_ALLOWED_MESSAGE));
        Mockito.verifyNoInteractions(ownerService, veterinarianService, administratorService);
    }
}
