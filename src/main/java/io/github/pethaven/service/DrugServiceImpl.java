package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.DrugMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DrugServiceImpl implements DrugService {

    private final DrugRepository drugRepository;
    private final DrugMapper drugMapper;
    private final TreatmentDrugRepository treatmentDrugRepository;

    @Autowired
    public DrugServiceImpl(
            DrugRepository drugRepository,
            DrugMapper drugMapper,
            TreatmentDrugRepository treatmentDrugRepository
    ) {
        this.drugRepository = drugRepository;
        this.drugMapper = drugMapper;
        this.treatmentDrugRepository = treatmentDrugRepository;
    }

    @Override
    public Drug getDrugEntity(Long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drug", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DrugResponse> getAllDrugs() {
        return drugRepository.findAll().stream()
                .map(drugMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DrugResponse> getAvailableDrugs() {
        return drugRepository.findByUnitsAvailableGreaterThan(0).stream()
                .map(drugMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DrugResponse getDrugById(Long id) {
        return drugMapper.toResponse(getDrugEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DrugResponse getDrugByName(String name) {
        Drug drug = drugRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Drug", "name", name));
        return drugMapper.toResponse(drug);
    }

    @Override
    @Transactional
    public DrugResponse createDrug(DrugRequest request) {
        ensureUnique(request.name(), null);
        Drug drug = drugMapper.toEntity(request);
        Drug saved = drugRepository.save(drug);
        return drugMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DrugResponse updateDrug(Long id, DrugRequest request) {
        Drug existing = getDrugEntity(id);
        ensureUnique(request.name(), id);
        drugMapper.applyUpdate(request, existing);
        Drug updated = drugRepository.save(existing);
        return drugMapper.toResponse(updated);
    }

    private void ensureUnique(String name, Long selfId) {
        boolean nameTaken = selfId == null
                ? drugRepository.existsByName(name)
                : drugRepository.existsByNameAndIdNot(name, selfId);
        if (nameTaken) {
            throw new IllegalOperationException("Ya existe un medicamento con el nombre '" + name + "'.");
        }
    }

    // Una droga que ya fue administrada en algún tratamiento no se puede eliminar:
    // se perdería el detalle histórico (unidades y precios) de esos tratamientos.
    @Override
    @Transactional
    public void deleteDrugById(Long id) {
        Drug drug = getDrugEntity(id);

        if (treatmentDrugRepository.existsByDrugId(id)) {
            throw new IllegalOperationException(
                    "No se puede eliminar el medicamento '" + drug.getName()
                            + "' porque está asociado a uno o más tratamientos existentes.");
        }

        drugRepository.deleteById(id);
    }

}
