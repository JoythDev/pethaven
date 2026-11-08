package io.github.pethaven.security.jwt;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Los manejadores de la cadena de filtros no escriben la respuesta: la delegan en el resolver de
// Spring MVC (sin handler) para que GlobalExceptionHandler aplique el contrato ApiErrorResponse.
public class SecurityErrorHandlersTest {

    private final HandlerExceptionResolver resolver = Mockito.mock(HandlerExceptionResolver.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/owners");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    public void jwtAuthEntryPoint_commence_DelegatesExceptionToResolverWithoutHandler() {
        // Arrange
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(resolver);
        InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Sin token");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        Mockito.verify(resolver).resolveException(request, response, null, exception);
        Mockito.verifyNoMoreInteractions(resolver);
    }

    @Test
    public void jwtAccessDeniedHandler_handle_DelegatesExceptionToResolverWithoutHandler() {
        // Arrange
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(resolver);
        AccessDeniedException exception = new AccessDeniedException("Access Denied");

        // Act
        handler.handle(request, response, exception);

        // Assert
        Mockito.verify(resolver).resolveException(request, response, null, exception);
        Mockito.verifyNoMoreInteractions(resolver);
    }
}
