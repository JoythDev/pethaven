package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.TreatmentMapper;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper treatmentMapper;
    private final VeterinarianRepository veterinarianRepository;
    private final PetService petService;
    private final VeterinarianService veterinarianService;

    @Autowired
    public TreatmentServiceImpl(
            TreatmentRepository treatmentRepository,
            TreatmentMapper treatmentMapper,
            VeterinarianRepository veterinarianRepository,
            PetService petService,
            VeterinarianService veterinarianService
    ) {
        this.treatmentRepository = treatmentRepository;
        this.treatmentMapper = treatmentMapper;
        this.veterinarianRepository = veterinarianRepository;
        this.petService = petService;
        this.veterinarianService = veterinarianService;
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

        Treatment treatment = treatmentMapper.toEntity(request);
        treatment.setPet(pet);
        treatment.setVeterinarian(veterinarian);

        Treatment saved = treatmentRepository.save(treatment);

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
