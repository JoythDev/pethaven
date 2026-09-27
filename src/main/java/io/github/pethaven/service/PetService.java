package io.github.pethaven.service;

import io.github.pethaven.entity.Pet;

import java.util.List;

public interface PetService {

    public Pet getPetById(Long id);
    public List<Pet> getAllPets();
    public void createPet(Pet pet, Long ownerId);
    public void switchPetActiveStatus(Long id, boolean active);
    public List<Pet> getPetsByOwnerId(Long ownerId);

}
