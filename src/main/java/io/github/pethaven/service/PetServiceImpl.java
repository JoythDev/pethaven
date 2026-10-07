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
    public Pet createPet(Pet pet, Long ownerId) {
        Owner owner = ownerService.getOwnerById(ownerId);
        pet.setOwner(owner);
        return petRepository.save(pet);
    }

    @Override
    @Transactional
    public Pet updatePet(Long id, Pet updatedPet) {
        Pet existingPet = getPetById(id);
        existingPet.setName(updatedPet.getName());
        existingPet.setSpecies(updatedPet.getSpecies());
        existingPet.setBreed(updatedPet.getBreed());
        existingPet.setAge(updatedPet.getAge());
        existingPet.setWeight(updatedPet.getWeight());
        existingPet.setDisease(updatedPet.getDisease());
        existingPet.setPhotoUrl(updatedPet.getPhotoUrl());
        return petRepository.save(existingPet);
    }

    // NOTE: Una mascota no se puede eliminar directamente, solo se puede desactivar.
    // Para eliminar una mascota, se debe eliminar su dueño (eliminación en cascada)

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
