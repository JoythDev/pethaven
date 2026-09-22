package io.github.pethaven.service;

import io.github.pethaven.entity.Treatment;
import java.util.List;

public interface TreatmentService {

    public Treatment getTreatmentById(Long id);
    public List<Treatment> getAllTreatments();
    public List<Treatment> getTreatmentsByPetId(Long petId);
    public List<Treatment> getTreatmentsByVeterinarianId(Long veterinarianId);
    public void createTreatment(Treatment treatment, Long petId, Long veterinarianId);

}
