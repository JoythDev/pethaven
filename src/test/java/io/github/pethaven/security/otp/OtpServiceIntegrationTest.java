package io.github.pethaven.security.otp;

import io.github.pethaven.entity.OtpChallenge;
import io.github.pethaven.exception.OtpException;
import io.github.pethaven.repository.OtpChallengeRepository;
import io.github.pethaven.security.auth.dto.OtpChallengeResponse;
import io.github.pethaven.support.OtpTestSupport;
import io.github.pethaven.support.OtpTestSupport.MutableClock;
import io.github.pethaven.support.OtpTestSupport.RecordingOtpMailSender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

// Reglas del código de un solo uso contra la base real (H2), con el envío de correo y el reloj sustituidos.
@SpringBootTest
@ActiveProfiles("test")
@Import(OtpTestSupport.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:otp-service-test;DB_CLOSE_DELAY=-1",
        "app.otp.ttl-seconds=300",
        "app.otp.max-attempts=3",
        "app.otp.resend-cooldown-seconds=60",
        "app.otp.max-resends=2"
})
public class OtpServiceIntegrationTest {

    private static final String EMAIL = "otp.service@pethaven.test";

    @Autowired
    private OtpService otpService;
    @Autowired
    private OtpChallengeRepository repository;
    @Autowired
    private RecordingOtpMailSender mailSender;
    @Autowired
    private MutableClock clock;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
        mailSender.clear();
    }

    // ---------- start ----------

    @Test
    public void start_ValidAccount_SendsCodeAndStoresOnlyItsHash() {
        // Act
        OtpChallengeResponse response = otpService.start(EMAIL);

        // Assert
        String code = mailSender.lastCodeFor(EMAIL);
        Assertions.assertThat(code).matches("\\d{6}");
        Assertions.assertThat(response.maskedEmail()).isEqualTo("o***@pethaven.test");
        Assertions.assertThat(response.expiresIn()).isEqualTo(300);
        Assertions.assertThat(response.resendAvailableIn()).isEqualTo(60);

        OtpChallenge stored = repository.findById(response.challengeId()).orElseThrow();
        Assertions.assertThat(stored.getUsername()).isEqualTo(EMAIL);
        Assertions.assertThat(stored.getCodeHash()).hasSize(64).doesNotContain(code);
    }

    @Test
    public void start_SecondLoginOfTheSameAccount_InvalidatesThePreviousChallenge() {
        // Arrange
        OtpChallengeResponse first = otpService.start(EMAIL);
        String firstCode = mailSender.lastCodeFor(EMAIL);

        // Act
        otpService.start(EMAIL);

        // Assert
        Assertions.assertThat(repository.count()).isEqualTo(1);
        Assertions.assertThatThrownBy(() -> otpService.verify(first.challengeId(), firstCode))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void start_MailDeliveryFails_Throws503AndLeavesNoChallenge() {
        // Arrange
        mailSender.failNext();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> otpService.start(EMAIL))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Assertions.assertThat(repository.count()).isZero();
    }

    // ---------- verify ----------

    @Test
    public void verify_CorrectCode_ReturnsTheUsernameAndConsumesTheChallenge() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        String code = mailSender.lastCodeFor(EMAIL);

        // Act
        String username = otpService.verify(challenge.challengeId(), code);

        // Assert
        Assertions.assertThat(username).isEqualTo(EMAIL);
        Assertions.assertThat(repository.count()).isZero();
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), code))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("venció");
    }

    @Test
    public void verify_WrongCode_ReportsRemainingAttemptsAndKeepsCountingBetweenCalls() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        String wrong = otherCodeThan(mailSender.lastCodeFor(EMAIL));

        // Act + Assert
        // Si el intento fallido se revirtiera con la excepción, el contador nunca avanzaría.
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), wrong))
                .isInstanceOf(OtpException.class).hasMessageContaining("Te quedan 2 intentos");
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), wrong))
                .isInstanceOf(OtpException.class).hasMessageContaining("Te quedan 1 intento.");
        Assertions.assertThat(repository.findById(challenge.challengeId()).orElseThrow().getFailedAttempts()).isEqualTo(2);
    }

    @Test
    public void verify_AttemptsExhausted_Returns429AndInvalidatesEvenTheCorrectCode() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        String code = mailSender.lastCodeFor(EMAIL);
        String wrong = otherCodeThan(code);

        // Act
        for (int i = 0; i < 2; i++) {
            Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), wrong))
                    .isInstanceOf(OtpException.class);
        }

        // Assert
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), wrong))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        Assertions.assertThat(repository.existsById(challenge.challengeId())).isFalse();
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), code))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void verify_AfterTheCodeExpired_Returns401AndRemovesTheChallenge() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        String code = mailSender.lastCodeFor(EMAIL);
        clock.advance(Duration.ofSeconds(300));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), code))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(repository.existsById(challenge.challengeId())).isFalse();
    }

    @Test
    public void verify_UnknownChallenge_Returns401() {
        Assertions.assertThatThrownBy(() -> otpService.verify("no-existe", "123456"))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- resend ----------

    @Test
    public void resend_BeforeTheCooldown_Returns429WithTheSecondsToWait() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        clock.advance(Duration.ofSeconds(20));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> otpService.resend(challenge.challengeId()))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("Espera 40 s")
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    public void resend_AfterTheCooldown_SendsANewCodeAndResetsAttemptsAndExpiry() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        String oldCode = mailSender.lastCodeFor(EMAIL);
        Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), otherCodeThan(oldCode)))
                .isInstanceOf(OtpException.class);
        clock.advance(Duration.ofSeconds(61));

        // Act
        OtpChallengeResponse resent = otpService.resend(challenge.challengeId());

        // Assert
        Assertions.assertThat(resent.challengeId()).isEqualTo(challenge.challengeId());
        OtpChallenge stored = repository.findById(challenge.challengeId()).orElseThrow();
        Assertions.assertThat(stored.getFailedAttempts()).isZero();
        Assertions.assertThat(stored.getResends()).isEqualTo(1);
        Assertions.assertThat(stored.getExpiresAt()).isEqualTo(clock.instant().plusSeconds(300));

        String newCode = mailSender.lastCodeFor(EMAIL);
        if (!newCode.equals(oldCode)) {
            Assertions.assertThatThrownBy(() -> otpService.verify(challenge.challengeId(), oldCode))
                    .isInstanceOf(OtpException.class).hasMessageContaining("incorrecto");
        }
        Assertions.assertThat(otpService.verify(challenge.challengeId(), newCode)).isEqualTo(EMAIL);
    }

    @Test
    public void resend_MaxResendsReached_Returns429() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        for (int i = 0; i < 2; i++) {
            clock.advance(Duration.ofSeconds(61));
            otpService.resend(challenge.challengeId());
        }
        clock.advance(Duration.ofSeconds(61));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> otpService.resend(challenge.challengeId()))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("máximo de reenvíos")
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    public void resend_MailDeliveryFails_Returns503AndTheOldCodeStillWorks() {
        // Arrange
        OtpChallengeResponse challenge = otpService.start(EMAIL);
        String oldCode = mailSender.lastCodeFor(EMAIL);
        clock.advance(Duration.ofSeconds(61));
        mailSender.failNext();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> otpService.resend(challenge.challengeId()))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        // La transacción se revierte: el reenvío fallido no consume un reenvío ni cambia el código.
        Assertions.assertThat(repository.findById(challenge.challengeId()).orElseThrow().getResends()).isZero();
        Assertions.assertThat(otpService.verify(challenge.challengeId(), oldCode)).isEqualTo(EMAIL);
    }

    @Test
    public void resend_UnknownChallenge_Returns401() {
        Assertions.assertThatThrownBy(() -> otpService.resend("no-existe"))
                .isInstanceOf(OtpException.class)
                .extracting(e -> ((OtpException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static String otherCodeThan(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }
}
