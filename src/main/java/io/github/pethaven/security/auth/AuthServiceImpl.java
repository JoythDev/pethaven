package io.github.pethaven.security.auth;

import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.UserRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.github.pethaven.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final AdministratorRepository administratorRepository;
    private final JwtProvider jwtProvider;

    @Autowired
    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            OwnerRepository ownerRepository,
            VeterinarianRepository veterinarianRepository,
            AdministratorRepository administratorRepository,
            JwtProvider jwtProvider
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.administratorRepository = administratorRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        AuthProfileResponse profile = getProfile(authentication.getName());
        String token = jwtProvider.generateToken(authentication);

        return new AuthResponse(token, jwtProvider.getExpirationSeconds(),
                profile.profileId(), profile.name(), profile.email(), profile.role());
    }

    // Sin @Transactional a propósito: login() lo invoca desde la propia clase (sin pasar por el proxy), y
    // ninguna lectura lo necesita: los roles se cargan EAGER y cada consulta del repositorio es transaccional.
    @Override
    public AuthProfileResponse getProfile(String username) {
        UserEntity account = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("No existe la cuenta autenticada '" + username + "'."));
        String role = singleRole(account);

        Optional<AuthProfileResponse> profile = switch (role) {
            case Role.OWNER -> ownerRepository.findByUserUsername(username)
                    .map(owner -> new AuthProfileResponse(owner.getId(), owner.getName(), owner.getEmail(), role));
            case Role.VETERINARIAN -> veterinarianRepository.findByUserUsername(username)
                    .map(vet -> new AuthProfileResponse(vet.getId(), vet.getName(), vet.getEmail(), role));
            case Role.ADMIN -> administratorRepository.findByUserUsername(username)
                    .map(admin -> new AuthProfileResponse(admin.getId(), admin.getName(), admin.getEmail(), role));
            default -> throw new IllegalStateException("Rol no soportado: '" + role + "'.");
        };
        return profile.orElseThrow(() -> new IllegalStateException(
                "La cuenta '" + username + "' no tiene un perfil de " + role + " asociado."));
    }

    /** La API expone un único rol por usuario; el modelo admite varios para crecer en el futuro. */
    private static String singleRole(UserEntity account) {
        List<Role> roles = account.getRoles();
        if (roles == null || roles.size() != 1) {
            throw new IllegalStateException("La cuenta '" + account.getUsername()
                    + "' debe tener exactamente un rol (tiene " + (roles == null ? 0 : roles.size()) + ").");
        }
        return roles.getFirst().getName();
    }
}
