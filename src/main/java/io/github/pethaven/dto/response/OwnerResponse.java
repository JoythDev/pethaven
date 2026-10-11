package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dueño registrado (sin datos sensibles).
 *
 * @param id       id del dueño
 * @param document documento de identidad
 * @param name     nombre completo
 * @param email    correo electrónico
 * @param phone    teléfono
 */
@Schema(description = "Dueño registrado")
public record OwnerResponse(
        @Schema(description = "Id del dueño", example = "1")
        Long id,

        @Schema(description = "Documento de identidad", example = "123456789")
        String document,

        @Schema(description = "Nombre completo", example = "John Doe")
        String name,

        @Schema(description = "Correo electrónico", example = "john.doe@example.com")
        String email,

        @Schema(description = "Teléfono", example = "3001234567")
        String phone
) { }
