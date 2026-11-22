package io.github.pethaven.security.otp;

import io.github.pethaven.entity.OtpChallenge;
import io.github.pethaven.exception.OtpException;
import io.github.pethaven.repository.OtpChallengeRepository;
import io.github.pethaven.security.auth.dto.OtpChallengeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OtpServiceImpl implements OtpService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OtpChallengeRepository repository;
    private final OtpCodeGenerator codeGenerator;
    private final OtpMailSender mailSender;
    private final Clock clock;
    private final byte[] hashKey;
    private final Duration ttl;
    private final Duration resendCooldown;
    private final int maxAttempts;
    private final int maxResends;

    @Autowired
    public OtpServiceImpl(
            OtpChallengeRepository repository,
            OtpCodeGenerator codeGenerator,
            OtpMailSender mailSender,
            Clock clock,
            @Value("${security.jwt.secret-key}") String hashSecret,
            @Value("${app.otp.ttl-seconds:300}") long ttlSeconds,
            @Value("${app.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${app.otp.max-attempts:5}") int maxAttempts,
            @Value("${app.otp.max-resends:3}") int maxResends
    ) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.mailSender = mailSender;
        this.clock = clock;
        // La clave del hash se deriva del secreto de la aplicación: no hay otra variable que configurar.
        this.hashKey = ("otp:" + hashSecret).getBytes(StandardCharsets.UTF_8);
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.maxAttempts = maxAttempts;
        this.maxResends = maxResends;
    }

    // Sin @Transactional: el correo sale por HTTP y no debe sostener una transacción abierta. Si el envío
    // falla, el desafío recién guardado se elimina.
    @Override
    public OtpChallengeResponse start(String username) {
        Instant now = clock.instant();
        repository.deleteByExpiresAtBefore(now);
        repository.deleteByUsername(username);

        String code = codeGenerator.generate();
        OtpChallenge challenge = new OtpChallenge(
                UUID.randomUUID().toString(), username, "", now.plus(ttl), now, 0, 0);
        challenge.setCodeHash(hash(challenge.getId(), code));
        repository.save(challenge);

        try {
            mailSender.send(username, code, ttl);
        } catch (RuntimeException e) {
            repository.deleteById(challenge.getId());
            throw e;
        }
        return toResponse(challenge);
    }

    // Los intentos fallidos se guardan aunque se lance la excepción: sin noRollbackFor se revertirían.
    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public String verify(String challengeId, String code) {
        OtpChallenge challenge = loadActive(challengeId);

        boolean matches = MessageDigest.isEqual(
                hash(challenge.getId(), code).getBytes(StandardCharsets.UTF_8),
                challenge.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            if (challenge.getFailedAttempts() >= maxAttempts) {
                repository.delete(challenge);
                throw OtpException.attemptsExhausted();
            }
            repository.save(challenge);
            throw OtpException.wrongCode(maxAttempts - challenge.getFailedAttempts());
        }

        repository.delete(challenge);
        return challenge.getUsername();
    }

    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public OtpChallengeResponse resend(String challengeId) {
        OtpChallenge challenge = loadActive(challengeId);
        Instant now = clock.instant();

        if (challenge.getResends() >= maxResends) {
            throw OtpException.resendsExhausted();
        }
        Duration remaining = Duration.between(now, challenge.getLastSentAt().plus(resendCooldown));
        if (remaining.isPositive()) {
            // Se redondea hacia arriba: nunca se pide esperar menos de lo necesario.
            throw OtpException.resendTooSoon((remaining.toMillis() + 999) / 1000);
        }

        String code = codeGenerator.generate();
        challenge.setCodeHash(hash(challenge.getId(), code));
        challenge.setExpiresAt(now.plus(ttl));
        challenge.setLastSentAt(now);
        challenge.setFailedAttempts(0);
        challenge.setResends(challenge.getResends() + 1);
        repository.save(challenge);

        try {
            mailSender.send(challenge.getUsername(), code, ttl);
        } catch (OtpException e) {
            // noRollbackFor conserva los rechazos por límites, pero un envío fallido debe deshacer el cambio:
            // el código anterior sigue vigente y no se gasta un reenvío.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }
        return toResponse(challenge);
    }

    /** Desafío existente y no vencido, con bloqueo; uno vencido se elimina. */
    private OtpChallenge loadActive(String challengeId) {
        OtpChallenge challenge = repository.findWithLockById(challengeId).orElseThrow(OtpException::expired);
        if (!clock.instant().isBefore(challenge.getExpiresAt())) {
            repository.delete(challenge);
            throw OtpException.expired();
        }
        return challenge;
    }

    private OtpChallengeResponse toResponse(OtpChallenge challenge) {
        return new OtpChallengeResponse(challenge.getId(), mask(challenge.getUsername()),
                ttl.toSeconds(), resendCooldown.toSeconds());
    }

    /** HMAC-SHA256 hexadecimal del código, ligado al id del desafío. */
    private String hash(String challengeId, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hashKey, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash del código.", e);
        }
    }

    /** {@code juan@gmail.com} -> {@code j***@gmail.com}. */
    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
