package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Renglón de medicamento administrado dentro de un tratamiento.
 *
 * @param id                id del renglón
 * @param drugId            id del medicamento
 * @param drugName          nombre del medicamento
 * @param units             unidades administradas
 * @param unitPurchasePrice precio de compra unitario al momento de administrar
 * @param unitSalePrice     precio de venta unitario al momento de administrar
 */
@Schema(description = "Medicamento administrado en un tratamiento")
public record TreatmentDrugResponse(
        @Schema(description = "Id del renglón", example = "1")
        Long id,

        @Schema(description = "Id del medicamento", example = "1")
        Long drugId,

        @Schema(description = "Nombre del medicamento", example = "Amoxicilina 250mg")
        String drugName,

        @Schema(description = "Unidades administradas", example = "3")
        Integer units,

        @Schema(description = "Precio de compra unitario al momento de administrar", example = "8000.00")
        BigDecimal unitPurchasePrice,

        @Schema(description = "Precio de venta unitario al momento de administrar", example = "15000.00")
        BigDecimal unitSalePrice
) { }
