package io.github.pethaven.security.otp;

import io.github.pethaven.exception.OtpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envía el código por la API HTTP transaccional de Brevo (los planes gratuitos de Render bloquean el SMTP).
 * La clave de API y el remitente verificado salen de la configuración; sin ellos la aplicación no arranca.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.otp.mail.provider", havingValue = "brevo")
public class BrevoOtpMailSender implements OtpMailSender {

    private final RestClient client;
    private final String senderEmail;
    private final String senderName;

    public BrevoOtpMailSender(
            @Value("${app.otp.brevo.api-key:}") String apiKey,
            @Value("${app.otp.brevo.sender-email:}") String senderEmail,
            @Value("${app.otp.brevo.sender-name:PetHaven}") String senderName,
            @Value("${app.otp.brevo.base-url:https://api.brevo.com}") String baseUrl,
            @Value("${app.otp.brevo.timeout-seconds:10}") long timeoutSeconds
    ) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "Con app.otp.mail.provider=brevo se requieren BREVO_API_KEY y BREVO_SENDER_EMAIL.");
        }
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("api-key", apiKey)
                .requestFactory(requestFactory)
                .build();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    public void send(String to, String code, Duration validity) {
        long minutes = validity.toMinutes();
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", "Tu código de verificación de PetHaven",
                "textContent", "Tu código de verificación es " + code + ". Vence en " + minutes
                        + " minutos. Si no iniciaste sesión, ignora este mensaje.",
                "htmlContent", html(code, minutes));
        try {
            client.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // No se registra el código ni el destinatario completo.
            log.error("Brevo no pudo entregar el código de verificación ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            throw OtpException.deliveryFailed();
        }
    }

    private static String html(String code, long minutes) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:420px;margin:auto;padding:24px;color:#261a10\">"
                + "<h2 style=\"margin:0 0 12px\">Tu código de verificación</h2>"
                + "<p>Usa este código para terminar de iniciar sesión en PetHaven:</p>"
                + "<p style=\"font-size:32px;letter-spacing:8px;font-weight:bold;margin:16px 0\">" + code + "</p>"
                + "<p>Vence en " + minutes + " minutos. Si no iniciaste sesión, ignora este mensaje.</p>"
                + "</div>";
    }
}
