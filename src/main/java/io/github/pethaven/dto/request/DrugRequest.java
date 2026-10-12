package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos para registrar o actualizar un medicamento (en PUT es reemplazo total).
 * `unitsSold` no forma parte del request: lo calcula el sistema.
 *
 * @param name           nombre (obligatorio, máx. 120 caracteres, único)
 * @param purchasePrice  precio de compra (obligatorio, no negativo, máx. 10 enteros y 2 decimales)
 * @param salePrice      precio de venta (obligatorio, no negativo, máx. 10 enteros y 2 decimales)
 * @param unitsAvailable unidades disponibles (obligatorias, no negativas)
 */
public record DrugRequest(
        @Schema(description = "Nombre del medicamento", example = "Amoxicilina 250mg")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        @Schema(description = "Precio de compra", example = "8000.00")
        @NotNull(message = "El precio de compra es obligatorio")
        @PositiveOrZero(message = "El precio de compra no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio de compra admite máximo 10 dígitos enteros y 2 decimales")
        BigDecimal purchasePrice,

        @Schema(description = "Precio de venta", example = "15000.00")
        @NotNull(message = "El precio de venta es obligatorio")
        @PositiveOrZero(message = "El precio de venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio de venta admite máximo 10 dígitos enteros y 2 decimales")
        BigDecimal salePrice,

        @Schema(description = "Unidades disponibles en inventario", example = "200")
        @NotNull(message = "Las unidades disponibles son obligatorias")
        @PositiveOrZero(message = "Las unidades disponibles no pueden ser negativas")
        Integer unitsAvailable
) { }
