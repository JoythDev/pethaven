package io.github.pethaven.security.ssrf;

import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * Permite peticiones salientes solo hacia Internet público:
 * <ul>
 *   <li>esquema {@code http} o {@code https}, sin credenciales en la URL;</li>
 *   <li>puerto implícito, 80 o 443 (impide usar el servidor para escanear puertos);</li>
 *   <li>todas las direcciones a las que resuelve el host son públicas: se bloquean loopback, redes privadas,
 *       link-local (incluidos los metadatos de la nube, 169.254.169.254), CGNAT, multicast, rangos reservados
 *       y las direcciones IPv6 equivalentes.</li>
 * </ul>
 * Riesgo residual conocido: el cliente HTTP vuelve a resolver el nombre al conectar (DNS rebinding); la
 * validación de cada redirección, el límite de saltos y el filtrado del contenido limitan su impacto.
 */
@Component
public class PublicNetworkUrlPolicy implements OutboundUrlPolicy {

    /** Resolución de nombres; sustituible en pruebas para no depender del DNS real. */
    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private static final Set<String> SCHEMES = Set.of("http", "https");
    private static final Set<Integer> PORTS = Set.of(-1, 80, 443);

    private final HostResolver resolver;

    public PublicNetworkUrlPolicy() {
        this(InetAddress::getAllByName);
    }

    PublicNetworkUrlPolicy(HostResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean isAllowed(URI uri) {
        if (uri == null || uri.getScheme() == null
                || !SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))
                || uri.getRawUserInfo() != null
                || !PORTS.contains(uri.getPort())) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        try {
            InetAddress[] addresses = resolver.resolve(host);
            if (addresses == null || addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (!isPublic(address)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return first != 0                                        // 0.0.0.0/8 "esta red"
                    && !(first == 100 && (second & 0xC0) == 64)      // 100.64.0.0/10 CGNAT
                    && !(first == 192 && second == 0 && (b[2] & 0xFF) == 0) // 192.0.0.0/24 asignaciones IETF
                    && !(first == 198 && (second & 0xFE) == 18)      // 198.18.0.0/15 pruebas de rendimiento
                    && first < 240;                                  // 240.0.0.0/4 reservada y broadcast
        }
        if (address instanceof Inet6Address) {
            return (b[0] & 0xFE) != 0xFC;                            // fc00::/7 direcciones locales únicas
        }
        return false;
    }
}
