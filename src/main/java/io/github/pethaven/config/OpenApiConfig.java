package io.github.pethaven.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad que usa el botón "Authorize" de Swagger UI. */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI pethavenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PetHaven API")
                        .version("v1")
                        .description("API REST de PetHaven: gestión de la clínica veterinaria "
                                + "(dueños, mascotas, veterinarios, drogas y tratamientos). "
                                + "Obtén un token con POST /api/auth/login y pégalo en \"Authorize\".")
                        .contact(new Contact()
                                .name("Equipo PetHaven")
                                .url("https://github.com/JoythDev/pethaven")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token de acceso emitido por POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
