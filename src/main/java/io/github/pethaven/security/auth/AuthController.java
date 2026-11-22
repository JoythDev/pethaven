package io.github.pethaven.security.auth;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.security.auth.dto.AuthProfileResponse;
import io.github.pethaven.security.auth.dto.AuthResponse;
import io.github.pethaven.security.auth.dto.LoginRequest;
import io.github.pethaven.security.auth.dto.OtpChallengeResponse;
import io.github.pethaven.security.auth.dto.ResendOtpRequest;
import io.github.pethaven.security.auth.dto.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
 * Autenticación con Spring Security y JWT: inicio de sesión en dos pasos (contraseña y código de un solo uso
 * enviado por correo) y perfil del usuario autenticado.
 */
@Tag(name = "Autenticación", description = "Inicio de sesión en dos pasos (contraseña y código por correo), token JWT y perfil del usuario autenticado")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Primer paso del inicio de sesión como dueño, veterinario o administrador.
     *
     * @param request correo electrónico y contraseña
     * @return el desafío de verificación; el código se envió al correo de la cuenta
     */
    // Operación pública: anula el requisito global del token Bearer en la documentación.
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión (paso 1)",
            description = "Valida las credenciales y envía un código de 6 dígitos al correo de la cuenta. No devuelve el token: "
                    + "el token se obtiene con la verificación del código (/auth/verify-otp).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas; código enviado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta desactivada (solo con la contraseña correcta)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "No se pudo enviar el código por correo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/login")
    public OtpChallengeResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Segundo paso del inicio de sesión: verifica el código recibido por correo.
     *
     * @param request identificador del desafío y código de 6 dígitos
     * @return el token de acceso y el perfil del usuario
     */
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión (paso 2)",
            description = "Verifica el código recibido por correo y devuelve el token JWT con el perfil y el rol del usuario. "
                    + "El código vence a los 5 minutos, se usa una sola vez y admite 5 intentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código correcto"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Código incorrecto o vencido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "La cuenta se desactivó entre los dos pasos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Se agotaron los intentos de verificación",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    /**
     * Pide un código nuevo para un inicio de sesión pendiente.
     *
     * @param request identificador del desafío
     * @return el mismo desafío, con la vigencia y la espera renovadas
     */
    @SecurityRequirements
    @Operation(summary = "Reenviar el código",
            description = "Envía un código nuevo al correo de la cuenta. Hay que esperar 60 s entre reenvíos y se permiten 3 por desafío.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código reenviado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "El desafío no existe o venció",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Falta esperar o se agotaron los reenvíos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "No se pudo enviar el código por correo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/resend-otp")
    public OtpChallengeResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendOtp(request);
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
