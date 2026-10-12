package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Treatment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TreatmentMapper {

    private final TreatmentDrugMapper treatmentDrugMapper;

    public TreatmentResponse toResponse(Treatment treatment) {
        if (treatment == null) return null;
        return new TreatmentResponse(
            treatment.getId(),
            treatment.getDate(),
            treatment.getPet().getId(),
            treatment.getPet().getName(),
            treatment.getVeterinarian().getId(),
            treatment.getVeterinarian().getName(),
            treatment.getTreatmentDrugs().stream()
                .map(treatmentDrugMapper::toResponse)
                .toList()
        );
    }

    public Treatment toEntity(TreatmentRequest request) {
        if (request == null) return null;
        if (request.date() == null) {
            return Treatment.builder()
                .date(LocalDateTime.now())
                .build();
        }
        return Treatment.builder()
            .date(request.date())
            .build();
    }
}
