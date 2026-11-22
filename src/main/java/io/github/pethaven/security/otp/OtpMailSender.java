package io.github.pethaven.security.otp;

import java.time.Duration;

/** Entrega el código de un solo uso al correo del usuario. */
public interface OtpMailSender {

    /**
     * Envía el código.
     *
     * @param to       correo del destinatario
     * @param code     código en claro (solo viaja en este mensaje; nunca se guarda así)
     * @param validity tiempo durante el que el código es válido
     * @throws io.github.pethaven.exception.OtpException si el correo no se pudo entregar
     */
    void send(String to, String code, Duration validity);
}
