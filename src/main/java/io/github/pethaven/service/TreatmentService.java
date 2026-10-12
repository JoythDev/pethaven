package io.github.pethaven.service;

import io.github.pethaven.entity.Treatment;
import java.util.List;

public interface TreatmentService {

    Treatment getTreatmentById(Long id);
    List<Treatment> getAllTreatments();
    List<Treatment> getTreatmentsByPetId(Long petId);
    List<Treatment> getTreatmentsByVeterinarianId(Long veterinarianId);
    void createTreatment(Treatment treatment, Long petId, Long veterinarianId);

}
