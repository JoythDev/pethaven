package io.github.pethaven.service;

import io.github.pethaven.entity.TreatmentDrug;
import java.util.List;

public interface TreatmentDrugService {

    TreatmentDrug getTreatmentDrugById(Long id);
    List<TreatmentDrug> getAllTreatmentDrugs();
    List<TreatmentDrug> getTreatmentDrugsByTreatmentId(Long treatmentId);
    List<TreatmentDrug> getTreatmentDrugsByDrugId(Long drugId);
    void createTreatmentDrug(Long treatmentId, Long drugId, Integer units);

}
