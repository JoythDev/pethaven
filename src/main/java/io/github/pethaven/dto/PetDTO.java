package io.github.pethaven.dto;

import io.github.pethaven.entity.Species;

public record PetDTO (
        String name,
        Long ownerId,
        Species species,
        String breed,
        Integer age,
        Double weight,
        String disease,
        String photoUrl,
        Boolean isActive
) {}
