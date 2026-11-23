package io.github.pethaven.notification;

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

/** Envía los avisos por la API HTTP transaccional de Brevo, con la misma cuenta y remitente que el código de inicio de sesión. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.otp.mail.provider", havingValue = "brevo")
public class BrevoEmailSender implements EmailSender {

    private final RestClient client;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailSender(
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
    public void send(EmailMessage message) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", message.to())),
                "subject", message.subject(),
                "textContent", message.text(),
                "htmlContent", message.html());
        try {
            client.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IllegalStateException("Brevo no pudo entregar el aviso: " + e.getClass().getSimpleName(), e);
        }
    }
}
