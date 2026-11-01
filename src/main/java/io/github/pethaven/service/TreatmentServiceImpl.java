package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Evolution;
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
        ensureCoherentWithCurrentState(pet, treatment);
        if (treatment.getDate() == null) {
            treatment.setDate(LocalDateTime.now());
        }
        treatment.setPet(pet);
        // La consulta define el estado actual de la mascota: su peso y su diagnóstico.
        pet.setWeight(treatment.getWeight());
        pet.setDisease(treatment.getDiagnosis());
        treatment.setVeterinarian(veterinarian);

        Treatment saved = treatmentRepository.save(treatment);
        treatmentDrugService.administerDrugs(saved, request.drugs());

        veterinarian.setAttentions(veterinarian.getAttentions() + 1);
        veterinarianRepository.save(veterinarian);

        return treatmentMapper.toResponse(saved);
    }

    // Alta: la mascota mejoró y ya no necesita medicamentos. Queda en su historia clínica como un
    // registro sin medicamentos (evolución RECOVERED, con el diagnóstico del que se recuperó) y
    // la mascota vuelve a estar estable. No suma atenciones: no es un tratamiento.
    @Override
    @Transactional
    public TreatmentResponse dischargePet(Long petId, Long veterinarianId) {
        Pet pet = petService.getPetEntity(petId);
        Veterinarian veterinarian = veterinarianService.getVeterinarianEntity(veterinarianId);
        if (pet.getDisease() == null) {
            throw new IllegalOperationException("La mascota '" + pet.getName() + "' ya está estable.");
        }
        if (!veterinarian.isActive()) {
            throw new IllegalOperationException(
                    "El veterinario '" + veterinarian.getName() + "' no está activo: no puede dar de alta.");
        }
        Treatment discharge = Treatment.builder()
                .pet(pet)
                .veterinarian(veterinarian)
                .date(LocalDateTime.now())
                .weight(pet.getWeight())
                .diagnosis(pet.getDisease())
                .evolution(Evolution.RECOVERED)
                .notes("Alta: la mascota queda estable.")
                .build();
        pet.setDisease(null);
        return treatmentMapper.toResponse(treatmentRepository.save(discharge));
    }

    // Si la mascota llega estable es un ingreso: no hay con qué comparar, así que no lleva evolución.
    // Si está en tratamiento la evolución es obligatoria, y "sin cambios" conserva su diagnóstico.
    private void ensureCoherentWithCurrentState(Pet pet, Treatment treatment) {
        String currentDisease = pet.getDisease();
        Evolution evolution = treatment.getEvolution();
        if (currentDisease == null && evolution != null) {
            throw new IllegalOperationException(
                    "La mascota '" + pet.getName() + "' está estable: es un ingreso y no lleva evolución.");
        }
        if (currentDisease != null && evolution == null) {
            throw new IllegalOperationException(
                    "La mascota '" + pet.getName() + "' está en tratamiento por '" + currentDisease + "': indica su evolución.");
        }
        if (evolution == Evolution.RECOVERED) {
            throw new IllegalOperationException(
                    "Un tratamiento siempre formula medicamentos: para dejar estable a la mascota dale el alta.");
        }
        if (evolution == Evolution.STABLE && !treatment.getDiagnosis().equals(currentDisease)) {
            throw new IllegalOperationException(
                    "Con evolución 'sin cambios' el diagnóstico debe seguir siendo '" + currentDisease + "'.");
        }
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
