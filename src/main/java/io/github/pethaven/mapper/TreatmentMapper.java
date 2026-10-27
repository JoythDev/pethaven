package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Treatment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
            treatment.getWeight(),
            treatment.getDiagnosis(),
            treatment.getEvolution(),
            treatment.getNotes(),
            treatment.getTreatmentDrugs().stream()
                .map(treatmentDrugMapper::toResponse)
                .toList()
        );
    }

    public Treatment toEntity(TreatmentRequest request) {
        if (request == null) return null;
        return Treatment.builder()
            .date(request.date())
            .weight(request.weight())
            .diagnosis(request.diagnosis().trim())
            .evolution(request.evolution())
            .notes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim())
            .build();
    }
}
