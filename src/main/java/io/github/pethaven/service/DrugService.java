package io.github.pethaven.service;

import io.github.pethaven.entity.Drug;
import java.util.List;

public interface DrugService {

    public Drug getDrugById(Long id);
    public List<Drug> getAllDrugs();
    public Drug getDrugByName(String name);
    public void createDrug(Drug drug);
    public void deleteDrugById(Long id);

}
