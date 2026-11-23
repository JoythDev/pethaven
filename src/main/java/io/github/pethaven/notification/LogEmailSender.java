package io.github.pethaven.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Solo para desarrollo y pruebas: escribe el aviso en el log en vez de enviarlo (las cuentas de ejemplo son ficticias). */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.otp.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.info("[Correo de desarrollo] para {}: {}", message.to(), message.subject());
    }
}
