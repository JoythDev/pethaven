package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Usuario autenticado y su token de acceso.
 *
 * @param token token de acceso JWT
 * @param username nombre de usuario (o correo electrónico)
 * @param roles  roles del usuario (OWNER, VETERINARIAN y/o ADMIN)
 */
@Schema(description = "Usuario autenticado")
public record AuthResponse(
        @Schema(description = "Token de acceso JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Nombre de usuario (o correo electrónico)", example = "john.doe@example.com")
        String username,

        @Schema(description = "Roles del usuario", example = "OWNER")
        List<String> roles
) { }
