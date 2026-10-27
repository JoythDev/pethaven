package io.github.pethaven.mapper;

import io.github.pethaven.dto.response.AuthResponse;
import io.github.pethaven.dto.response.AuthRole;
import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Veterinarian;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthResponse toResponse(Owner owner) {
        if (owner == null) return null;
        return new AuthResponse(
            owner.getId(),
            owner.getName(),
            owner.getEmail(),
            AuthRole.OWNER
        );
    }

    public AuthResponse toResponse(Veterinarian veterinarian) {
        if (veterinarian == null) return null;
        return new AuthResponse(
            veterinarian.getId(),
            veterinarian.getName(),
            veterinarian.getEmail(),
            AuthRole.VETERINARIAN
        );
    }

    public AuthResponse toResponse(Administrator administrator) {
        if (administrator == null) return null;
        return new AuthResponse(
            administrator.getId(),
            administrator.getName(),
            administrator.getEmail(),
            AuthRole.ADMIN
        );
    }
}
