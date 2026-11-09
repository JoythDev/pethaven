package io.github.pethaven.security.auth;

import com.jayway.jsonpath.JsonPath;
import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.service.OwnerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Login y perfil de punta a punta: cadena de seguridad real, {@code AuthServiceImpl} real y base de datos, sin
 * Open Session In View y sin una transacción del test que mantenga la sesión abierta. Si el perfil dependiera de
 * una relación perezosa, {@code /me} fallaría aquí con {@code LazyInitializationException}.
 * Usa su propia base H2 en memoria para no recrear la que comparten los demás tests de integración.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    private Long ownerId;

    @BeforeEach
    public void setUp() {
        ownerId = ownerService.createOwner(new OwnerRequest("555000555", "Auth Flow", EMAIL, PASSWORD, "3001234567")).id();
    }

    @AfterEach
    public void tearDown() {
        ownerService.deleteOwnerById(ownerId);
    }

    @Test
    public void login_ThenMe_WithoutOpenSessionInView_ReturnsTheProfileOfTheToken() throws Exception {
        // Arrange
        String loginBody = """
                {"email": "%s", "password": "%s"}
                """.formatted(EMAIL, PASSWORD);

        // Act
        String loginJson = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
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
