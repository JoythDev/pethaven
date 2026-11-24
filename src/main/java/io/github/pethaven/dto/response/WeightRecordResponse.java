package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Un pesaje del historial de una mascota.
 *
 * @param id     id del registro
 * @param date   fecha y hora del pesaje
 * @param weight peso en kilogramos
 */
@Schema(description = "Pesaje del historial de una mascota")
public record WeightRecordResponse(
        @Schema(description = "Id del registro", example = "12")
        Long id,

        @Schema(description = "Fecha y hora del pesaje", example = "2026-10-12T10:30:15")
        LocalDateTime date,

        @Schema(description = "Peso en kilogramos", example = "12.5")
        Double weight
) { }
