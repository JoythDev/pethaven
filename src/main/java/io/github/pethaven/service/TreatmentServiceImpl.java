package io.github.pethaven.service;

import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private PetService petService;

    @Autowired
    private VeterinarianService veterinarianService;

    @Override
    public Treatment getTreatmentById(Long id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Treatment> getAllTreatments() {
        return treatmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Treatment> getTreatmentsByPetId(Long petId) {
        return treatmentRepository.findByPetId(petId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Treatment> getTreatmentsByVeterinarianId(Long veterinarianId) {
        return treatmentRepository.findByVeterinarianId(veterinarianId);
    }

    // NOTA: Un tratamiento nunca se actualiza ni se elimina, solo se crea y se consulta:
    // es un registro médico que debe permanecer intacto aunque la mascota deje de existir.
    @Override
    @Transactional
    public void createTreatment(Treatment treatment, Long petId, Long veterinarianId) {
        Pet pet = petService.getPetById(petId);
        Veterinarian veterinarian = veterinarianService.getVeterinarianById(veterinarianId);

        treatment.setPet(pet);
        treatment.setVeterinarian(veterinarian);
        treatmentRepository.save(treatment);

        veterinarian.setAttentions(veterinarian.getAttentions() + 1);
        veterinarianRepository.save(veterinarian);
    }

}
