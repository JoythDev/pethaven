package io.github.pethaven.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pethavenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PetHaven API")
                        .version("v1")
                        .description("API REST de PetHaven: gestión de la clínica veterinaria "
                                + "(dueños, mascotas, veterinarios, drogas y tratamientos).")
                        .contact(new Contact()
                                .name("Equipo PetHaven")
                                .url("https://github.com/JoythDev/pethaven")));
    }
}
