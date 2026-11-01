package io.github.pethaven.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException() {
        this("La cuenta está desactivada.");
    }
}
