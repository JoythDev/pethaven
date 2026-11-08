package io.github.pethaven.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y lee los tokens de acceso JWT firmados con HMAC-SHA.
 * <p>
 * La clave se decodifica (Base64) y se valida una sola vez al crear el bean: una clave ausente,
 * mal codificada o de menos de 256 bits impide que la aplicación arranque.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtProvider(
            @Value("${security.jwt.secret-key}") String secretKey,
            @Value("${security.jwt.expiration-time}") long expirationMillis
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.expirationMillis = expirationMillis;
    }

    /** Vigencia de los tokens emitidos, en segundos. */
    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    public String generateToken(Authentication authentication) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifica firma y expiración del token y devuelve su usuario (subject).
     *
     * @return el username, o vacío si el token es nulo, está mal formado, no está firmado
     *         con la clave de la aplicación o ha expirado
     */
    public Optional<String> resolveUsername(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            String subject = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(subject);
        } catch (ExpiredJwtException e) {
            // Caso habitual (sesión vencida): no es un error del servidor.
            log.debug("Token JWT expirado: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            // Firma inválida, token mal formado, sin firmar o no soportado. Nunca se registra el token.
            log.warn("Token JWT rechazado ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return Optional.empty();
    }
}
