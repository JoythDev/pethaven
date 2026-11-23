package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Segundo paso del inicio de sesión: el código recibido por correo.
 *
 * @param challengeId identificador del desafío devuelto por el inicio de sesión
 * @param code        código de 6 dígitos
 */
@Schema(description = "Código de verificación recibido por correo")
public record VerifyOtpRequest(
        @Schema(description = "Identificador del desafío", example = "3f1c6a52-8d0b-4b77-9a2e-5d0f6c1e7b19")
        @NotBlank(message = "El identificador del desafío es obligatorio")
        @Size(max = 36, message = "El identificador del desafío no es válido")
        String challengeId,

        @Schema(description = "Código de 6 dígitos", example = "482915")
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos")
        String code
) { }
