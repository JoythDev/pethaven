package io.github.pethaven.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VeterinarianUpdateRequest(
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

        @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
        String password,

        @NotBlank(message = "La especialidad es obligatoria")
        @Size(max = 80, message = "La especialidad no puede superar los 80 caracteres")
        String specialty,

        @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
        String photoUrl
) {
    /** Normaliza la contraseña: null o en blanco se convierten en null ("conservar la actual"). */
    public VeterinarianUpdateRequest {
        password = (password == null || password.isBlank()) ? null : password;
    }
}
