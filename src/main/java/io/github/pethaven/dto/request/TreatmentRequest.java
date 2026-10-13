package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

/**
 * Datos para registrar un tratamiento.
 *
 * @param petId          id de la mascota (obligatorio; debe existir y estar activa)
 * @param veterinarianId id del veterinario (obligatorio; debe existir y estar activo)
 * @param date           fecha del tratamiento (opcional; por defecto, la fecha actual; no puede ser futura)
 */
public record TreatmentRequest(
        @Schema(description = "Id de la mascota", example = "1")
        @NotNull(message = "El ID de la mascota es obligatorio")
        Long petId,

        @Schema(description = "Id del veterinario", example = "1")
        @NotNull(message = "El ID del veterinario es obligatorio")
        Long veterinarianId,

        @Schema(description = "Fecha del tratamiento (por defecto, ahora; no puede ser futura)", example = "2026-10-12T10:00:00")
        @PastOrPresent(message = "La fecha del tratamiento no puede ser futura")
        LocalDateTime date
) { }
