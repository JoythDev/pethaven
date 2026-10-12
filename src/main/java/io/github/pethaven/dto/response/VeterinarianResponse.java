package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Veterinario registrado (sin datos sensibles).
 *
 * @param id         id del veterinario
 * @param document   documento de identidad
 * @param name       nombre completo
 * @param email      correo electrónico
 * @param specialty  especialidad
 * @param photoUrl   URL de la foto (puede ser null)
 * @param attentions número de atenciones realizadas (solo lectura)
 * @param active     true si está activo laboralmente; false en vacaciones o incapacidad
 */
@Schema(description = "Veterinario registrado")
public record VeterinarianResponse(
        @Schema(description = "Id del veterinario", example = "1")
        Long id,

        @Schema(description = "Documento de identidad", example = "241451123")
        String document,

        @Schema(description = "Nombre completo", example = "Dra. Valentina Soler")
        String name,

        @Schema(description = "Correo electrónico", example = "valentina.soler@pethaven.com")
        String email,

        @Schema(description = "Especialidad", example = "Cuidados Críticos")
        String specialty,

        @Schema(description = "URL de la foto (puede ser null)", example = "https://randomuser.me/api/portraits/women/44.jpg")
        String photoUrl,

        @Schema(description = "Número de atenciones realizadas (solo lectura)", example = "0")
        Integer attentions,

        @Schema(description = "Estado laboral: activo (true) o inactivo (false)", example = "true")
        Boolean active
) { }
