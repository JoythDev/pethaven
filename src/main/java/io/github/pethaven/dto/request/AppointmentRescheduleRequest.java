package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Nueva franja de una cita.
 *
 * @param startTime  inicio de la nueva franja, en hora local de la clínica
 * @param ownerNotes notas del dueño (opcional; solo las cambia el dueño, null las conserva)
 */
public record AppointmentRescheduleRequest(
        @Schema(description = "Inicio de la nueva franja en hora local de la clínica", example = "2026-11-25T10:00:00")
        @NotNull(message = "La fecha y hora de la cita es obligatoria")
        LocalDateTime startTime,

        @Schema(description = "Notas del dueño (opcional; null las conserva)", example = "Prefiero en la mañana.")
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
        String ownerNotes
) { }
