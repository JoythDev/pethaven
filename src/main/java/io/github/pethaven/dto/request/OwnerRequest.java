package io.github.pethaven.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerRequest(
        @NotBlank(message = "El documento es obligatorio")
        String document,

        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe ser válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El teléfono es obligatorio")
        String phone
) { }
