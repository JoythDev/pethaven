package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Treatment;
import java.util.List;

public interface TreatmentService {

    Treatment getTreatmentEntity(Long id);
    List<TreatmentResponse> getAllTreatments();
    TreatmentResponse getTreatmentById(Long id);
    TreatmentResponse createTreatment(TreatmentRequest request);
    TreatmentResponse dischargePet(Long petId, Long veterinarianId);

    List<TreatmentResponse> getTreatmentsByPetId(Long petId);
    List<TreatmentResponse> getTreatmentsByVeterinarianId(Long veterinarianId);

}
