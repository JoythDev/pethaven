package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.entity.TreatmentDrug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TreatmentDrugMapper {

    public TreatmentDrugResponse toResponse(TreatmentDrug treatmentDrug) {
        if (treatmentDrug == null) return null;
        return new TreatmentDrugResponse(
            treatmentDrug.getId(),
            treatmentDrug.getDrug().getId(),
            treatmentDrug.getDrug().getName(),
            treatmentDrug.getUnits(),
            treatmentDrug.getUnitPurchasePrice(),
            treatmentDrug.getUnitSalePrice()
        );
    }

    public TreatmentDrug toEntity(TreatmentDrugRequest request) {
        if (request == null) return null;
        return TreatmentDrug.builder()
            .units(request.units())
            .build();
    }
}
