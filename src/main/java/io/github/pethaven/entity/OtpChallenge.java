package io.github.pethaven.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Desafío de código de un solo uso (OTP) de un inicio de sesión en curso. Solo guarda el hash del
 * código, nunca el código. Se elimina al verificarse, al vencer o al agotar los intentos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    /** UUID aleatorio que identifica el desafío; es lo único que el cliente conoce. */
    @Id
    @Column(length = 36)
    private String id;

    /** Username (correo) de la cuenta que superó la contraseña. */
    @Column(nullable = false, length = 120)
    private String username;

    /** HMAC-SHA256 en hexadecimal del código ligado al id del desafío. */
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    /** Intentos de verificación fallidos con el código vigente. */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Veces que se reenvió el código en este desafío. */
    @Column(nullable = false)
    private int resends;
}
