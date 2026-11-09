package io.github.pethaven.security;

import io.github.pethaven.support.WebMvcSecurityTestSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// Configuración transversal de la cadena de seguridad: CORS desde propiedades y consola H2 por entorno.
public class SecurityConfigWebTest {

    private static final String ALLOWED_ORIGIN = "http://allowed.test";
    // Única ruta del API abierta sin sesión en la matriz de permisos: el controlador de prueba la ocupa para
    // probar CORS sobre una respuesta exitosa.
    private static final String PUBLIC_SAMPLE_PATH = "/api/auth/login";

    // ---------- CORS ----------

    @Nested
    @WebMvcTest(controllers = SampleController.class)
    @WebMvcSecurityTestSupport
    @Import(SampleController.class)
    @TestPropertySource(properties = {
            "app.cors.allowed-origins=" + ALLOWED_ORIGIN + ",http://second.test",
            "spring.h2.console.enabled=false"
    })
    public class Cors {

        private final MockMvc mockMvc;

        @Autowired
        public Cors(MockMvc mockMvc) {
            this.mockMvc = mockMvc;
        }

        @Test
        public void cors_PreflightFromConfiguredOrigin_IsAllowedWithoutCredentials() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.options("/api/sample")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                    // El token viaja en la cabecera Authorization, no en cookies: sin credenciales.
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }

        @Test
        public void cors_SecondConfiguredOrigin_IsAlsoAllowed() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.options("/api/sample")
                            .header(HttpHeaders.ORIGIN, "http://second.test")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://second.test"));
        }

        @Test
        public void cors_SuccessfulActualRequestFromConfiguredOrigin_ExposesLocationHeader() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(PUBLIC_SAMPLE_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Location"));
        }

        @Test
        public void cors_RejectedActualRequestFromConfiguredOrigin_StillCarriesCorsHeaders() throws Exception {
            // CORS se resuelve antes que la autorización: aun con la petición rechazada (401, /api/sample no está
            // en la matriz de permisos) la respuesta lleva las cabeceras CORS y el navegador puede leer el error.
            mockMvc.perform(MockMvcRequestBuilders.post("/api/sample").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Location"));
        }

        @Test
        public void cors_PreflightFromUnknownOrigin_IsRejected() throws Exception {
            // MockMvc atiende en http://localhost: un Origin distinto a ese y fuera de la lista es cross-origin no permitido.
            mockMvc.perform(MockMvcRequestBuilders.options("/api/sample")
                            .header(HttpHeaders.ORIGIN, "http://evil.test")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        public void cors_PreflightForUnsupportedMethod_IsRejected() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.options("/api/sample")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        public void cors_PathOutsideApi_HasNoCorsConfiguration() throws Exception {
            // CORS solo se registra para /api/**: otra ruta no recibe cabeceras CORS.
            mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    // ---------- Consola H2 ----------

    @Nested
    @WebMvcTest(controllers = SampleController.class)
    @WebMvcSecurityTestSupport
    @Import(SampleController.class)
    @TestPropertySource(properties = {"spring.h2.console.enabled=true", "spring.h2.console.path=/h2"})
    public class H2ConsoleEnabled {

        private final MockMvc mockMvc;

        @Autowired
        public H2ConsoleEnabled(MockMvc mockMvc) {
            this.mockMvc = mockMvc;
        }

        @Test
        public void h2Console_Enabled_PathIsPublicAndFramesAllowedOnlyFromSameOrigin() throws Exception {
            // El servlet de H2 no existe en este slice: 404 prueba que la seguridad dejó pasar la petición (no 401).
            mockMvc.perform(MockMvcRequestBuilders.get("/h2/login.jsp"))
                    .andExpect(MockMvcResultMatchers.status().isNotFound())
                    .andExpect(MockMvcResultMatchers.header().string("X-Frame-Options", "SAMEORIGIN"));
        }
    }

    @Nested
    @WebMvcTest(controllers = SampleController.class)
    @WebMvcSecurityTestSupport
    @Import(SampleController.class)
    @TestPropertySource(properties = {"spring.h2.console.enabled=false", "spring.h2.console.path=/h2"})
    public class H2ConsoleDisabled {

        private final MockMvc mockMvc;

        @Autowired
        public H2ConsoleDisabled(MockMvc mockMvc) {
            this.mockMvc = mockMvc;
        }

        @Test
        public void h2Console_Disabled_PathRequiresAuthenticationAndFramesAreDenied() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/h2/login.jsp"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.header().string("X-Frame-Options", "DENY"));
        }
    }

    // Controlador de prueba bajo /api/**: los POST imitan una creación con cabecera Location.
    @RestController
    public static class SampleController {

        @GetMapping("/api/sample")
        public String get() {
            return "ok";
        }

        @PostMapping("/api/sample")
        public ResponseEntity<Void> create() {
            return ResponseEntity.created(URI.create("http://localhost/api/sample/1")).build();
        }

        @PostMapping(PUBLIC_SAMPLE_PATH)
        public ResponseEntity<Void> createPublic() {
            return ResponseEntity.created(URI.create("http://localhost/api/sample/1")).build();
        }
    }
}
