package io.github.pethaven.service;

import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// NOTE: Un TreatmentDrug nunca se actualiza ni se elimina de forma independiente: solo se crea
// (al administrar una droga) y solo desaparece en cascada si su Treatment padre se elimina, cosa
// que tampoco ocurre nunca (ver TreatmentService). Es un renglón del historial médico permanente.
@Service
public class TreatmentDrugServiceImpl implements TreatmentDrugService {

    @Autowired
    private TreatmentDrugRepository treatmentDrugRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private DrugRepository drugRepository;

    @Override
    public TreatmentDrug getTreatmentDrugById(Long id) {
        return treatmentDrugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TreatmentDrug", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDrug> getAllTreatmentDrugs() {
        return treatmentDrugRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDrug> getTreatmentDrugsByTreatmentId(Long treatmentId) {
        return treatmentDrugRepository.findByTreatmentId(treatmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDrug> getTreatmentDrugsByDrugId(Long drugId) {
        return treatmentDrugRepository.findByDrugId(drugId);
    }

    // Al agregar una droga a un tratamiento se descuenta su inventario disponible y se
    // copian sus precios actuales (el precio de la droga puede cambiar después sin afectar
    // el historial ya registrado).
    @Override
    @Transactional
    public void createTreatmentDrug(Long treatmentId, Long drugId, Integer units) {
        if (units == null || units <= 0) {
            throw new IllegalOperationException("Las unidades administradas deben ser mayores a cero.");
        }

        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", treatmentId));
        Drug drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new ResourceNotFoundException("Drug", "id", drugId));

        if (drug.getUnitsAvailable() < units) {
            throw new IllegalOperationException(
                    "No hay suficientes unidades disponibles de '" + drug.getName() + "'. Disponibles: "
                            + drug.getUnitsAvailable() + ", solicitadas: " + units + ".");
        }

        drug.setUnitsAvailable(drug.getUnitsAvailable() - units);
        drug.setUnitsSold(drug.getUnitsSold() + units);
        drugRepository.save(drug);

        TreatmentDrug treatmentDrug = TreatmentDrug.builder()
                .treatment(treatment)
                .drug(drug)
                .units(units)
                .unitPurchasePrice(drug.getPurchasePrice())
                .unitSalePrice(drug.getSalePrice())
                .build();

        treatmentDrugRepository.save(treatmentDrug);
        treatment.getTreatmentDrugs().add(treatmentDrug);
    }

}
