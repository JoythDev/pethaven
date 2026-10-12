package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetMapper {

    public PetResponse toResponse(Pet pet) {
        if (pet == null) return null;
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getAge(),
            pet.getWeight(),
            pet.getDisease(),
            pet.getPhotoUrl(),
            pet.isActive(),
            pet.getOwner().getId(),
            pet.getOwner().getName()
        );
    }

    public Pet toEntity(PetRequest request) {
        if (request == null) return null;
        return Pet.builder()
            .name(request.name())
            .species(request.species())
            .breed(request.breed())
            .age(request.age())
            .weight(request.weight())
            .disease(request.disease())
            .photoUrl(request.photoUrl())
            .active(true)
            .build();
    }

    /** Copia los campos editables sobre la entidad; el dueño y el estado se gestionan en el service. */
    public void applyUpdate(PetRequest request, Pet pet) {
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setBreed(request.breed());
        pet.setAge(request.age());
        pet.setWeight(request.weight());
        pet.setDisease(request.disease());
        pet.setPhotoUrl(request.photoUrl());
    }
}
