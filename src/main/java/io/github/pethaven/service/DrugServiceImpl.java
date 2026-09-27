package io.github.pethaven.service;

import io.github.pethaven.entity.Drug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DrugServiceImpl implements DrugService {

    @Autowired
    private DrugRepository drugRepository;

    @Autowired
    private TreatmentDrugRepository treatmentDrugRepository;

    @Override
    public Drug getDrugById(Long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drug", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Drug> getAllDrugs() {
        return drugRepository.findAll();
    }

    @Override
    public Drug getDrugByName(String name) {
        return drugRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Drug", "name", name));
    }

    @Override
    public void createDrug(Drug drug) {
        drugRepository.save(drug);
    }

    // Una droga que ya fue administrada en algún tratamiento no se puede eliminar:
    // se perdería el detalle histórico (unidades y precios) de esos tratamientos.
    @Override
    @Transactional
    public void deleteDrugById(Long id) {
        Drug drug = getDrugById(id);
        if (!treatmentDrugRepository.findByDrugId(id).isEmpty()) {
            throw new IllegalOperationException(
                    "No se puede eliminar la droga '" + drug.getName()
                            + "' porque está asociada a uno o más tratamientos existentes.");
        }
        drugRepository.deleteById(id);
    }

}
