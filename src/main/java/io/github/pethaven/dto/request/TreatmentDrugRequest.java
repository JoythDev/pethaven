package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos para administrar un medicamento en un tratamiento.
 *
 * @param drugId id del medicamento (obligatorio; debe existir y tener stock)
 * @param units  unidades a administrar (obligatorias, mayores a cero)
 */
public record TreatmentDrugRequest(
        @Schema(description = "Id del medicamento", example = "1")
        @NotNull(message = "El ID del medicamento es obligatorio")
        Long drugId,

        @Schema(description = "Unidades a administrar", example = "3")
        @NotNull(message = "La cantidad de unidades es obligatoria")
        @Positive(message = "La cantidad de unidades debe ser mayor que cero")
        Integer units
) { }
