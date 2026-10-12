package io.github.pethaven.dto.response;

import java.math.BigDecimal;

public record TreatmentDrugResponse(
        Long id,
        Long drugId,
        String drugName,
        Integer units,
        BigDecimal unitPurchasePrice,
        BigDecimal unitSalePrice
) { }
