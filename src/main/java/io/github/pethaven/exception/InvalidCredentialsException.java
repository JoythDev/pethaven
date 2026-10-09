package io.github.pethaven.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException() {
        this("Correo o contraseña incorrectos.");
    }
}
