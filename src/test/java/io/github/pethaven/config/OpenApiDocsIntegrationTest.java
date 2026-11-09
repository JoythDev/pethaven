package io.github.pethaven.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

// The generated OpenAPI document marks only the public operations as not requiring the bearer token.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_Login_OverridesGlobalBearerRequirementWithNone() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.paths['/api/auth/login'].post.security").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.paths['/api/auth/login'].post.security").isEmpty());
    }

    @Test
    void apiDocs_ProtectedOperation_InheritsGlobalBearerRequirement() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.paths['/api/auth/me'].get.security").doesNotExist());
    }
}
