package io.github.pethaven.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TreatmentResponse(
        Long id,
        LocalDateTime date,
        Long petId,
        String petName,
        Long veterinarianId,
        String veterinarianName,
        List<TreatmentDrugResponse> drugs
) { }
