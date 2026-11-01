package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para iniciar sesión (dueño o veterinario).
 *
 * @param email    correo electrónico (obligatorio, válido, máx. 120 caracteres)
 * @param password contraseña (obligatoria)
 */
public record LoginRequest(
        @Schema(description = "Correo electrónico", example = "john.doe@example.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe ser válido")
        @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
        String email,

        @Schema(description = "Contraseña", example = "password123")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) { }
