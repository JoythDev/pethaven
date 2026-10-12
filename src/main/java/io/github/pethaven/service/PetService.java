package io.github.pethaven.service;

import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.Pet;

import java.util.List;

public interface PetService {

    Pet getPetEntity(Long id);
    List<PetResponse> getAllPets();
    List<PetResponse> searchPets(String name);
    PetResponse getPetById(Long id);
    PetResponse createPet(PetRequest request);
    PetResponse updatePet(Long id, PetRequest request);
    PetResponse switchPetActiveStatus(Long id, boolean active);
    List<PetResponse> getPetsByOwnerId(Long ownerId);
}
