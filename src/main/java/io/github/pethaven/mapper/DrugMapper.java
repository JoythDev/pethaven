package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.entity.Drug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrugMapper {

    public DrugResponse toResponse(Drug drug) {
        if (drug == null) return null;
        return new DrugResponse(
            drug.getId(),
            drug.getName(),
            drug.getPurchasePrice(),
            drug.getSalePrice(),
            drug.getUnitsAvailable(),
            drug.getUnitsSold()
        );
    }

    public Drug toEntity(DrugRequest request) {
        if (request == null) return null;
        return Drug.builder()
            .name(request.name())
            .purchasePrice(request.purchasePrice())
            .salePrice(request.salePrice())
            .unitsAvailable(request.unitsAvailable())
            .unitsSold(0)
            .build();
    }

    /** Copia los campos editables sobre la entidad */
    public void applyUpdate(DrugRequest request, Drug drug) {
        drug.setName(request.name());
        drug.setPurchasePrice(request.purchasePrice());
        drug.setSalePrice(request.salePrice());
        drug.setUnitsAvailable(request.unitsAvailable());
    }
}
