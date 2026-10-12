package io.github.pethaven.service;

import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.PetMapper;
import io.github.pethaven.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetMapper petMapper;
    private final OwnerService ownerService;

    @Autowired
    public PetServiceImpl(
            PetRepository petRepository,
            PetMapper petMapper,
            OwnerService ownerService
    ) {
        this.petRepository = petRepository;
        this.petMapper = petMapper;
        this.ownerService = ownerService;
    }

    @Override
    public Pet getPetEntity(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getAllPets() {
        return petRepository.findAll().stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> searchPets(String name) {
        return petRepository.findByNameContainingIgnoreCase(name).stream()
                .map(petMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPetById(Long id) {
        return petMapper.toResponse(getPetEntity(id));
    }

    @Override
    @Transactional
    public PetResponse createPet(PetRequest request) {
        Pet pet = petMapper.toEntity(request);
        Owner owner = ownerService.getOwnerEntity(request.ownerId());
        pet.setOwner(owner);
        Pet saved = petRepository.save(pet);
        return petMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PetResponse updatePet(Long id, PetRequest updatedPet) {
        Pet existing = getPetEntity(id);
        Owner owner = ownerService.getOwnerEntity(updatedPet.ownerId());
        petMapper.applyUpdate(updatedPet, existing);
        existing.setOwner(owner);
        Pet updated = petRepository.save(existing);
        return petMapper.toResponse(updated);
    }

    // NOTE: Una mascota no se puede eliminar directamente, solo se puede desactivar.
    // Para eliminar una mascota, se debe eliminar su dueño (eliminación en cascada)
    @Override
    @Transactional
    public PetResponse switchPetActiveStatus(Long id, boolean active) {
        Pet pet = getPetEntity(id);
        pet.setActive(active);
        petRepository.save(pet);
        return petMapper.toResponse(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getPetsByOwnerId(Long ownerId) {
        return petRepository.findByOwnerId(ownerId).stream()
                .map(petMapper::toResponse)
                .toList();
    }
}
