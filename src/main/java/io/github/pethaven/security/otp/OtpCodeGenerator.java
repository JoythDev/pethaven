package io.github.pethaven.security.otp;

/** Genera el código numérico de un solo uso. Es una interfaz para poder fijar el valor en las pruebas. */
public interface OtpCodeGenerator {

    String generate();
}
