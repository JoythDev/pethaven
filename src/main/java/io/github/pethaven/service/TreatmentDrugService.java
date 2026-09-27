package io.github.pethaven.service;

import io.github.pethaven.entity.TreatmentDrug;
import java.util.List;

public interface TreatmentDrugService {

    public TreatmentDrug getTreatmentDrugById(Long id);
    public List<TreatmentDrug> getAllTreatmentDrugs();
    public List<TreatmentDrug> getTreatmentDrugsByTreatmentId(Long treatmentId);
    public List<TreatmentDrug> getTreatmentDrugsByDrugId(Long drugId);
    public void createTreatmentDrug(Long treatmentId, Long drugId, Integer units);

}
