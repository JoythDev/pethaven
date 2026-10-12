package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.entity.Veterinarian;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VeterinarianMapper {

    public VeterinarianResponse toResponse(Veterinarian veterinarian) {
        if (veterinarian == null) return null;
        return new VeterinarianResponse(
            veterinarian.getId(),
            veterinarian.getDocument(),
            veterinarian.getName(),
            veterinarian.getEmail(),
            veterinarian.getSpecialty(),
            veterinarian.getPhotoUrl(),
            veterinarian.getAttentions(),
            veterinarian.isActive()
        );
    }

    public Veterinarian toEntity(VeterinarianRequest request) {
        if (request == null) return null;
        return Veterinarian.builder()
            .document(request.document())
            .name(request.name())
            .email(request.email())
            .specialty(request.specialty())
            .photoUrl(request.photoUrl())
            .attentions(0)
            .active(true)
            .build();
    }

    /** Copia los campos editables sobre la entidad; el estado se gestiona en el service. */
    public void applyUpdate(VeterinarianUpdateRequest request, Veterinarian veterinarian) {
        veterinarian.setDocument(request.document());
        veterinarian.setName(request.name());
        veterinarian.setEmail(request.email());
        veterinarian.setSpecialty(request.specialty());
        veterinarian.setPhotoUrl(request.photoUrl());
        if (request.password() != null) {
            veterinarian.setPassword(request.password());
        }
    }
}
