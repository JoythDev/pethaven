package io.github.pethaven.security.jwt;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

// El filtro autentica la petición solo con un token válido de un usuario existente y habilitado;
// en cualquier otro caso deja pasar la petición sin autenticar (el entry point decide el 401).
public class JwtFilterTest {

    private static final String EMAIL = "john.doe@example.com";

    private final JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
    private final UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
    private final JwtFilter filter = new JwtFilter(jwtProvider, userDetailsService);

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void jwtFilter_ValidTokenOfEnabledUser_AuthenticatesWithUserAuthorities() throws Exception {
        // Arrange
        Mockito.when(jwtProvider.resolveUsername("valid")).thenReturn(Optional.of(EMAIL));
        Mockito.when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(user(true));

        // Act
        filter.doFilter(bearer("valid"), response, chain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertThat(authentication).isNotNull();
        Assertions.assertThat(authentication.getName()).isEqualTo(EMAIL);
        Assertions.assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_OWNER");
        Assertions.assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    public void jwtFilter_NoAuthorizationHeader_ContinuesUnauthenticated() throws Exception {
        // Act
        filter.doFilter(new MockHttpServletRequest("GET", "/api/owners"), response, chain);

        // Assert
        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isNotNull();
        Mockito.verifyNoInteractions(jwtProvider, userDetailsService);
    }

    @Test
    public void jwtFilter_NonBearerAuthorizationHeader_ContinuesUnauthenticated() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/owners");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isNotNull();
        Mockito.verifyNoInteractions(jwtProvider, userDetailsService);
    }

    @Test
    public void jwtFilter_InvalidToken_ContinuesUnauthenticatedWithoutLoadingUser() throws Exception {
        // Arrange
        Mockito.when(jwtProvider.resolveUsername("invalid")).thenReturn(Optional.empty());

        // Act
        filter.doFilter(bearer("invalid"), response, chain);

        // Assert
        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isNotNull();
        Mockito.verifyNoInteractions(userDetailsService);
    }

    @Test
    public void jwtFilter_ValidTokenOfDeletedUser_ContinuesUnauthenticatedWithoutThrowing() throws Exception {
        // Arrange
        // Dueño borrado (la cascada elimina su cuenta) con un token todavía vigente.
        Mockito.when(jwtProvider.resolveUsername("orphan")).thenReturn(Optional.of(EMAIL));
        Mockito.when(userDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("Usuario no encontrado"));

        // Act
        filter.doFilter(bearer("orphan"), response, chain);

        // Assert
        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void jwtFilter_ValidTokenOfDisabledUser_ContinuesUnauthenticated() throws Exception {
        // Arrange
        // Veterinario desactivado después de recibir su token: el token deja de servir.
        Mockito.when(jwtProvider.resolveUsername("disabled")).thenReturn(Optional.of(EMAIL));
        Mockito.when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(user(false));

        // Act
        filter.doFilter(bearer("disabled"), response, chain);

        // Assert
        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    public void jwtFilter_ContextAlreadyAuthenticated_DoesNotReplaceIt() throws Exception {
        // Arrange
        Authentication existing = new UsernamePasswordAuthenticationToken("already@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        Mockito.when(jwtProvider.resolveUsername("valid")).thenReturn(Optional.of(EMAIL));

        // Act
        filter.doFilter(bearer("valid"), response, chain);

        // Assert
        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        Mockito.verifyNoInteractions(userDetailsService);
    }

    private static MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/owners");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static UserDetails user(boolean enabled) {
        return User.withUsername(EMAIL)
                .password("{noop}irrelevant")
                .disabled(!enabled)
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"))
                .build();
    }
}
