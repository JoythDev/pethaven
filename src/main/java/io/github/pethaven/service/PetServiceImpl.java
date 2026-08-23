package io.github.pethaven.service;

import io.github.pethaven.entity.Pet;
import io.github.pethaven.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetServiceImpl implements PetService {

    @Autowired
    private PetRepository petRepository;

    @Override
    public Pet getPetById(Long id) {
        return petRepository.findById(id);
    }

    @Override
    public List<Pet> getAllPets(String search, String status) {
        return petRepository.findAll(search, status);
    }

    @Override
    public void createPet(Pet pet) {
        petRepository.save(pet);
    }

    @Override
    public void deletePetById(Long id) {
        petRepository.deleteById(id);
    }

}
