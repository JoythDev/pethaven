package io.github.pethaven.support;

import io.github.pethaven.exception.OtpException;
import io.github.pethaven.security.otp.OtpMailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sustitutos de las dependencias externas del código de un solo uso para las pruebas de integración: un envío de
 * correo que guarda el último código por destinatario (en vez de enviarlo) y un reloj que se mueve a mano.
 * Se activa con {@code @Import(OtpTestSupport.class)}.
 */
@TestConfiguration
public class OtpTestSupport {

    @Bean
    @Primary
    public RecordingOtpMailSender recordingOtpMailSender() {
        return new RecordingOtpMailSender();
    }

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
    }

    /** Guarda el último código enviado a cada correo; {@link #failNext()} simula un fallo del proveedor. */
    public static class RecordingOtpMailSender implements OtpMailSender {

        private final Map<String, String> lastCodes = new ConcurrentHashMap<>();
        private volatile boolean failNext;

        @Override
        public void send(String to, String code, Duration validity) {
            if (failNext) {
                failNext = false;
                throw OtpException.deliveryFailed();
            }
            lastCodes.put(to, code);
        }

        /** Último código enviado al correo; falla si no se envió ninguno. */
        public String lastCodeFor(String email) {
            String code = lastCodes.get(email);
            if (code == null) {
                throw new IllegalStateException("No se envió ningún código a " + email);
            }
            return code;
        }

        public boolean hasSentTo(String email) {
            return lastCodes.containsKey(email);
        }

        /** Hace que el próximo envío falle como si el proveedor de correo no respondiera. */
        public void failNext() {
            this.failNext = true;
        }

        public void clear() {
            lastCodes.clear();
            failNext = false;
        }
    }

    /** Reloj con un instante que se avanza a mano. */
    public static class MutableClock extends Clock {

        private volatile Instant now;

        public MutableClock(Instant now) {
            this.now = now;
        }

        public void advance(Duration duration) {
            now = now.plus(duration);
        }

        public void set(Instant instant) {
            now = instant;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
