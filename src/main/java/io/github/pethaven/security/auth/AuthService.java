package io.github.pethaven.security.auth;

import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.github.pethaven.security.auth.dto.OtpChallengeResponse;
import io.github.pethaven.security.auth.dto.ResendOtpRequest;
import io.github.pethaven.security.auth.dto.VerifyOtpRequest;

public interface AuthService {

    /**
     * Primer paso: valida las credenciales y envía un código de verificación al correo de la cuenta. No emite
     * el token de acceso: ese lo entrega {@link #verifyOtp(VerifyOtpRequest)}.
     *
     * @throws org.springframework.security.core.AuthenticationException si las credenciales son
     *         incorrectas (BadCredentialsException) o la cuenta está desactivada (DisabledException)
     * @throws io.github.pethaven.exception.OtpException con 503 si el correo no se pudo entregar
     */
    OtpChallengeResponse login(LoginRequest request);

    /**
     * Segundo paso: verifica el código recibido por correo y emite el token de acceso.
     *
     * @throws io.github.pethaven.exception.OtpException si el código es incorrecto, venció o se agotaron los intentos
     * @throws org.springframework.security.core.AuthenticationException si la cuenta se desactivó o eliminó entre los dos pasos
     */
    AuthResponse verifyOtp(VerifyOtpRequest request);

    /** Envía un código nuevo para un desafío pendiente. */
    OtpChallengeResponse resendOtp(ResendOtpRequest request);

    /** Perfil (id, nombre, correo y rol) de la cuenta con el username indicado. */
    AuthProfileResponse getProfile(String username);
}
