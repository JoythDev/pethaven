package io.github.pethaven.security.auth;

import com.jayway.jsonpath.JsonPath;
import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.support.OtpTestSupport;
import io.github.pethaven.support.OtpTestSupport.RecordingOtpMailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Login en dos pasos (contraseña y código) y perfil de punta a punta: cadena de seguridad real, {@code AuthServiceImpl} real y base de datos, sin
 * Open Session In View y sin una transacción del test que mantenga la sesión abierta. Si el perfil dependiera de
 * una relación perezosa, {@code /me} fallaría aquí con {@code LazyInitializationException}.
 * Usa su propia base H2 en memoria para no recrear la que comparten los demás tests de integración.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(OtpTestSupport.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-flow-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.open-in-view=false"
})
public class AuthFlowIntegrationTest {

    private static final String EMAIL = "auth.flow@pethaven.test";
    private static final String PASSWORD = "password123";
    private static final String SPA_ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private RecordingOtpMailSender mailSender;

    private Long ownerId;

    @BeforeEach
    public void setUp() {
        mailSender.clear();
        ownerId = ownerService.createOwner(new OwnerRequest("555000555", "Auth Flow", EMAIL, PASSWORD, "3001234567")).id();
    }

    @AfterEach
    public void tearDown() {
        ownerService.deleteOwnerById(ownerId);
    }

    @Test
    public void login_ThenVerifyOtp_ThenMe_WithoutOpenSessionInView_ReturnsTheProfileOfTheToken() throws Exception {
        // Act: el primer paso no entrega el token, solo el desafío.
        String challengeJson = login(PASSWORD)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.maskedEmail").value("a***@pethaven.test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String challengeId = JsonPath.read(challengeJson, "$.challengeId");

        String loginJson = verify(challengeId, mailSender.lastCodeFor(EMAIL))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.profileId").value(ownerId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginJson, "$.token");

        // Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.profileId").value(ownerId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Auth Flow"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OWNER"));
    }

    @Test
    public void login_WrongPassword_Returns401AndSendsNoCode() throws Exception {
        // Act + Assert
        login("incorrecta").andExpect(MockMvcResultMatchers.status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(mailSender.hasSentTo(EMAIL)).isFalse();
    }

    @Test
    public void verifyOtp_WrongCode_Returns401AndTheTokenIsNotIssued() throws Exception {
        // Arrange
        String challengeJson = login(PASSWORD).andReturn().getResponse().getContentAsString();
        String challengeId = JsonPath.read(challengeJson, "$.challengeId");
        String code = mailSender.lastCodeFor(EMAIL);
        String wrong = code.equals("000000") ? "000001" : "000000";

        // Act + Assert
        verify(challengeId, wrong)
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
        // El código correcto sigue sirviendo mientras queden intentos.
        verify(challengeId, code).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void verifyOtp_WithoutAnyToken_IsReachableAndRejectsAnUnknownChallenge() throws Exception {
        // Los pasos del login son públicos (no hay token todavía): responde el desafío, no el filtro de seguridad.
        verify("no-existe", "123456")
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(
                        "El código venció o ya no es válido. Inicia sesión de nuevo."));
    }

    @Test
    public void resendOtp_RightAfterLogin_Returns429() throws Exception {
        // Arrange
        String challengeJson = login(PASSWORD).andReturn().getResponse().getContentAsString();
        String challengeId = JsonPath.read(challengeJson, "$.challengeId");

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\": \"%s\"}".formatted(challengeId)))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(EMAIL, password)));
    }

    private org.springframework.test.web.servlet.ResultActions verify(String challengeId, String code) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"challengeId": "%s", "code": "%s"}
                        """.formatted(challengeId, code)));
    }

    @Test
    public void corsPreflight_OnAuthenticatedRoute_AnswersBeforeAuthorization() throws Exception {
        // Act + Assert
        // El navegador envía el preflight sin token: debe responderse con CORS, no con 401.
        mockMvc.perform(MockMvcRequestBuilders.options("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, SPA_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SPA_ORIGIN))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization"));
    }
}
