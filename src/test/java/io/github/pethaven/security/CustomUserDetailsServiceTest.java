package io.github.pethaven.security;

import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Traduce la cuenta (UserEntity) al UserDetails de Spring Security: credenciales, roles y estado.
public class CustomUserDetailsServiceTest {

    private static final String EMAIL = "laura@pethaven.test";

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    public void customUserDetailsService_loadUserByUsername_ExistingAccount_MapsCredentialsAndPrefixedRole() {
        // Arrange
        Mockito.when(userRepository.findByUsername(EMAIL)).thenReturn(Optional.of(account(Role.VETERINARIAN, true)));

        // Act
        UserDetails details = service.loadUserByUsername(EMAIL);

        // Assert
        // Prefijo ROLE_: necesario para que hasRole("VETERINARIAN") reconozca la autoridad.
        Assertions.assertThat(details.getUsername()).isEqualTo(EMAIL);
        Assertions.assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
        Assertions.assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_VETERINARIAN");
        Assertions.assertThat(details.isEnabled()).isTrue();
    }

    @Test
    public void customUserDetailsService_loadUserByUsername_DisabledAccount_ReturnsDisabledUser() {
        // Arrange
        Mockito.when(userRepository.findByUsername(EMAIL)).thenReturn(Optional.of(account(Role.VETERINARIAN, false)));

        // Act
        UserDetails details = service.loadUserByUsername(EMAIL);

        // Assert
        Assertions.assertThat(details.isEnabled()).isFalse();
        Assertions.assertThat(details.isAccountNonLocked()).isTrue();
        Assertions.assertThat(details.isAccountNonExpired()).isTrue();
        Assertions.assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    public void customUserDetailsService_loadUserByUsername_UnknownUsername_ThrowsUsernameNotFound() {
        // Arrange
        Mockito.when(userRepository.findByUsername(EMAIL)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private static UserEntity account(String roleName, boolean enabled) {
        UserEntity account = new UserEntity();
        account.setUsername(EMAIL);
        account.setPassword("{bcrypt}hash");
        account.setRoles(new ArrayList<>(List.of(new Role(roleName))));
        account.setEnabled(enabled);
        return account;
    }
}
