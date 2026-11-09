package io.github.pethaven.security;

import io.github.pethaven.controller.AdministratorDashboardController;
import io.github.pethaven.controller.DrugController;
import io.github.pethaven.controller.OwnerController;
import io.github.pethaven.controller.PetController;
import io.github.pethaven.controller.TreatmentController;
import io.github.pethaven.controller.VeterinarianController;
import io.github.pethaven.entity.Role;
import io.github.pethaven.security.auth.AuthController;
import io.github.pethaven.security.auth.AuthService;
import io.github.pethaven.service.AdministratorDashboardService;
import io.github.pethaven.service.DrugImportService;
import io.github.pethaven.service.DrugService;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import io.github.pethaven.service.TreatmentDrugService;
import io.github.pethaven.service.TreatmentService;
import io.github.pethaven.service.VeterinarianService;
import io.github.pethaven.support.WebMvcSecurityTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Role layer of the permission matrix (odd S11): which roles may reach each API operation.
 * Ownership ("only their own") is checked one layer below and is not covered here.
 * Unauthenticated → 401, authenticated with a role outside the row → 403, allowed role → neither.
 */
@WebMvcTest(controllers = {
        AuthController.class,
        AdministratorDashboardController.class,
        DrugController.class,
        OwnerController.class,
        PetController.class,
        TreatmentController.class,
        VeterinarianController.class
})
@WebMvcSecurityTestSupport
public class PermissionMatrixWebTest {

    private static final String A = Role.ADMIN;
    private static final String V = Role.VETERINARIAN;
    private static final String O = Role.OWNER;
    private static final Set<String> ALL_ROLES = Set.of(A, V, O);

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AdministratorDashboardService dashboardService;
    @MockitoBean
    private DrugService drugService;
    @MockitoBean
    private DrugImportService drugImportService;
    @MockitoBean
    private OwnerService ownerService;
    @MockitoBean
    private PetService petService;
    @MockitoBean
    private TreatmentService treatmentService;
    @MockitoBean
    private TreatmentDrugService treatmentDrugService;
    @MockitoBean
    private VeterinarianService veterinarianService;

    private final MockMvc mockMvc;

    @Autowired
    public PermissionMatrixWebTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Every API operation with the roles allowed to reach it (S11).
    static Stream<Arguments> authenticatedOperations() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/auth/me", ALL_ROLES),

                Arguments.of(HttpMethod.GET, "/api/administrators/dashboard", Set.of(A)),

                Arguments.of(HttpMethod.GET, "/api/drugs", Set.of(A, V)),
                Arguments.of(HttpMethod.GET, "/api/drugs/1", Set.of(A, V)),
                Arguments.of(HttpMethod.POST, "/api/drugs", Set.of(A)),
                Arguments.of(HttpMethod.POST, "/api/drugs/import", Set.of(A)),
                Arguments.of(HttpMethod.PUT, "/api/drugs/1", Set.of(A)),
                Arguments.of(HttpMethod.DELETE, "/api/drugs/1", Set.of(A)),

                Arguments.of(HttpMethod.GET, "/api/owners", Set.of(A, V)),
                Arguments.of(HttpMethod.GET, "/api/owners/1", ALL_ROLES),
                Arguments.of(HttpMethod.POST, "/api/owners", Set.of(V)),
                Arguments.of(HttpMethod.PUT, "/api/owners/1", Set.of(V, O)),
                Arguments.of(HttpMethod.DELETE, "/api/owners/1", Set.of(V)),
                Arguments.of(HttpMethod.GET, "/api/owners/1/pets", ALL_ROLES),

                Arguments.of(HttpMethod.GET, "/api/pets", Set.of(A, V)),
                Arguments.of(HttpMethod.GET, "/api/pets/1", ALL_ROLES),
                Arguments.of(HttpMethod.POST, "/api/pets", Set.of(V)),
                Arguments.of(HttpMethod.PUT, "/api/pets/1", Set.of(V)),
                Arguments.of(HttpMethod.PUT, "/api/pets/1/active", Set.of(V)),
                Arguments.of(HttpMethod.GET, "/api/pets/1/treatments", ALL_ROLES),
                Arguments.of(HttpMethod.GET, "/api/pets/1/photo", ALL_ROLES),
                Arguments.of(HttpMethod.POST, "/api/pets/1/discharge", Set.of(V)),

                Arguments.of(HttpMethod.GET, "/api/treatments", Set.of(A, V)),
                Arguments.of(HttpMethod.GET, "/api/treatments/1", Set.of(A, V)),
                Arguments.of(HttpMethod.POST, "/api/treatments", Set.of(V)),
                Arguments.of(HttpMethod.GET, "/api/treatments/1/drugs", Set.of(A, V)),
                Arguments.of(HttpMethod.POST, "/api/treatments/1/drugs", Set.of(V)),

                Arguments.of(HttpMethod.GET, "/api/veterinarians", Set.of(A)),
                Arguments.of(HttpMethod.GET, "/api/veterinarians/1", Set.of(A, V)),
                Arguments.of(HttpMethod.POST, "/api/veterinarians", Set.of(A)),
                Arguments.of(HttpMethod.PUT, "/api/veterinarians/1", Set.of(A, V)),
                Arguments.of(HttpMethod.PUT, "/api/veterinarians/1/active", Set.of(A)),
                Arguments.of(HttpMethod.GET, "/api/veterinarians/1/treatments", Set.of(A, V))
        );
    }

    // One case per (operation, role): the role is either allowed or denied by the row.
    static Stream<Arguments> operationsByRole() {
        return authenticatedOperations().flatMap(arguments -> {
            Object[] row = arguments.get();
            @SuppressWarnings("unchecked")
            Set<String> allowed = (Set<String>) row[2];
            return Stream.of(A, V, O).map(role -> Arguments.of(row[0], row[1], role, allowed.contains(role)));
        });
    }

    @ParameterizedTest(name = "{0} {1} without token → 401")
    @MethodSource("authenticatedOperations")
    public void operation_WithoutAuthentication_Returns401(HttpMethod method, String path, Set<String> allowed) throws Exception {
        mockMvc.perform(request(method, path))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401));
    }

    @ParameterizedTest(name = "{0} {1} as {2} → allowed={3}")
    @MethodSource("operationsByRole")
    public void operation_ByRole_FollowsPermissionMatrix(HttpMethod method, String path, String role, boolean allowed)
            throws Exception {
        var result = mockMvc.perform(request(method, path).with(SecurityMockMvcRequestPostProcessors.user("user").roles(role)));

        if (allowed) {
            // The request got past authorization; whatever the controller answers (200, 400, 404…) is not ours.
            Assertions.assertThat(result.andReturn().getResponse().getStatus()).isNotIn(401, 403);
        } else {
            result.andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(403))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                            .value("No tienes permisos para acceder a este recurso."));
        }
    }

    @Test
    public void login_WithoutAuthentication_IsPublic() throws Exception {
        int status = mockMvc.perform(request(HttpMethod.POST, "/api/auth/login")).andReturn().getResponse().getStatus();

        Assertions.assertThat(status).isNotIn(401, 403);
    }

    @Test
    public void unlistedApiRoute_AuthenticatedWithAnyRole_IsDenied() throws Exception {
        // Deny by default: an /api route without a rule answers 403, never reaches a handler (no 404).
        mockMvc.perform(MockMvcRequestBuilders.get("/api/unlisted")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles(A, V, O)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void unlistedApiRoute_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/unlisted"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private static AbstractMockHttpServletRequestBuilder<?> request(HttpMethod method, String path) {
        if (path.endsWith("/import")) {
            MockMultipartFile file = new MockMultipartFile("file", "drugs.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);
            return MockMvcRequestBuilders.multipart(path).file(file);
        }
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, path);
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return builder;
    }
}
