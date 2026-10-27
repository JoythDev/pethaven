package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.LoginRequest;
import io.github.pethaven.dto.response.AuthResponse;
import io.github.pethaven.mapper.AuthMapper;
import io.github.pethaven.service.AdministratorService;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.VeterinarianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de autenticación provisional (sin Spring Security ni token).
 * Solo valida credenciales y devuelve el usuario con su rol.
 */
@Tag(name = "Auth", description = "Inicio de sesión provisional (sin token)")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OwnerService ownerService;
    private final VeterinarianService veterinarianService;
    private final AdministratorService administratorService;
    private final AuthMapper authMapper;

    @Autowired
    public AuthController(
            OwnerService ownerService,
            VeterinarianService veterinarianService,
            AdministratorService administratorService,
            AuthMapper authMapper
    ) {
        this.ownerService = ownerService;
        this.veterinarianService = veterinarianService;
        this.administratorService = administratorService;
        this.authMapper = authMapper;
    }

    /**
     * Inicia sesión como dueño.
     *
     * @param request credenciales del dueño
     * @return el dueño autenticado con su rol
     */
    @Operation(summary = "Login de dueño",
            description = "Valida las credenciales del dueño y devuelve sus datos con el rol OWNER. Provisional: no emite token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/owners/login")
    public AuthResponse loginOwner(@Valid @RequestBody LoginRequest request) {
        return authMapper.toResponse(ownerService.authenticate(request.email(), request.password()));
    }

    /**
     * Inicia sesión como veterinario.
     *
     * @param request credenciales del veterinario
     * @return el veterinario autenticado con su rol
     */
    @Operation(summary = "Login de veterinario",
            description = "Valida las credenciales del veterinario y devuelve sus datos con el rol VETERINARIAN. Falla con 403 si la cuenta está desactivada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta desactivada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/veterinarians/login")
    public AuthResponse loginVeterinarian(@Valid @RequestBody LoginRequest request) {
        return authMapper.toResponse(veterinarianService.authenticate(request.email(), request.password()));
    }

    /**
     * Inicia sesión como administrador.
     *
     * @param request credenciales del administrador
     * @return el administrador autenticado con su rol
     */
    @Operation(summary = "Login de administrador",
            description = "Valida las credenciales del administrador y devuelve sus datos con el rol ADMIN. Provisional: no emite token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/administrators/login")
    public AuthResponse loginAdministrator(@Valid @RequestBody LoginRequest request) {
        return authMapper.toResponse(administratorService.authenticate(request.email(), request.password()));
    }
}
