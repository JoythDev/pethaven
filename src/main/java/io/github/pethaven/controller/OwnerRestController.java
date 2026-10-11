package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.service.OwnerService;
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
 * Controlador REST del módulo de dueños.
 */
@Tag(name = "Owners", description = "Gestión de dueños de mascotas")
@RestController
@RequestMapping("/api/owners")
public class OwnerRestController {

    private final OwnerService ownerService;

    @Autowired
    public OwnerRestController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * Lista todos los dueños registrados.
     *
     * @return lista de dueños (sin datos sensibles)
     */
    @Operation(summary = "Listar dueños", description = "Devuelve todos los dueños registrados, sin datos sensibles.")
    @GetMapping
    public List<OwnerResponse> getAllOwners() {
        return ownerService.getAllOwners();
    }

    /**
     * Obtiene el detalle de un dueño por su id.
     *
     * @param id id del dueño
     * @return el dueño encontrado
     */
    @Operation(summary = "Obtener un dueño", description = "Devuelve el detalle de un dueño por su id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un dueño con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public OwnerResponse getOwnerById(
            @Parameter(description = "Id del dueño", example = "1") @PathVariable Long id) {
        return ownerService.getOwnerById(id);
    }

    /**
     * Registra un nuevo dueño.
     *
     * @param request datos del dueño
     * @return el dueño creado (201 + cabecera Location)
     */
    @Operation(summary = "Crear dueño", description = "Registra un nuevo dueño. Devuelve 201 con la cabecera Location.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dueño creado (incluye cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento o correo ya registrados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerRequest request) {
        OwnerResponse newOwner = ownerService.createOwner(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newOwner.id())
                .toUri();
        return ResponseEntity.created(location).body(newOwner);
    }

    /**
     * Actualiza los datos de un dueño.
     *
     * @param id      id del dueño
     * @param request datos actualizados (la contraseña es opcional)
     * @return el dueño actualizado
     */
    @Operation(summary = "Actualizar dueño",
            description = "Reemplaza los datos del dueño. Si la contraseña se omite o va en blanco, se conserva la actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dueño actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un dueño con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento o correo pertenecen a otro dueño",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public OwnerResponse updateOwner(
            @Parameter(description = "Id del dueño", example = "1") @PathVariable Long id,
            @Valid @RequestBody OwnerUpdateRequest request) {
        return ownerService.updateOwner(id, request);
    }

    /**
     * Elimina un dueño y sus mascotas en cascada.
     *
     * @param id id del dueño
     */
    @Operation(summary = "Eliminar dueño",
            description = "Elimina un dueño y sus mascotas en cascada. Falla con 409 si alguna mascota tiene tratamientos registrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dueño eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe un dueño con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Alguna mascota del dueño tiene tratamientos registrados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwner(
            @Parameter(description = "Id del dueño", example = "1") @PathVariable Long id) {
        ownerService.deleteOwnerById(id);
        return ResponseEntity.noContent().build();
    }

    // TODO (Fase 2): habilitar GET /{id}/pets → [PetResponse] al cerrar la Fase 2.

}
