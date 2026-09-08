package io.github.pethaven.service;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetServiceImpl implements PetService {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerService ownerService;

    @Override
    public Pet getPetById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pet> getAllPets() {
        return petRepository.findAll();
    }

    @Override
    @Transactional
    public void createPet(Pet pet, Long ownerId) {
        Owner owner = ownerService.getOwnerById(ownerId);
        pet.setOwner(owner);
        petRepository.save(pet);
    }

    @Override
    @Transactional
    public void switchPetActiveStatus(Long id, boolean active) {
        Pet pet = getPetById(id);
        pet.setActive(active);
        petRepository.save(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pet> getPetsByOwnerId(Long ownerId) {
        return petRepository.findByOwnerId(ownerId);
    }

}
