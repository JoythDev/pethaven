package io.github.pethaven.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public record OwnerUpdateRequest(
        @NotBlank(message = "El documento es obligatorio")
        String document,

        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe ser válido")
        String email,

        String password,

        @NotBlank(message = "El teléfono es obligatorio")
        String phone
) {
    public OwnerUpdateRequest(
        String document,
        String name,
        String email,
        String phone
    ) {
        this(document, name, email, null, phone);
    }

    public Optional<String> optionalPassword() {
        return Optional.ofNullable(password);
    }
}
