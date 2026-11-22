package io.github.pethaven.security.otp;

import com.sun.net.httpserver.HttpServer;
import io.github.pethaven.exception.OtpException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

// Generador de códigos, enmascarado del correo y envío por la API HTTP de Brevo (contra un servidor local).
public class OtpMailAndCodeTest {

    private HttpServer server;
    private final AtomicReference<String> receivedApiKey = new AtomicReference<>();
    private final AtomicReference<String> receivedPath = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private volatile int responseStatus = 201;

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedApiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    // ---------- SecureOtpCodeGenerator ----------

    @Test
    public void secureOtpCodeGenerator_Generate_ReturnsSixDigitsIncludingLeadingZeros() {
        // Arrange
        SecureOtpCodeGenerator generator = new SecureOtpCodeGenerator();

        // Act + Assert
        for (int i = 0; i < 2_000; i++) {
            Assertions.assertThat(generator.generate()).matches("\\d{6}");
        }
    }

    // ---------- OtpServiceImpl.mask ----------

    @Test
    public void mask_Email_KeepsTheFirstLetterAndTheDomain() {
        Assertions.assertThat(OtpServiceImpl.mask("juan.perez@gmail.com")).isEqualTo("j***@gmail.com");
    }

    @Test
    public void mask_ValueWithoutAtSign_HidesEverything() {
        Assertions.assertThat(OtpServiceImpl.mask("sin-arroba")).isEqualTo("***");
        Assertions.assertThat(OtpServiceImpl.mask("@dominio.com")).isEqualTo("***");
    }

    // ---------- LogOtpMailSender ----------

    @Test
    public void logOtpMailSender_Send_DoesNotThrow() {
        Assertions.assertThatCode(() -> new LogOtpMailSender().send("a@b.test", "123456", Duration.ofMinutes(5)))
                .doesNotThrowAnyException();
    }

    // ---------- BrevoOtpMailSender ----------

    @Test
    public void brevoOtpMailSender_Send_PostsTheMessageWithTheApiKeyAndRecipient() {
        // Arrange
        BrevoOtpMailSender sender = brevo("clave-secreta", "no-reply@pethaven.test");

        // Act
        sender.send("juan@gmail.com", "482915", Duration.ofMinutes(5));

        // Assert
        Assertions.assertThat(receivedPath.get()).isEqualTo("/v3/smtp/email");
        Assertions.assertThat(receivedApiKey.get()).isEqualTo("clave-secreta");
        Assertions.assertThat(receivedBody.get())
                .contains("\"juan@gmail.com\"")
                .contains("no-reply@pethaven.test")
                .contains("482915")
                .contains("5 minutos");
    }

    @Test
    public void brevoOtpMailSender_ProviderAnswersWithAnError_ThrowsDeliveryFailed() {
        // Arrange
        responseStatus = 401;
        BrevoOtpMailSender sender = brevo("clave-mala", "no-reply@pethaven.test");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> sender.send("juan@gmail.com", "482915", Duration.ofMinutes(5)))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    public void brevoOtpMailSender_ProviderUnreachable_ThrowsDeliveryFailed() {
        // Arrange
        server.stop(0);
        BrevoOtpMailSender sender = brevo("clave", "no-reply@pethaven.test");

        // Act + Assert
        Assertions.assertThatThrownBy(() -> sender.send("juan@gmail.com", "482915", Duration.ofMinutes(5)))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    public void brevoOtpMailSender_MissingApiKeyOrSender_FailsAtStartup() {
        Assertions.assertThatThrownBy(() -> brevo("", "no-reply@pethaven.test"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("BREVO_API_KEY");
        Assertions.assertThatThrownBy(() -> brevo("clave", ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("BREVO_SENDER_EMAIL");
    }

    private BrevoOtpMailSender brevo(String apiKey, String senderEmail) {
        return new BrevoOtpMailSender(apiKey, senderEmail, "PetHaven",
                "http://127.0.0.1:" + server.getAddress().getPort(), 3);
    }
}
