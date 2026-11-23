package io.github.pethaven.security.otp;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Código de 6 dígitos (con ceros a la izquierda) tomado de un generador criptográficamente seguro. */
@Component
public class SecureOtpCodeGenerator implements OtpCodeGenerator {

    public static final int CODE_LENGTH = 6;

    private static final int BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%0" + CODE_LENGTH + "d", random.nextInt(BOUND));
    }
}
