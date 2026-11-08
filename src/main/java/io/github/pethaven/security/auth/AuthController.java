package io.github.pethaven.security.auth;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación con Spring Security y JWT: inicio de sesión y perfil del usuario autenticado.
 */
@Tag(name = "Autenticación", description = "Inicio de sesión con token JWT y perfil del usuario autenticado")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Inicia sesión como dueño, veterinario o administrador.
     *
     * @param request correo electrónico y contraseña
     * @return el token de acceso y el perfil del usuario
     */
    @Operation(summary = "Iniciar sesión",
            description = "Valida las credenciales y devuelve un token JWT junto con el perfil y el rol del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta desactivada (solo con la contraseña correcta)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Devuelve el perfil del usuario autenticado por el token de la petición.
     *
     * @param authentication autenticación establecida por el filtro JWT
     * @return el perfil y el rol del usuario
     */
    @Operation(summary = "Perfil del usuario autenticado",
            description = "Devuelve el perfil asociado al token de la cabecera Authorization. Permite recuperar la sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/me")
    public AuthProfileResponse me(Authentication authentication) {
        return authService.getProfile(authentication.getName());
    }
}
