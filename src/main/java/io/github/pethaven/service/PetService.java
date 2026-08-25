package io.github.pethaven.service;

import io.github.pethaven.entity.Pet;

import java.util.List;

public interface PetService {

    public Pet getPetById(Long id);

    public List<Pet> getAllPets(String search, String status);

    public void createPet(Pet pet);

    public void deletePetById(Long id);

}
