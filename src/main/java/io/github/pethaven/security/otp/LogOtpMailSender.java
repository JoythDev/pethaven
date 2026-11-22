package io.github.pethaven.security.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Solo para desarrollo y pruebas: escribe el código en el log en vez de enviar un correo, así las cuentas
 * de ejemplo (con correos ficticios) pueden iniciar sesión. Está activo por defecto; el perfil de
 * producción selecciona {@code brevo} y no lo usa.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.otp.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogOtpMailSender implements OtpMailSender {

    @Override
    public void send(String to, String code, Duration validity) {
        log.warn("[OTP de desarrollo] código para {}: {} (vigente {} min)", to, code, validity.toMinutes());
    }
}
