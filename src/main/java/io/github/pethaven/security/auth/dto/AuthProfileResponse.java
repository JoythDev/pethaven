package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Perfil del usuario autenticado (sin token), para recuperar la sesión en el frontend.
 *
 * @param profileId id del perfil (dueño, veterinario o administrador)
 * @param name      nombre del perfil
 * @param email     correo electrónico (username de la cuenta)
 * @param role      rol del usuario
 */
@Schema(description = "Perfil del usuario autenticado")
public record AuthProfileResponse(
        @Schema(description = "Id del perfil (dueño, veterinario o administrador)", example = "1")
        Long profileId,

        @Schema(description = "Nombre del perfil", example = "John Doe")
        String name,

        @Schema(description = "Correo electrónico", example = "john.doe@example.com")
        String email,

        @Schema(description = "Rol del usuario", example = "OWNER", allowableValues = {"OWNER", "VETERINARIAN", "ADMIN"})
        String role
) { }
