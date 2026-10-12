package io.github.pethaven.dto.response;

import java.math.BigDecimal;

public record DrugResponse(
        Long id,
        String name,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer unitsAvailable,
        Integer unitsSold
) { }
