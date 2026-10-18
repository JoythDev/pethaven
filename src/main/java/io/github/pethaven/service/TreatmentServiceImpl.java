package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.TreatmentMapper;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper treatmentMapper;
    private final VeterinarianRepository veterinarianRepository;
    private final PetService petService;
    private final VeterinarianService veterinarianService;
    private final DrugRepository drugRepository;
    private final TreatmentDrugService treatmentDrugService;

    @Autowired
    public TreatmentServiceImpl(
            TreatmentRepository treatmentRepository,
            TreatmentMapper treatmentMapper,
            VeterinarianRepository veterinarianRepository,
            PetService petService,
            VeterinarianService veterinarianService,
            DrugRepository drugRepository,
            TreatmentDrugService treatmentDrugService
    ) {
        this.treatmentRepository = treatmentRepository;
        this.treatmentMapper = treatmentMapper;
        this.veterinarianRepository = veterinarianRepository;
        this.petService = petService;
        this.veterinarianService = veterinarianService;
        this.drugRepository = drugRepository;
        this.treatmentDrugService = treatmentDrugService;
    }

    @Override
    public Treatment getTreatmentEntity(Long id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentResponse> getAllTreatments() {
        return treatmentRepository.findAll().stream()
                .map(treatmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TreatmentResponse getTreatmentById(Long id) {
        return treatmentMapper.toResponse(getTreatmentEntity(id));
    }

    // NOTA: Un tratamiento nunca se actualiza ni se elimina, solo se crea y se consulta:
    // es un registro médico que debe permanecer intacto aunque la mascota deje de existir.

    @Override
    @Transactional
    public TreatmentResponse createTreatment(TreatmentRequest request) {
        Pet pet = petService.getPetEntity(request.petId());
        Veterinarian veterinarian = veterinarianService.getVeterinarianEntity(request.veterinarianId());

        if (!pet.isActive()) {
            throw new IllegalOperationException(
                    "La mascota '" + pet.getName() + "' no está activa: no se le pueden registrar tratamientos.");
        }
        if (!veterinarian.isActive()) {
            throw new IllegalOperationException(
                    "El veterinario '" + veterinarian.getName() + "' no está activo: no puede registrar atenciones.");
        }
        // Un tratamiento se apoya en el inventario de medicamentos: sin ninguno con stock no se puede registrar.
        if (!drugRepository.existsByUnitsAvailableGreaterThan(0)) {
            throw new IllegalOperationException(
                    "No hay medicamentos disponibles en el inventario: no se pueden registrar tratamientos.");
        }

        Treatment treatment = treatmentMapper.toEntity(request);
        if (treatment.getDate() == null) {
            treatment.setDate(LocalDateTime.now());
        }
        treatment.setPet(pet);
        treatment.setVeterinarian(veterinarian);

        Treatment saved = treatmentRepository.save(treatment);
        treatmentDrugService.administerDrugs(saved, request.drugs());

        veterinarian.setAttentions(veterinarian.getAttentions() + 1);
        veterinarianRepository.save(veterinarian);

        return treatmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentResponse> getTreatmentsByPetId(Long petId) {
        return treatmentRepository.findByPetId(petId).stream()
                .map(treatmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentResponse> getTreatmentsByVeterinarianId(Long veterinarianId) {
        return treatmentRepository.findByVeterinarianId(veterinarianId).stream()
                .map(treatmentMapper::toResponse)
                .toList();
    }
}
