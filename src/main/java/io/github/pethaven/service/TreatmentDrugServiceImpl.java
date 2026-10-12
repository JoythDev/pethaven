package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.mapper.TreatmentDrugMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// NOTA: Un TreatmentDrug nunca se actualiza ni se elimina de forma independiente: solo se crea
// (al administrar un medicamento) y solo desaparece en cascada si su Treatment padre se elimina, cosa
// que tampoco ocurre nunca (ver TreatmentService). Es un renglón del historial médico permanente.

// Propuesta: Un TreatmentDrug solo se gestiona a través de su Treatment padre, y no de forma independiente.
@Service
public class TreatmentDrugServiceImpl implements TreatmentDrugService {

    private final TreatmentDrugRepository treatmentDrugRepository;
    private final TreatmentDrugMapper treatmentDrugMapper;
    private final TreatmentService treatmentService;
    private final DrugService drugService;
    private final DrugRepository drugRepository;

    @Autowired
    public TreatmentDrugServiceImpl(
            TreatmentDrugRepository treatmentDrugRepository,
            TreatmentDrugMapper treatmentDrugMapper,
            TreatmentService treatmentService,
            DrugService drugService,
            DrugRepository drugRepository
    ) {
        this.treatmentDrugRepository = treatmentDrugRepository;
        this.treatmentDrugMapper = treatmentDrugMapper;
        this.treatmentService = treatmentService;
        this.drugService = drugService;
        this.drugRepository = drugRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDrugResponse> getTreatmentDrugsByTreatmentId(Long treatmentId) {
        return treatmentDrugRepository.findByTreatmentId(treatmentId).stream()
                .map(treatmentDrugMapper::toResponse)
                .toList();
    }

    // Al agregar un medicamento a un tratamiento se descuenta su inventario disponible y se
    // copian sus precios actuales (el precio del medicamento puede cambiar después sin afectar
    // el historial ya registrado).
    @Override
    @Transactional
    public TreatmentDrugResponse createTreatmentDrug(Long treatmentId, TreatmentDrugRequest request) {
        // TODO: Necesario validar esto al usar TreatmentDrugRequest?
        if (request.units() == null || request.units() <= 0) {
            throw new IllegalOperationException("Las unidades administradas deben ser mayores a cero.");
        }

        Treatment treatment = treatmentService.getTreatmentEntity(treatmentId);
        Drug drug = drugService.getDrugEntity(request.drugId());

        if (treatmentDrugRepository.existsByTreatmentIdAndDrugId(treatmentId, request.drugId())) {
            throw new IllegalOperationException(
                    "La droga '" + drug.getName() + "' ya fue administrada en este tratamiento.");
        }

        if (drug.getUnitsAvailable() < request.units()) {
            throw new IllegalOperationException(
                    "No hay suficientes unidades disponibles de '" + drug.getName() + "'. Disponibles: "
                            + drug.getUnitsAvailable() + ", solicitadas: " + request.units() + ".");
        }

        drug.setUnitsAvailable(drug.getUnitsAvailable() - request.units());
        drug.setUnitsSold(drug.getUnitsSold() + request.units());
        drugRepository.save(drug);

        TreatmentDrug treatmentDrug = TreatmentDrug.builder()
                .treatment(treatment)
                .drug(drug)
                .units(request.units())
                .unitPurchasePrice(drug.getPurchasePrice())
                .unitSalePrice(drug.getSalePrice())
                .build();

        treatmentDrugRepository.save(treatmentDrug);
        treatment.getTreatmentDrugs().add(treatmentDrug);
        return treatmentDrugMapper.toResponse(treatmentDrug);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDrugResponse> getTreatmentDrugsByDrugId(Long drugId) {
        return treatmentDrugRepository.findByDrugId(drugId).stream()
                .map(treatmentDrugMapper::toResponse)
                .toList();
    }

}
