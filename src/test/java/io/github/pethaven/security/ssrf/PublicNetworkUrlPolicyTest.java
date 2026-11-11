package io.github.pethaven.security.ssrf;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;

// Política anti-SSRF: el servidor solo descarga de URLs http(s) cuyo host resuelve únicamente a direcciones públicas.
// El resolvedor DNS es simulado: las pruebas no dependen de la red.
public class PublicNetworkUrlPolicyTest {

    private static final String PUBLIC_V4 = "93.184.216.34";
    private static final String PUBLIC_V6 = "2606:4700:4700::1111";

    private static PublicNetworkUrlPolicy policyResolving(Map<String, String[]> dns) {
        return new PublicNetworkUrlPolicy(host -> {
            String[] addresses = dns.get(host);
            if (addresses == null) {
                throw new UnknownHostException(host);
            }
            InetAddress[] resolved = new InetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                resolved[i] = InetAddress.getByName(addresses[i]); // literales: no consulta DNS
            }
            return resolved;
        });
    }

    private static boolean allows(String url, String... addresses) {
        return policyResolving(Map.of("photos.test", addresses)).isAllowed(URI.create(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",          // loopback
            "127.10.20.30",       // loopback (todo 127/8)
            "::1",                // loopback IPv6
            "0.0.0.0",            // any-local
            "0.1.2.3",            // 0/8 "esta red"
            "10.0.0.5",           // privada 10/8
            "172.16.0.1",         // privada 172.16/12
            "172.31.255.254",     // privada 172.16/12 (último bloque)
            "192.168.1.1",        // privada 192.168/16
            "169.254.169.254",    // link-local: metadatos de la nube
            "fe80::1",            // link-local IPv6
            "fc00::1",            // IPv6 de ámbito local único (fc00::/7)
            "fd12:3456::1",       // IPv6 de ámbito local único (fd00::/8)
            "100.64.0.1",         // CGNAT 100.64/10
            "100.127.255.254",    // CGNAT (último bloque)
            "192.0.0.8",          // asignaciones IETF 192.0.0/24
            "198.18.0.1",         // pruebas de rendimiento 198.18/15
            "224.0.0.1",          // multicast
            "ff02::1",            // multicast IPv6
            "240.0.0.1",          // reservada 240/4
            "255.255.255.255",    // broadcast
            "::ffff:127.0.0.1",   // IPv4 escrita como IPv6
            "::ffff:10.0.0.1",
            // Prefijos de traducción IPv6 -> IPv4: podrían llegar a direcciones internas con NAT64/6to4/Teredo.
            "::7f00:1",           // IPv4-compatible (::/96) de 127.0.0.1
            "::a00:1",            // IPv4-compatible de 10.0.0.1
            "64:ff9b::7f00:1",    // NAT64 64:ff9b::/96 de 127.0.0.1
            "64:ff9b::5db8:d822", // NAT64 incluso con una IPv4 pública incrustada
            "64:ff9b:1::a00:1",   // NAT64 de uso local 64:ff9b:1::/48
            "2002:7f00:1::1",     // 6to4 2002::/16 de 127.0.0.1
            "2001:0:4136:e378::1" // Teredo 2001::/32
    })
    public void publicNetworkUrlPolicy_HostResolvingToNonPublicAddress_IsBlocked(String address) {
        Assertions.assertThat(allows("https://photos.test/a.png", address)).isFalse();
    }

    @ParameterizedTest
    // Incluye direcciones justo fuera de los rangos bloqueados (172.16/12, 100.64/10, 198.18/15).
    @ValueSource(strings = {PUBLIC_V4, PUBLIC_V6, "172.32.0.1", "100.128.0.1", "198.20.0.1",
            "2001:4860:4860::8888", "2a00:1450:4001::1"})
    public void publicNetworkUrlPolicy_HostResolvingToPublicAddress_IsAllowed(String address) {
        Assertions.assertThat(allows("https://photos.test/a.png", address)).isTrue();
    }

    @Test
    public void publicNetworkUrlPolicy_HostWithPublicAndPrivateAddresses_IsBlocked() {
        // Basta una dirección interna para bloquear: el cliente HTTP podría elegir esa.
        Assertions.assertThat(allows("https://photos.test/a.png", PUBLIC_V4, "10.0.0.5")).isFalse();
    }

    @Test
    public void publicNetworkUrlPolicy_UnresolvableHost_IsBlocked() {
        Assertions.assertThat(policyResolving(Map.of()).isAllowed(URI.create("https://unknown.test/a.png"))).isFalse();
    }

    @Test
    public void publicNetworkUrlPolicy_HostWithoutAddresses_IsBlocked() {
        Assertions.assertThat(allows("https://photos.test/a.png")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://photos.test/a.png",
            "http://photos.test/a.png",
            "https://photos.test:443/a.png",
            "http://photos.test:80/a.png",
            "HTTPS://photos.test/a.png"
    })
    public void publicNetworkUrlPolicy_HttpOrHttpsOnDefaultPort_IsAllowed(String url) {
        Assertions.assertThat(allows(url, PUBLIC_V4)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://photos.test:8443/a.png",      // puerto no estándar: evita escanear puertos de un host público
            "http://photos.test:22/a.png",
            "ftp://photos.test/a.png",              // esquema no permitido
            "file:///etc/passwd",
            "https://user:secret@photos.test/a.png", // credenciales en la URL
            "https:///a.png",                       // sin host
            "/relative/a.png"
    })
    public void publicNetworkUrlPolicy_UnsupportedSchemePortCredentialsOrHost_IsBlocked(String url) {
        Assertions.assertThat(allows(url, PUBLIC_V4)).isFalse();
    }

    @Test
    public void publicNetworkUrlPolicy_NullUri_IsBlocked() {
        Assertions.assertThat(policyResolving(Map.of()).isAllowed(null)).isFalse();
    }

    @Test
    public void publicNetworkUrlPolicy_DefaultResolver_BlocksLocalhostName() {
        // Con el resolvedor real del sistema, "localhost" resuelve a loopback.
        Assertions.assertThat(new PublicNetworkUrlPolicy().isAllowed(URI.create("http://localhost/a.png"))).isFalse();
    }
}
