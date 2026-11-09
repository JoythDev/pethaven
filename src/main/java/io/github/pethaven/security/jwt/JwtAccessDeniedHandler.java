package io.github.pethaven.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Responde 403 cuando un usuario autenticado no tiene permisos para la ruta solicitada. Igual que
 * {@link JwtAuthEntryPoint}, delega en el {@link HandlerExceptionResolver} de Spring MVC para que
 * {@code GlobalExceptionHandler} construya el cuerpo {@code ApiErrorResponse}.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        if (!SecurityErrorResponses.resolve(handlerExceptionResolver, request, response, accessDeniedException)) {
            // El resolver no pudo construir el cuerpo (p. ej. el cliente no acepta JSON): se garantiza el 403.
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
