package io.github.pethaven.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Delega un error de la cadena de seguridad en el resolver de Spring MVC, para que GlobalExceptionHandler
 * escriba el ApiErrorResponse.
 */
final class SecurityErrorResponses {

    private SecurityErrorResponses() {
    }

    /**
     * @return {@code true} si el resolver escribió la respuesta; {@code false} si no pudo y quien llama debe
     * fijar el estado. Con la seguridad por método activa, Spring Security añade un resolver que relanza toda
     * AccessDeniedException de la cadena de causas cuando GlobalExceptionHandler no logra escribir el cuerpo
     * (p. ej. el cliente no acepta JSON); esa excepción no puede salir de la cadena de filtros, así que cuenta
     * como "no pudo".
     */
    static boolean resolve(HandlerExceptionResolver resolver, HttpServletRequest request,
                           HttpServletResponse response, Exception exception) {
        try {
            return resolver.resolveException(request, response, null, exception) != null;
        } catch (RuntimeException resolverFailure) {
            return false;
        }
    }
}
