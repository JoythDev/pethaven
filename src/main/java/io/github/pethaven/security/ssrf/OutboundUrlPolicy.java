package io.github.pethaven.security.ssrf;

import java.net.URI;

/**
 * Decide si el servidor puede hacer una petición saliente a una URL controlada por un usuario
 * (protección contra SSRF: Server-Side Request Forgery).
 */
@FunctionalInterface
public interface OutboundUrlPolicy {

    /** @return true solo si la URL es un destino permitido; ante cualquier duda, false */
    boolean isAllowed(URI uri);
}
