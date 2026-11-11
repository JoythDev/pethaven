package io.github.pethaven.security.auth;

import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;

public interface AuthService {

    /**
     * Autentica las credenciales y emite un token de acceso.
     *
     * @throws org.springframework.security.core.AuthenticationException si las credenciales son
     *         incorrectas (BadCredentialsException) o la cuenta está desactivada (DisabledException)
     */
    AuthResponse login(LoginRequest request);

    /** Perfil (id, nombre, correo y rol) de la cuenta con el username indicado. */
    AuthProfileResponse getProfile(String username);
}
