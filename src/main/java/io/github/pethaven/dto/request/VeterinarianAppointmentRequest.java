package io.github.pethaven.dto.request;

import io.github.pethaven.entity.AppointmentReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Cita que el veterinario agenda con el dueño de una mascota hospitalizada, asignada a sí mismo.
 *
 * @param petId     id de una mascota hospitalizada
 * @param reason    DISCHARGE (alta / entrega) o BRIEFING (reunión informativa)
 * @param startTime inicio de una franja libre del veterinario, en hora local de la clínica
 */
public record VeterinarianAppointmentRequest(
        @Schema(description = "Id de la mascota hospitalizada", example = "1")
        @NotNull(message = "El ID de la mascota es obligatorio")
        Long petId,

        @Schema(description = "Motivo: DISCHARGE o BRIEFING", example = "DISCHARGE")
        @NotNull(message = "El motivo de la cita es obligatorio")
        AppointmentReason reason,

        @Schema(description = "Inicio de la franja en hora local de la clínica", example = "2026-11-24T11:00:00")
        @NotNull(message = "La fecha y hora de la cita es obligatoria")
        LocalDateTime startTime
) { }
