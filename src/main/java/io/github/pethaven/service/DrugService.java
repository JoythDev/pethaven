package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.entity.Drug;
import java.util.List;

public interface DrugService {

    Drug getDrugEntity(Long id);
    List<DrugResponse> getAllDrugs();
    DrugResponse getDrugById(Long id);
    DrugResponse getDrugByName(String name);
    DrugResponse createDrug(DrugRequest request);
    DrugResponse updateDrug(Long id, DrugRequest request);
    void deleteDrugById(Long id);

}
