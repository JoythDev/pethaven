package io.github.pethaven.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

// Swagger UI debe poder enviar el token JWT (botón "Authorize") a las operaciones protegidas.
public class OpenApiConfigTest {

    @Test
    public void openApiConfig_pethavenOpenApi_DeclaresBearerJwtSchemeRequiredGlobally() {
        // Act
        OpenAPI openApi = new OpenApiConfig().pethavenOpenApi();

        // Assert
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_SCHEME);
        Assertions.assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        Assertions.assertThat(scheme.getScheme()).isEqualTo("bearer");
        Assertions.assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        Assertions.assertThat(openApi.getSecurity())
                .anySatisfy(requirement -> Assertions.assertThat(requirement).containsKey(OpenApiConfig.BEARER_SCHEME));
        Assertions.assertThat(openApi.getInfo().getTitle()).isEqualTo("PetHaven API");
    }
}
