package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.service.DrugService;
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
 * Controlador REST del módulo de medicamentos.
 */
@Tag(name = "Drugs", description = "Gestión de medicamentos")
@RestController
@RequestMapping("/api/drugs")
public class DrugRestController {

    private final DrugService drugService;

    @Autowired
    public DrugRestController(DrugService drugService) {
        this.drugService = drugService;
    }

    /**
     * Lista todos los medicamentos registrados.
     *
     * @return lista de medicamentos
     */
    @Operation(summary = "Listar medicamentos", description = "Devuelve todos los medicamentos registrados.")
    @GetMapping
    public List<DrugResponse> getAllDrugs() {
        return drugService.getAllDrugs();
    }

    /**
     * Obtiene el detalle de un medicamento por su id.
     *
     * @param id id del medicamento
     * @return el medicamento encontrado
     */
    @Operation(summary = "Obtener un medicamento", description = "Devuelve el detalle de un medicamento por su id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medicamento encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un medicamento con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public DrugResponse getDrugById(
            @Parameter(description = "Id del medicamento", example = "1") @PathVariable Long id) {
        return drugService.getDrugById(id);
    }

    /**
     * Registra un nuevo medicamento.
     *
     * @param request datos del medicamento
     * @return el medicamento creado (201 + cabecera Location)
     */
    @Operation(summary = "Crear medicamento",
            description = "Registra un nuevo medicamento. Las unidades vendidas empiezan en 0 y las controla el sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medicamento creado (incluye cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un medicamento con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DrugResponse> createDrug(@Valid @RequestBody DrugRequest request) {
        DrugResponse newDrug = drugService.createDrug(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newDrug.id())
                .toUri();
        return ResponseEntity.created(location).body(newDrug);
    }

    /**
     * Actualiza los datos de un medicamento.
     *
     * @param id      id del medicamento
     * @param request datos actualizados
     * @return el medicamento actualizado
     */
    @Operation(summary = "Actualizar medicamento",
            description = "Reemplaza los datos del medicamento. Las unidades vendidas no se modifican (solo lectura).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medicamento actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un medicamento con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe otro medicamento con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public DrugResponse updateDrug(
            @Parameter(description = "Id del medicamento", example = "1") @PathVariable Long id,
            @Valid @RequestBody DrugRequest request) {
        return drugService.updateDrug(id, request);
    }

    /**
     * Elimina un medicamento.
     *
     * @param id id del medicamento
     */
    @Operation(summary = "Eliminar medicamento",
            description = "Elimina un medicamento. Falla con 409 si está asociado a tratamientos existentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Medicamento eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe un medicamento con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El medicamento está asociado a uno o más tratamientos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDrug(
            @Parameter(description = "Id del medicamento", example = "1") @PathVariable Long id) {
        drugService.deleteDrugById(id);
        return ResponseEntity.noContent().build();
    }
}
