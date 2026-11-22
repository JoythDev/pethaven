package io.github.pethaven.security.otp;

import io.github.pethaven.security.auth.dto.OtpChallengeResponse;

/**
 * Segundo factor del inicio de sesión: código de un solo uso enviado por correo.
 * <p>
 * Reglas: el código tiene 6 dígitos, vence a los pocos minutos, se usa una sola vez, admite un número
 * limitado de intentos fallidos y de reenvíos, y solo se guarda su hash.
 */
public interface OtpService {

    /**
     * Crea un desafío para la cuenta (descarta los anteriores de esa cuenta) y envía el código a su correo.
     *
     * @throws io.github.pethaven.exception.OtpException con 503 si el correo no se pudo entregar
     */
    OtpChallengeResponse start(String username);

    /**
     * Verifica el código y consume el desafío.
     *
     * @return el username de la cuenta que completó el segundo paso
     * @throws io.github.pethaven.exception.OtpException con 401 si el código es incorrecto o venció,
     *         y con 429 si se agotaron los intentos
     */
    String verify(String challengeId, String code);

    /**
     * Genera y envía un código nuevo para el mismo desafío (reinicia los intentos y la vigencia).
     *
     * @throws io.github.pethaven.exception.OtpException con 401 si el desafío venció, con 429 si no terminó
     *         la espera o se agotaron los reenvíos, y con 503 si el correo no se pudo entregar
     */
    OtpChallengeResponse resend(String challengeId);
}
