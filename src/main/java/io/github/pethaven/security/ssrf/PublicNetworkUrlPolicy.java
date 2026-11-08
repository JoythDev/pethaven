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
 *       link-local (incluidos los metadatos de la nube, 169.254.169.254), CGNAT, multicast, rangos reservados,
 *       las direcciones IPv6 equivalentes y los prefijos IPv6 que transportan IPv4 (IPv4-compatible, NAT64,
 *       6to4 y Teredo).</li>
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
            return (b[0] & 0xFE) != 0xFC                             // fc00::/7 direcciones locales únicas
                    && !isIpv4Translation(b);
        }
        return false;
    }

    /**
     * Prefijos IPv6 que transportan una dirección IPv4 (y con NAT64, 6to4 o Teredo podrían alcanzar redes
     * internas IPv4). Se bloquean enteros: ningún host legítimo de imágenes publica direcciones en ellos.
     */
    private static boolean isIpv4Translation(byte[] b) {
        boolean first96BitsZero = true;                              // ::/96 IPv4-compatible
        for (int i = 0; i < 12; i++) {
            first96BitsZero &= b[i] == 0;
        }
        boolean nat64 = b[0] == 0x00 && b[1] == 0x64 && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B
                && b[4] == 0 && (b[5] == 0 || b[5] == 1);           // 64:ff9b::/96 y 64:ff9b:1::/48
        boolean sixToFour = b[0] == 0x20 && b[1] == 0x02;            // 2002::/16
        boolean teredo = b[0] == 0x20 && b[1] == 0x01 && b[2] == 0 && b[3] == 0; // 2001::/32
        return first96BitsZero || nat64 || sixToFour || teredo;
    }
}
