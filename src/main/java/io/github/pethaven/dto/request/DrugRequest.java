package io.github.pethaven.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DrugRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 20, message = "El nombre no puede superar los 20 caracteres")
        String name,

        @NotNull(message = "El precio de compra es obligatorio")
        @PositiveOrZero(message = "El precio de compra no puede ser negativo")
        BigDecimal purchasePrice,

        @NotNull(message = "El precio de venta es obligatorio")
        @PositiveOrZero(message = "El precio de venta no puede ser negativo")
        BigDecimal salePrice,

        @NotNull(message = "Las unidades disponibles son obligatorias")
        @PositiveOrZero(message = "Las unidades disponibles no pueden ser negativas")
        Integer unitsAvailable
) { }
