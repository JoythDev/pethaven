package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Disponibilidad de un día de la ventana de reserva.
 *
 * @param date      día
 * @param open      la clínica atiende ese día
 * @param freeSlots inicio de cada franja con al menos un veterinario libre (vacía si el día está cerrado o lleno)
 */
@Schema(description = "Franjas libres de un día")
public record AvailabilityDayResponse(
        @Schema(description = "Día", example = "2026-11-24")
        LocalDate date,

        @Schema(description = "La clínica atiende ese día", example = "true")
        boolean open,

        @Schema(description = "Inicio de cada franja libre, en hora local de la clínica", example = "[\"08:00:00\", \"08:30:00\"]")
        List<LocalTime> freeSlots
) { }
