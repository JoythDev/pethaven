package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de un nuevo código para un desafío pendiente.
 *
 * @param challengeId identificador del desafío devuelto por el inicio de sesión
 */
@Schema(description = "Reenvío del código de verificación")
public record ResendOtpRequest(
        @Schema(description = "Identificador del desafío", example = "3f1c6a52-8d0b-4b77-9a2e-5d0f6c1e7b19")
        @NotBlank(message = "El identificador del desafío es obligatorio")
        @Size(max = 36, message = "El identificador del desafío no es válido")
        String challengeId
) { }
