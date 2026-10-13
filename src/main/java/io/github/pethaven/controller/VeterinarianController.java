package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.service.TreatmentService;
import io.github.pethaven.service.VeterinarianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del módulo de veterinarios.
 */
@Tag(name = "Veterinarians", description = "Gestión de veterinarios")
@RestController
@RequestMapping("/api/veterinarians")
public class VeterinarianController {

    private final VeterinarianService veterinarianService;
    private final TreatmentService treatmentService;

    @Autowired
    public VeterinarianController(
            VeterinarianService veterinarianService,
            TreatmentService treatmentService
    ) {
        this.veterinarianService = veterinarianService;
        this.treatmentService = treatmentService;
    }

    /**
     * Lista todos los veterinarios registrados.
     *
     * @return lista de veterinarios (sin datos sensibles)
     */
    @Operation(summary = "Listar veterinarios", description = "Devuelve todos los veterinarios registrados, sin datos sensibles.")
    @GetMapping
    public List<VeterinarianResponse> getAllVeterinarians() {
        return veterinarianService.getAllVeterinarians();
    }

    /**
     * Obtiene el detalle de un veterinario por su id.
     *
     * @param id id del veterinario
     * @return el veterinario encontrado
     */
    @Operation(summary = "Obtener un veterinario", description = "Devuelve el detalle de un veterinario por su id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veterinario encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un veterinario con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public VeterinarianResponse getVeterinarianById(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id) {
        return veterinarianService.getVeterinarianById(id);
    }

    /**
     * Registra un nuevo veterinario.
     *
     * @param request datos del veterinario
     * @return el veterinario creado (201 + cabecera Location)
     */
    @Operation(summary = "Crear veterinario", description = "Registra un nuevo veterinario (activo por defecto). Devuelve 201 con la cabecera Location.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Veterinario creado (incluye cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento o correo ya registrados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<VeterinarianResponse> createVeterinarian(@Valid @RequestBody VeterinarianRequest request) {
        VeterinarianResponse newVeterinarian = veterinarianService.createVeterinarian(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newVeterinarian.id())
                .toUri();
        return ResponseEntity.created(location).body(newVeterinarian);
    }

    /**
     * Actualiza los datos de un veterinario.
     *
     * @param id      id del veterinario
     * @param request datos actualizados (la contraseña es opcional)
     * @return el veterinario actualizado
     */
    @Operation(summary = "Actualizar veterinario",
            description = "Reemplaza los datos del veterinario. Si la contraseña se omite o va en blanco, se conserva la actual. El estado y las atenciones no se modifican.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veterinario actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un veterinario con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento o correo pertenecen a otro veterinario",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public VeterinarianResponse updateVeterinarian(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id,
            @Valid @RequestBody VeterinarianUpdateRequest request) {
        return veterinarianService.updateVeterinarian(id, request);
    }

    /**
     * Activa o desactiva un veterinario.
     *
     * @param id     id del veterinario
     * @param active nuevo estado laboral
     * @return el veterinario actualizado
     */
    @Operation(summary = "Activar o desactivar veterinario",
            description = "Cambia el estado laboral del veterinario (activo o inactivo, p. ej. vacaciones o incapacidad).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Parámetro active inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un veterinario con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/active")
    public VeterinarianResponse switchVeterinarianActiveStatus(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id,
            @Parameter(description = "Nuevo estado", example = "true") @RequestParam("active") boolean active) {
        return veterinarianService.switchVeterinarianActiveStatus(id, active);
    }

    /**
     * Lista los tratamientos (mascotas atendidas) de un veterinario.
     *
     * @param id id del veterinario
     * @return lista de tratamientos del veterinario
     */
    @Operation(summary = "Listar tratamientos de un veterinario",
            description = "Devuelve las mascotas atendidas por el veterinario. Falla con 404 si el veterinario no existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tratamientos del veterinario"),
            @ApiResponse(responseCode = "404", description = "No existe un veterinario con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/treatments")
    public List<TreatmentResponse> getTreatmentsByVeterinarianId(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id) {
        veterinarianService.getVeterinarianById(id);
        return treatmentService.getTreatmentsByVeterinarianId(id);
    }
}
