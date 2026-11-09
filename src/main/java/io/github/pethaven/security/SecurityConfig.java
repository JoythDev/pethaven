package io.github.pethaven.security;

import io.github.pethaven.entity.Role;
import io.github.pethaven.security.jwt.JwtAccessDeniedHandler;
import io.github.pethaven.security.jwt.JwtAuthEntryPoint;
import io.github.pethaven.security.jwt.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Autowired
    public SecurityConfig(
            JwtFilter jwtFilter,
            JwtAuthEntryPoint jwtAuthEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) {
        this.jwtFilter = jwtFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    /**
     * @param h2ConsoleEnabled si la consola H2 está habilitada (solo en desarrollo); con ella apagada no se
     *                         abre ninguna excepción de acceso ni de frames para su ruta
     * @param h2ConsolePath    ruta de la consola H2 ({@code spring.h2.console.path})
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
            @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath
    ) throws Exception {
        if (h2ConsoleEnabled) {
            // La consola H2 se dibuja en frames de su propio origen.
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    authorizeApi(auth);
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers(h2ConsolePath + "/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Matriz de permisos por rol (qué roles llegan a cada operación del API). Que el recurso sea del usuario
     * ("solo el suyo") se comprueba después, en cada operación. Toda ruta de /api sin regla queda denegada.
     */
    private static void authorizeApi(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        final String admin = Role.ADMIN;
        final String vet = Role.VETERINARIAN;
        final String owner = Role.OWNER;

        auth
                // Autenticación
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                // Métricas del negocio
                .requestMatchers(HttpMethod.GET, "/api/administrators/dashboard").hasRole(admin)

                // Medicamentos: consulta para el personal; gestión solo del administrador
                .requestMatchers(HttpMethod.GET, "/api/drugs", "/api/drugs/*").hasAnyRole(admin, vet)
                .requestMatchers(HttpMethod.POST, "/api/drugs", "/api/drugs/import").hasRole(admin)
                .requestMatchers(HttpMethod.PUT, "/api/drugs/*").hasRole(admin)
                .requestMatchers(HttpMethod.DELETE, "/api/drugs/*").hasRole(admin)

                // Dueños: gestión del veterinario; el dueño solo accede a su propio perfil
                .requestMatchers(HttpMethod.GET, "/api/owners").hasAnyRole(admin, vet)
                .requestMatchers(HttpMethod.GET, "/api/owners/*", "/api/owners/*/pets").hasAnyRole(admin, vet, owner)
                .requestMatchers(HttpMethod.POST, "/api/owners").hasRole(vet)
                .requestMatchers(HttpMethod.PUT, "/api/owners/*").hasAnyRole(vet, owner)
                .requestMatchers(HttpMethod.DELETE, "/api/owners/*").hasRole(vet)

                // Mascotas: gestión del veterinario; el dueño solo consulta las suyas
                .requestMatchers(HttpMethod.GET, "/api/pets").hasAnyRole(admin, vet)
                .requestMatchers(HttpMethod.GET, "/api/pets/*", "/api/pets/*/treatments", "/api/pets/*/photo")
                .hasAnyRole(admin, vet, owner)
                .requestMatchers(HttpMethod.POST, "/api/pets", "/api/pets/*/discharge").hasRole(vet)
                .requestMatchers(HttpMethod.PUT, "/api/pets/*", "/api/pets/*/active").hasRole(vet)

                // Tratamientos: consulta para el personal; registro solo del veterinario
                .requestMatchers(HttpMethod.GET, "/api/treatments", "/api/treatments/*", "/api/treatments/*/drugs")
                .hasAnyRole(admin, vet)
                .requestMatchers(HttpMethod.POST, "/api/treatments", "/api/treatments/*/drugs").hasRole(vet)

                // Veterinarios: gestión del administrador; el veterinario solo accede a su propio perfil
                .requestMatchers(HttpMethod.GET, "/api/veterinarians").hasRole(admin)
                .requestMatchers(HttpMethod.GET, "/api/veterinarians/*", "/api/veterinarians/*/treatments")
                .hasAnyRole(admin, vet)
                .requestMatchers(HttpMethod.POST, "/api/veterinarians").hasRole(admin)
                .requestMatchers(HttpMethod.PUT, "/api/veterinarians/*").hasAnyRole(admin, vet)
                .requestMatchers(HttpMethod.PUT, "/api/veterinarians/*/active").hasRole(admin)

                // Denegado por defecto: una operación nueva no queda abierta por olvido.
                .requestMatchers("/api/**").denyAll();
    }

    /**
     * Spring Boot registra automáticamente todo bean {@code Filter} en el contenedor de servlets.
     * JwtFilter solo debe ejecutarse dentro de la cadena de seguridad, así que esa registración se desactiva.
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * CORS de la API para la SPA. Los orígenes salen de {@code app.cors.allowed-origins} (lista separada por
     * comas). Sin credenciales: el token viaja en la cabecera {@code Authorization}, no en cookies.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Login con usuario y contraseña. El estado de la cuenta (deshabilitada, bloqueada o expirada) se
     * comprueba solo después de validar la contraseña: sin ella, la respuesta es siempre "credenciales
     * incorrectas" y no se puede averiguar que una cuenta existe y está desactivada.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(user -> { });
        provider.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return new ProviderManager(provider);
    }
}
