package io.github.pethaven.security;

import io.github.pethaven.support.WebMvcSecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Errores producidos dentro de la cadena de filtros de seguridad (antes de llegar al controlador):
// deben responder con el mismo contrato ApiErrorResponse que el resto de la API.
@WebMvcTest(controllers = SecurityErrorContractTest.ProtectedController.class)
@WebMvcSecurityTestSupport
@Import(SecurityErrorContractTest.ProtectedController.class)
public class SecurityErrorContractTest {

    private final MockMvc mockMvc;

    @Autowired
    public SecurityErrorContractTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    public void securityFilterChain_ProtectedPathWithoutToken_Returns401WithApiErrorResponse() throws Exception {
        // Act + Assert
        // Ruta fuera de /api/** (exige autenticación) y sin cabecera Authorization: responde el entry point.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/protected"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Se requiere autenticación para acceder a este recurso."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/protected"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void securityFilterChain_ProtectedPathWithInvalidBearerToken_Returns401WithApiErrorResponse() throws Exception {
        // Act + Assert
        // El JwtProvider simulado rechaza cualquier token (devuelve false): la petición sigue sin autenticar.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/protected")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Se requiere autenticación para acceder a este recurso."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/protected"));
    }

    // Controlador de prueba en una ruta que la configuración de seguridad protege (anyRequest().authenticated()).
    @RestController
    public static class ProtectedController {

        @GetMapping("/test/protected")
        public String protectedResource() {
            return "ok";
        }
    }
}
