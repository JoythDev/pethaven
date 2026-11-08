package io.github.pethaven.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.WeakKeyException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

// Emisión y lectura de tokens JWT sin contexto de Spring: la clave y la expiración se pasan al constructor.
public class JwtProviderTest {

    private static final long TWO_HOURS = 7_200_000L;
    private static final String SECRET = Encoders.BASE64.encode(Jwts.SIG.HS512.key().build().getEncoded());
    private static final String OTHER_SECRET = Encoders.BASE64.encode(Jwts.SIG.HS512.key().build().getEncoded());

    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken("john.doe@example.com", null, List.of());

    @Test
    public void jwtProvider_resolveUsername_TokenItIssued_ReturnsSubject() {
        // Arrange
        JwtProvider provider = new JwtProvider(SECRET, TWO_HOURS);
        String token = provider.generateToken(authentication);

        // Act + Assert
        Assertions.assertThat(provider.resolveUsername(token)).contains("john.doe@example.com");
    }

    @Test
    public void jwtProvider_resolveUsername_ExpiredToken_ReturnsEmpty() {
        // Arrange
        // Expiración negativa: el token nace ya vencido.
        String token = new JwtProvider(SECRET, -1_000L).generateToken(authentication);

        // Act + Assert
        Assertions.assertThat(new JwtProvider(SECRET, TWO_HOURS).resolveUsername(token)).isEmpty();
    }

    @Test
    public void jwtProvider_resolveUsername_TokenSignedWithAnotherKey_ReturnsEmpty() {
        // Arrange
        String foreignToken = new JwtProvider(OTHER_SECRET, TWO_HOURS).generateToken(authentication);

        // Act + Assert
        Assertions.assertThat(new JwtProvider(SECRET, TWO_HOURS).resolveUsername(foreignToken)).isEmpty();
    }

    @Test
    public void jwtProvider_resolveUsername_MalformedToken_ReturnsEmpty() {
        // Act + Assert
        Assertions.assertThat(new JwtProvider(SECRET, TWO_HOURS).resolveUsername("no-es-un-jwt")).isEmpty();
    }

    @Test
    public void jwtProvider_resolveUsername_UnsignedToken_ReturnsEmpty() {
        // Arrange
        // Token sin firma ("alg": "none"): nunca debe aceptarse.
        String unsigned = Jwts.builder().subject("john.doe@example.com").compact();

        // Act + Assert
        Assertions.assertThat(new JwtProvider(SECRET, TWO_HOURS).resolveUsername(unsigned)).isEmpty();
    }

    @Test
    public void jwtProvider_resolveUsername_NullOrBlankToken_ReturnsEmpty() {
        // Arrange
        JwtProvider provider = new JwtProvider(SECRET, TWO_HOURS);

        // Act + Assert
        Assertions.assertThat(provider.resolveUsername(null)).isEmpty();
        Assertions.assertThat(provider.resolveUsername("   ")).isEmpty();
    }

    @Test
    public void jwtProvider_constructor_WeakSecret_FailsFast() {
        // Arrange
        // 16 bytes (128 bits): por debajo del mínimo de 256 bits para HMAC-SHA.
        String weakSecret = Encoders.BASE64.encode(new byte[16]);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> new JwtProvider(weakSecret, TWO_HOURS))
                .isInstanceOf(WeakKeyException.class);
    }
}
