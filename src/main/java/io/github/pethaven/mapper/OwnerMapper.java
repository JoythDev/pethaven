package io.github.pethaven.mapper;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.entity.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnerMapper {

    public OwnerResponse toResponse(Owner owner) {
        if (owner == null) return null;
        return new OwnerResponse(
            owner.getId(),
            owner.getDocument(),
            owner.getName(),
            owner.getEmail(),
            owner.getPhone()
        );
    }

    public Owner toEntity(OwnerRequest request) {
        if (request == null) return null;
        return Owner.builder()
            .document(request.document())
            .name(request.name())
            .email(request.email())
            .phone(request.phone())
            .build();
    }

    /** Copia los campos de perfil; las credenciales se gestionan en UserAccountService. */
    public void applyUpdate(OwnerUpdateRequest request, Owner owner) {
        owner.setDocument(request.document());
        owner.setName(request.name());
        owner.setEmail(request.email());
        owner.setPhone(request.phone());
    }
}
