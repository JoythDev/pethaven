package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Medicamento registrado.
 *
 * @param id             id del medicamento
 * @param name           nombre
 * @param purchasePrice  precio de compra
 * @param salePrice      precio de venta
 * @param unitsAvailable unidades disponibles
 * @param unitsSold      unidades vendidas (solo lectura: lo calcula el sistema)
 */
@Schema(description = "Medicamento registrado")
public record DrugResponse(
        @Schema(description = "Id del medicamento", example = "1")
        Long id,

        @Schema(description = "Nombre", example = "Amoxicilina 250mg")
        String name,

        @Schema(description = "Precio de compra", example = "8000.00")
        BigDecimal purchasePrice,

        @Schema(description = "Precio de venta", example = "15000.00")
        BigDecimal salePrice,

        @Schema(description = "Unidades disponibles en inventario", example = "200")
        Integer unitsAvailable,

        @Schema(description = "Unidades vendidas (solo lectura: lo calcula el sistema)", example = "0")
        Integer unitsSold
) { }
