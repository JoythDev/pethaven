package io.github.pethaven.exception;

import org.springframework.http.HttpStatus;

/**
 * Fallo en el segundo paso del inicio de sesión (código de un solo uso): código incorrecto o vencido,
 * demasiados intentos o reenvíos, o error al entregar el correo. Lleva el estado HTTP con el que se responde.
 */
public class OtpException extends RuntimeException {

    private final HttpStatus status;

    private OtpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** El desafío no existe, venció o ya se usó: hay que iniciar sesión de nuevo. */
    public static OtpException expired() {
        return new OtpException(HttpStatus.UNAUTHORIZED,
                "El código venció o ya no es válido. Inicia sesión de nuevo.");
    }

    /** El código no coincide; quedan intentos. */
    public static OtpException wrongCode(int remainingAttempts) {
        return new OtpException(HttpStatus.UNAUTHORIZED,
                "El código es incorrecto. Te quedan " + remainingAttempts
                        + (remainingAttempts == 1 ? " intento." : " intentos."));
    }

    /** Se agotaron los intentos de verificación: el desafío se invalida. */
    public static OtpException attemptsExhausted() {
        return new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                "Superaste el número de intentos. Inicia sesión de nuevo para recibir otro código.");
    }

    /** Se pidió un reenvío antes de que termine la espera. */
    public static OtpException resendTooSoon(long secondsToWait) {
        return new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                "Espera " + secondsToWait + " s antes de pedir otro código.");
    }

    /** Se alcanzó el máximo de reenvíos del desafío. */
    public static OtpException resendsExhausted() {
        return new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                "Alcanzaste el máximo de reenvíos. Inicia sesión de nuevo.");
    }

    /** El proveedor de correo no pudo entregar el mensaje. */
    public static OtpException deliveryFailed() {
        return new OtpException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo enviar el código por correo. Intenta de nuevo en unos minutos.");
    }
}
