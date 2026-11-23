package io.github.pethaven.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado del primer paso del inicio de sesión: la contraseña es correcta y se envió un código al correo.
 *
 * @param challengeId       identificador del desafío; se envía junto con el código en {@code verify-otp}
 * @param maskedEmail       correo al que se envió el código, parcialmente oculto
 * @param expiresIn         segundos de vigencia del código desde su envío
 * @param resendAvailableIn segundos que hay que esperar antes de pedir otro código
 */
@Schema(description = "Desafío de verificación en dos pasos pendiente de completar")
public record OtpChallengeResponse(
        @Schema(description = "Identificador del desafío", example = "3f1c6a52-8d0b-4b77-9a2e-5d0f6c1e7b19")
        String challengeId,

        @Schema(description = "Correo al que se envió el código, parcialmente oculto", example = "j***@gmail.com")
        String maskedEmail,

        @Schema(description = "Segundos de vigencia del código", example = "300")
        long expiresIn,

        @Schema(description = "Segundos que hay que esperar antes de reenviar el código", example = "60")
        long resendAvailableIn
) { }
