package io.github.pethaven.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TreatmentDrugRequest(
        @NotNull(message = "El ID del medicamento es obligatorio")
        Long drugId,

        @NotNull(message = "La cantidad de unidades es obligatoria")
        @Positive(message = "La cantidad de unidades debe ser mayor que cero")
        Integer units
) { }
