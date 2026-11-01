package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.entity.Treatment;
import java.util.List;

public interface TreatmentDrugService {

    List<TreatmentDrugResponse> getTreatmentDrugsByTreatmentId(Long treatmentId);
    TreatmentDrugResponse createTreatmentDrug(Long treatmentId, TreatmentDrugRequest request);
    void administerDrugs(Treatment treatment, List<TreatmentDrugRequest> requests);

}
