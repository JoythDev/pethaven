package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.TreatmentDrugMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// NOTA: Un TreatmentDrug nunca se actualiza ni se elimina de forma independiente: solo se crea
// (al administrar un medicamento) y solo desaparece en cascada si su Treatment padre se elimina, cosa
// que tampoco ocurre nunca (ver TreatmentService). Es un renglón del historial médico permanente.
// Un TreatmentDrug solo se gestiona a través de su Treatment padre, y no de forma independiente.
@Service
public class TreatmentDrugServiceImpl implements TreatmentDrugService {

    private final TreatmentDrugRepository treatmentDrugRepository;
    private final TreatmentDrugMapper treatmentDrugMapper;
    private final TreatmentRepository treatmentRepository;
    private final DrugService drugService;
    private final DrugRepository drugRepository;

    @Autowired
    public TreatmentDrugServiceImpl(
            TreatmentDrugRepository treatmentDrugRepository,
            TreatmentDrugMapper treatmentDrugMapper,
            TreatmentRepository treatmentRepository,
            DrugService drugService,
            DrugRepository drugRepository
    ) {
        this.treatmentDrugRepository = treatmentDrugRepository;
        this.treatmentDrugMapper = treatmentDrugMapper;
        this.treatmentRepository = treatmentRepository;
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
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", treatmentId));
        return treatmentDrugMapper.toResponse(administer(treatment, request));
    }

    // Administra varios medicamentos dentro de la transacción del llamador: si alguno falla
    // (sin stock, repetido, etc.) la excepción hace rollback también del tratamiento que los contiene.
    @Override
    @Transactional
    public void administerDrugs(Treatment treatment, List<TreatmentDrugRequest> requests) {
        if (requests == null) {
            return;
        }
        requests.forEach(request -> administer(treatment, request));
    }

    private TreatmentDrug administer(Treatment treatment, TreatmentDrugRequest request) {
        if (request.units() == null || request.units() <= 0) {
            throw new IllegalOperationException("Las unidades administradas deben ser mayores a cero.");
        }

        ensurePetIsActive(treatment);
        Drug drug = loadAvailableDrugFor(treatment.getId(), request);

        registerStockMovement(drug, request.units());

        TreatmentDrug treatmentDrug = saveTreatmentDrug(treatment, drug, request.units());
        treatment.getTreatmentDrugs().add(treatmentDrug);
        return treatmentDrug;
    }

    private void ensurePetIsActive(Treatment treatment) {
        if (!treatment.getPet().isActive()) {
            throw new IllegalOperationException(
                    "La mascota '" + treatment.getPet().getName()
                            + "' no está activa: no se le pueden administrar medicamentos.");
        }
    }

    private Drug loadAvailableDrugFor(Long treatmentId, TreatmentDrugRequest request) {
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

        return drug;
    }

    private void registerStockMovement(Drug drug, int units) {
        drug.setUnitsAvailable(drug.getUnitsAvailable() - units);
        drug.setUnitsSold(drug.getUnitsSold() + units);
        drugRepository.save(drug);
    }

    private TreatmentDrug saveTreatmentDrug(Treatment treatment, Drug drug, int units) {
        return treatmentDrugRepository.save(TreatmentDrug.builder()
                .treatment(treatment)
                .drug(drug)
                .units(units)
                .unitPurchasePrice(drug.getPurchasePrice())
                .unitSalePrice(drug.getSalePrice())
                .build());
    }

}
