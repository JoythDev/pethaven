package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un veterinario.
 *
 * @param document  documento de identidad (obligatorio, máx. 20 caracteres)
 * @param name      nombre completo (obligatorio, máx. 100 caracteres)
 * @param email     correo electrónico (obligatorio, válido, máx. 120 caracteres)
 * @param password  contraseña (obligatoria, entre 8 y 255 caracteres)
 * @param specialty especialidad (obligatoria, máx. 80 caracteres)
 * @param photoUrl  URL de la foto (opcional, máx. 500 caracteres)
 */
public record VeterinarianRequest(
        @Schema(description = "Documento de identidad", example = "103949123")
        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar los 20 caracteres")
        String document,

        @Schema(description = "Nombre completo", example = "Dra. Laura Gómez")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @Schema(description = "Correo electrónico", example = "laura.gomez@pethaven.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe ser válido")
        @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
        String email,

        @Schema(description = "Contraseña", example = "vet12345")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
        String password,

        @Schema(description = "Especialidad", example = "Cuidados Críticos")
        @NotBlank(message = "La especialidad es obligatoria")
        @Size(max = 80, message = "La especialidad no puede superar los 80 caracteres")
        String specialty,

        @Schema(description = "URL de la foto (puede ser null)", example = "https://randomuser.me/api/portraits/women/44.jpg")
        @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
        String photoUrl
) { }
