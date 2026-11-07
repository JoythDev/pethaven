package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para iniciar sesión (dueño, veterinario o administrador).
 *
 * @param username nombre de usuario (obligatorio, válido, máx. 120 caracteres)
 * @param password contraseña (obligatoria)
 */
public record LoginRequest(
        @Schema(description = "Nombre de usuario (o correo electrónico)", example = "john.doe@example.com")
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 120, message = "El nombre de usuario no puede superar los 120 caracteres")
        String username,

        @Schema(description = "Contraseña", example = "password123")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) { }
