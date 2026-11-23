package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Horario de un día de la semana.
 *
 * @param day   día de la semana
 * @param open  hora de apertura (null si ese día no atiende)
 * @param close hora de cierre (null si ese día no atiende)
 */
@Schema(description = "Horario de un día")
public record ClinicHoursResponse(
        @Schema(description = "Día de la semana", example = "MONDAY")
        DayOfWeek day,

        @Schema(description = "Apertura (null si está cerrado)", example = "08:00:00")
        LocalTime open,

        @Schema(description = "Cierre (null si está cerrado)", example = "17:00:00")
        LocalTime close
) { }
