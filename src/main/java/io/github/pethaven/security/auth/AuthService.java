package io.github.pethaven.security.auth;

import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
