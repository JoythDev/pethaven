package io.github.pethaven.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerUpdateRequest(
        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar los 20 caracteres")
        String document,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe ser válido")
        @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
        String email,

        // Opcional: null o en blanco significa "conservar la contraseña actual".
        @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
        String password,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone
) {
    public OwnerUpdateRequest {
        password = (password == null || password.isBlank()) ? null : password;
    }
}
