package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.service.TreatmentDrugService;
import io.github.pethaven.service.TreatmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del módulo de tratamientos (incluye los medicamentos administrados).
 */
@Tag(name = "Treatments", description = "Gestión de tratamientos")
@RestController
@RequestMapping("/api/treatments")
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final TreatmentDrugService treatmentDrugService;

    @Autowired
    public TreatmentController(
            TreatmentService treatmentService,
            TreatmentDrugService treatmentDrugService
    ) {
        this.treatmentService = treatmentService;
        this.treatmentDrugService = treatmentDrugService;
    }

    /**
     * Lista todos los tratamientos registrados.
     *
     * @return lista de tratamientos
     */
    @Operation(summary = "Listar tratamientos", description = "Devuelve todos los tratamientos registrados con su mascota, veterinario y medicamentos.")
    @GetMapping
    public List<TreatmentResponse> getAllTreatments() {
        return treatmentService.getAllTreatments();
    }

    /**
     * Obtiene el detalle de un tratamiento por su id.
     *
     * @param id id del tratamiento
     * @return el tratamiento encontrado
     */
    @Operation(summary = "Obtener un tratamiento", description = "Devuelve el detalle de un tratamiento por su id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tratamiento encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un tratamiento con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public TreatmentResponse getTreatmentById(
            @Parameter(description = "Id del tratamiento", example = "1") @PathVariable Long id) {
        return treatmentService.getTreatmentById(id);
    }

    /**
     * Registra un tratamiento e incrementa las atenciones del veterinario.
     *
     * @param request datos del tratamiento
     * @return el tratamiento creado (201 + cabecera Location)
     */
    @Operation(summary = "Crear tratamiento",
            description = "Registra un tratamiento para una mascota activa, atendida por un veterinario activo, e incrementa las atenciones del veterinario. La fecha es opcional (por defecto la actual).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tratamiento creado (incluye cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la mascota o el veterinario indicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La mascota o el veterinario no están activos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TreatmentResponse> createTreatment(@Valid @RequestBody TreatmentRequest request) {
        TreatmentResponse newTreatment = treatmentService.createTreatment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newTreatment.id())
                .toUri();
        return ResponseEntity.created(location).body(newTreatment);
    }

    /**
     * Lista los medicamentos administrados en un tratamiento.
     *
     * @param id id del tratamiento
     * @return lista de medicamentos administrados
     */
    @Operation(summary = "Listar medicamentos de un tratamiento",
            description = "Devuelve los medicamentos administrados en un tratamiento específico. Falla con 404 si el tratamiento no existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de medicamentos administrados"),
            @ApiResponse(responseCode = "404", description = "No existe un tratamiento con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/drugs")
    public List<TreatmentDrugResponse> getTreatmentDrugsByTreatmentId(
            @Parameter(description = "Id del tratamiento", example = "1") @PathVariable Long id) {
        treatmentService.getTreatmentById(id);
        return treatmentDrugService.getTreatmentDrugsByTreatmentId(id);
    }

    /**
     * Administra un medicamento en un tratamiento, descontando su inventario.
     *
     * @param id      id del tratamiento
     * @param request datos del medicamento a administrar
     * @return el renglón creado (201, sin Location: no existe endpoint de detalle del renglón)
     */
    @Operation(summary = "Administrar medicamento",
            description = "Administra un medicamento a un tratamiento: descuenta el inventario, suma las unidades vendidas y copia los precios actuales. Falla con 409 si la droga ya fue administrada, no hay stock o la mascota no está activa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medicamento administrado (sin cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el tratamiento o el medicamento indicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Droga repetida, sin stock o mascota inactiva",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/drugs")
    public ResponseEntity<TreatmentDrugResponse> createTreatmentDrug(
            @Parameter(description = "Id del tratamiento", example = "1") @PathVariable Long id,
            @Valid @RequestBody TreatmentDrugRequest request) {
        TreatmentDrugResponse newTreatmentDrug = treatmentDrugService.createTreatmentDrug(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newTreatmentDrug);
    }
}
