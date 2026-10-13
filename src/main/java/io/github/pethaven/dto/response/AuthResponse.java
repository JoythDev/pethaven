package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Usuario autenticado (sin datos sensibles).
 *
 * @param id    id del usuario
 * @param name  nombre completo
 * @param email correo electrónico
 * @param role  rol del usuario (OWNER o VETERINARIAN)
 */
@Schema(description = "Usuario autenticado")
public record AuthResponse(
        @Schema(description = "Id del usuario", example = "1")
        Long id,

        @Schema(description = "Nombre completo", example = "John Doe")
        String name,

        @Schema(description = "Correo electrónico", example = "john.doe@example.com")
        String email,

        @Schema(description = "Rol del usuario", example = "OWNER")
        AuthRole role
) { }
