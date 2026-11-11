package io.github.pethaven.security.auth;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.UserRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.github.pethaven.security.jwt.JwtProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Login y perfil autenticado: el rol sale de la cuenta y los datos del perfil (id, nombre) de su entidad.
public class AuthServiceImplTest {

    private final AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final OwnerRepository ownerRepository = Mockito.mock(OwnerRepository.class);
    private final VeterinarianRepository veterinarianRepository = Mockito.mock(VeterinarianRepository.class);
    private final AdministratorRepository administratorRepository = Mockito.mock(AdministratorRepository.class);
    private final JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);

    private final AuthServiceImpl authService = new AuthServiceImpl(
            authenticationManager, userRepository, ownerRepository, veterinarianRepository,
            administratorRepository, jwtProvider);

    // ---------- login ----------

    @Test
    public void authService_login_ValidOwnerCredentials_ReturnsTokenExpiryAndOwnerProfile() {
        // Arrange
        Authentication authenticated = givenAuthenticated("john.doe@example.com", "password123");
        givenAccount("john.doe@example.com", Role.OWNER);
        Owner owner = Owner.builder().id(7L).name("John Doe").email("john.doe@example.com").build();
        Mockito.when(ownerRepository.findByUserUsername("john.doe@example.com")).thenReturn(Optional.of(owner));
        Mockito.when(jwtProvider.generateToken(authenticated)).thenReturn("jwt-token");
        Mockito.when(jwtProvider.getExpirationSeconds()).thenReturn(7200L);

        // Act
        AuthResponse response = authService.login(new LoginRequest("john.doe@example.com", "password123"));

        // Assert
        Assertions.assertThat(response).isEqualTo(
                new AuthResponse("jwt-token", 7200L, 7L, "John Doe", "john.doe@example.com", Role.OWNER));
        // El AuthenticationManager recibe exactamente el email y la contraseña de la petición.
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        Mockito.verify(authenticationManager).authenticate(captor.capture());
        Assertions.assertThat(captor.getValue().getName()).isEqualTo("john.doe@example.com");
        Assertions.assertThat(captor.getValue().getCredentials()).isEqualTo("password123");
        Assertions.assertThat(captor.getValue().isAuthenticated()).isFalse();
    }

    @Test
    public void authService_login_ValidVeterinarianCredentials_ReturnsVeterinarianProfile() {
        // Arrange
        givenAuthenticated("valentina.soler@pethaven.com", "vet12345");
        givenAccount("valentina.soler@pethaven.com", Role.VETERINARIAN);
        Veterinarian veterinarian = Veterinarian.builder()
                .id(3L).name("Dra. Valentina Soler").email("valentina.soler@pethaven.com").build();
        Mockito.when(veterinarianRepository.findByUserUsername("valentina.soler@pethaven.com"))
                .thenReturn(Optional.of(veterinarian));
        Mockito.when(jwtProvider.generateToken(ArgumentMatchers.any())).thenReturn("jwt-token");
        Mockito.when(jwtProvider.getExpirationSeconds()).thenReturn(7200L);

        // Act
        AuthResponse response = authService.login(new LoginRequest("valentina.soler@pethaven.com", "vet12345"));

        // Assert
        Assertions.assertThat(response.profileId()).isEqualTo(3L);
        Assertions.assertThat(response.name()).isEqualTo("Dra. Valentina Soler");
        Assertions.assertThat(response.role()).isEqualTo(Role.VETERINARIAN);
    }

    @Test
    public void authService_login_ValidAdministratorCredentials_ReturnsAdministratorProfile() {
        // Arrange
        givenAuthenticated("admin@pethaven.com", "admin123");
        givenAccount("admin@pethaven.com", Role.ADMIN);
        Administrator administrator = Administrator.builder().id(1L).name("Admin").email("admin@pethaven.com").build();
        Mockito.when(administratorRepository.findByUserUsername("admin@pethaven.com"))
                .thenReturn(Optional.of(administrator));
        Mockito.when(jwtProvider.generateToken(ArgumentMatchers.any())).thenReturn("jwt-token");
        Mockito.when(jwtProvider.getExpirationSeconds()).thenReturn(7200L);

        // Act
        AuthResponse response = authService.login(new LoginRequest("admin@pethaven.com", "admin123"));

        // Assert
        Assertions.assertThat(response.profileId()).isEqualTo(1L);
        Assertions.assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    public void authService_login_InvalidCredentials_PropagatesAndIssuesNoToken() {
        // Arrange
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> authService.login(new LoginRequest("john.doe@example.com", "mala")))
                .isInstanceOf(BadCredentialsException.class);
        Mockito.verifyNoInteractions(jwtProvider, userRepository, ownerRepository);
    }

    // ---------- getProfile ----------

    @Test
    public void authService_getProfile_OwnerAccount_ReturnsProfileWithoutToken() {
        // Arrange
        givenAccount("john.doe@example.com", Role.OWNER);
        Owner owner = Owner.builder().id(7L).name("John Doe").email("john.doe@example.com").build();
        Mockito.when(ownerRepository.findByUserUsername("john.doe@example.com")).thenReturn(Optional.of(owner));

        // Act
        AuthProfileResponse profile = authService.getProfile("john.doe@example.com");

        // Assert
        Assertions.assertThat(profile).isEqualTo(
                new AuthProfileResponse(7L, "John Doe", "john.doe@example.com", Role.OWNER));
        Mockito.verifyNoInteractions(jwtProvider, authenticationManager);
    }

    @Test
    public void authService_getProfile_AccountWithoutProfile_ThrowsIllegalState() {
        // Arrange
        givenAccount("orphan@pethaven.com", Role.OWNER);
        Mockito.when(ownerRepository.findByUserUsername("orphan@pethaven.com")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> authService.getProfile("orphan@pethaven.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void authService_getProfile_AccountWithSeveralRoles_ThrowsIllegalState() {
        // Arrange
        // La API expone un único rol: una cuenta con varios es un estado no soportado.
        UserEntity account = new UserEntity();
        account.setUsername("multi@pethaven.com");
        account.setRoles(new ArrayList<>(List.of(new Role(Role.OWNER), new Role(Role.ADMIN))));
        Mockito.when(userRepository.findByUsername("multi@pethaven.com")).thenReturn(Optional.of(account));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> authService.getProfile("multi@pethaven.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void authService_getProfile_UnknownAccount_ThrowsIllegalState() {
        // Arrange
        Mockito.when(userRepository.findByUsername("ghost@pethaven.com")).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> authService.getProfile("ghost@pethaven.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    private Authentication givenAuthenticated(String email, String password) {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_X"), new SimpleGrantedAuthority("FACTOR_PASSWORD")));
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any())).thenReturn(authenticated);
        return authenticated;
    }

    private void givenAccount(String username, String roleName) {
        UserEntity account = new UserEntity();
        account.setUsername(username);
        account.setRoles(new ArrayList<>(List.of(new Role(roleName))));
        Mockito.when(userRepository.findByUsername(username)).thenReturn(Optional.of(account));
    }
}
