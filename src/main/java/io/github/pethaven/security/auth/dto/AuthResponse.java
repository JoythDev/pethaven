package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de un inicio de sesión: token de acceso y perfil del usuario autenticado.
 *
 * @param token     token de acceso JWT (se envía como {@code Authorization: Bearer <token>})
 * @param expiresIn segundos de vigencia del token desde su emisión
 * @param profileId id del perfil (dueño, veterinario o administrador)
 * @param name      nombre del perfil
 * @param email     correo electrónico (username de la cuenta)
 * @param role      rol del usuario
 */
@Schema(description = "Usuario autenticado con su token de acceso")
public record AuthResponse(
        @Schema(description = "Token de acceso JWT", example = "eyJhbGciOiJIUzUxMiJ9...")
        String token,

        @Schema(description = "Segundos de vigencia del token", example = "7200")
        long expiresIn,

        @Schema(description = "Id del perfil (dueño, veterinario o administrador)", example = "1")
        Long profileId,

        @Schema(description = "Nombre del perfil", example = "John Doe")
        String name,

        @Schema(description = "Correo electrónico", example = "john.doe@example.com")
        String email,

        @Schema(description = "Rol del usuario", example = "OWNER", allowableValues = {"OWNER", "VETERINARIAN", "ADMIN"})
        String role
) { }
