package io.github.pethaven.security.auth;

import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.UserRepository;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.github.pethaven.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Autowired
    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtProvider jwtProvider
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.username()));

        String token = jwtProvider.generateToken(authentication);

        return new AuthResponse(token, user.getUsername(), user.getRoles().stream().map(Role::getName).toList());
    }
}
