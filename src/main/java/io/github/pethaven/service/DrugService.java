package io.github.pethaven.service;

import io.github.pethaven.entity.Drug;
import java.util.List;

public interface DrugService {

    Drug getDrugById(Long id);
    List<Drug> getAllDrugs();
    Drug getDrugByName(String name);
    void createDrug(Drug drug);
    void deleteDrugById(Long id);

}
