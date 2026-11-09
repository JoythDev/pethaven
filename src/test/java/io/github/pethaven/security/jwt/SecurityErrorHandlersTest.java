package io.github.pethaven.security.jwt;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

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

    @Test
    public void jwtAuthEntryPoint_commence_ResolverCannotHandle_FallsBackTo401() {
        // Arrange
        // El resolver devuelve null cuando no puede negociar el cuerpo (p. ej. Accept: application/xml).
        Mockito.when(resolver.resolveException(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any()))
                .thenReturn(null);
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(resolver);

        // Act
        entryPoint.commence(request, response, new InsufficientAuthenticationException("Sin token"));

        // Assert
        Assertions.assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    public void jwtAuthEntryPoint_commence_ResolverHandles_KeepsResolverResponse() {
        // Arrange
        Mockito.when(resolver.resolveException(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any()))
                .thenReturn(new ModelAndView());
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(resolver);

        // Act
        entryPoint.commence(request, response, new InsufficientAuthenticationException("Sin token"));

        // Assert
        // El estado lo escribe GlobalExceptionHandler (aquí, simulado): el entry point no lo sobrescribe.
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void jwtAuthEntryPoint_commence_ResolverRethrowsAccessDenied_FallsBackTo401WithoutPropagating() {
        // Arrange
        // With method security on, Spring Security adds a resolver that rethrows any AccessDeniedException in the
        // cause chain once GlobalExceptionHandler cannot write the body (e.g. Accept: application/xml).
        Mockito.when(resolver.resolveException(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any()))
                .thenThrow(new AccessDeniedException("Access Denied"));
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(resolver);

        // Act
        entryPoint.commence(request, response, new InsufficientAuthenticationException("Sin token"));

        // Assert
        Assertions.assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    public void jwtAccessDeniedHandler_handle_ResolverRethrowsAccessDenied_FallsBackTo403WithoutPropagating() {
        // Arrange
        Mockito.when(resolver.resolveException(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any()))
                .thenThrow(new AccessDeniedException("Access Denied"));
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(resolver);

        // Act
        handler.handle(request, response, new AccessDeniedException("Access Denied"));

        // Assert
        Assertions.assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void jwtAccessDeniedHandler_handle_ResolverCannotHandle_FallsBackTo403() {
        // Arrange
        Mockito.when(resolver.resolveException(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any()))
                .thenReturn(null);
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(resolver);

        // Act
        handler.handle(request, response, new AccessDeniedException("Access Denied"));

        // Assert
        Assertions.assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void jwtAccessDeniedHandler_handle_ResolverHandles_KeepsResolverResponse() {
        // Arrange
        Mockito.when(resolver.resolveException(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any()))
                .thenReturn(new ModelAndView());
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(resolver);

        // Act
        handler.handle(request, response, new AccessDeniedException("Access Denied"));

        // Assert
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }
}
