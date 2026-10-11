package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un dueño.
 *
 * @param document documento de identidad (obligatorio, máx. 20 caracteres)
 * @param name     nombre completo (obligatorio, máx. 100 caracteres)
 * @param email    correo electrónico (obligatorio, válido, máx. 120 caracteres)
 * @param password contraseña (obligatoria, entre 8 y 255 caracteres)
 * @param phone    teléfono (obligatorio, máx. 20 caracteres)
 */
public record OwnerRequest(
        @Schema(description = "Documento de identidad", example = "123456789")
        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar los 20 caracteres")
        String document,

        @Schema(description = "Nombre completo", example = "John Doe")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @Schema(description = "Correo electrónico", example = "john.doe@example.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe ser válido")
        @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
        String email,

        @Schema(description = "Contraseña", example = "password123")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
        String password,

        @Schema(description = "Teléfono", example = "3001234567")
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone
) { }
