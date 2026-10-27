package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.service.PetService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del módulo de mascotas.
 */
@Tag(name = "Pets", description = "Gestión de mascotas")
@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final TreatmentService treatmentService;

    @Autowired
    public PetController(
            PetService petService,
            TreatmentService treatmentService
    ) {
        this.petService = petService;
        this.treatmentService = treatmentService;
    }

    /**
     * Lista todas las mascotas o busca por nombre.
     *
     * @param search filtro opcional por nombre (substring, sin distinguir mayúsculas)
     * @return lista de mascotas
     */
    @Operation(summary = "Listar o buscar mascotas",
            description = "Sin parámetros devuelve todas las mascotas. Con search filtra por nombre (substring, sin distinguir mayúsculas; incluye activas e inactivas).")
    @GetMapping
    public List<PetResponse> getAllPets(
            @Parameter(description = "Filtro por nombre (substring, sin distinguir mayúsculas)", example = "bud")
            @RequestParam(value = "search", required = false) String search) {
        if (search == null || search.isBlank()) {
            return petService.getAllPets();
        }
        return petService.searchPets(search.trim());
    }

    /**
     * Obtiene el detalle de una mascota por su id.
     *
     * @param id id de la mascota
     * @return la mascota encontrada
     */
    @Operation(summary = "Obtener una mascota", description = "Devuelve el detalle de una mascota por su id, incluido su dueño.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe una mascota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public PetResponse getPetById(
            @Parameter(description = "Id de la mascota", example = "1") @PathVariable Long id) {
        return petService.getPetById(id);
    }

    /**
     * Registra una nueva mascota.
     *
     * @param request datos de la mascota
     * @return la mascota creada (201 + cabecera Location)
     */
    @Operation(summary = "Crear mascota", description = "Registra una nueva mascota (activa por defecto). Devuelve 201 con la cabecera Location.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mascota creada (incluye cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un dueño con el ownerId indicado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PetResponse> createPet(@Valid @RequestBody PetRequest request) {
        PetResponse newPet = petService.createPet(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newPet.id())
                .toUri();
        return ResponseEntity.created(location).body(newPet);
    }

    /**
     * Actualiza los datos de una mascota (reemplazo total; puede reasignar dueño).
     *
     * @param id      id de la mascota
     * @param request datos actualizados
     * @return la mascota actualizada
     */
    @Operation(summary = "Actualizar mascota",
            description = "Reemplaza los datos de la mascota; puede reasignarla a otro dueño.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la mascota o el dueño indicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public PetResponse updatePet(
            @Parameter(description = "Id de la mascota", example = "1") @PathVariable Long id,
            @Valid @RequestBody PetRequest request) {
        return petService.updatePet(id, request);
    }

    /**
     * Activa o desactiva una mascota.
     *
     * @param id     id de la mascota
     * @param active nuevo estado
     * @return la mascota actualizada
     */
    @Operation(summary = "Activar o desactivar mascota",
            description = "Cambia el estado de la mascota (en la veterinaria o en su casa).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Parámetro active inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una mascota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/active")
    public PetResponse switchPetActiveStatus(
            @Parameter(description = "Id de la mascota", example = "1") @PathVariable Long id,
            @Parameter(description = "Nuevo estado", example = "true") @RequestParam("active") boolean active) {
        return petService.switchPetActiveStatus(id, active);
    }

    /**
     * Lista los tratamientos (historial médico) de una mascota.
     *
     * @param id id de la mascota
     * @return lista de tratamientos de la mascota
     */
    @Operation(summary = "Listar tratamientos de una mascota",
            description = "Devuelve el historial de tratamientos de la mascota. Falla con 404 si la mascota no existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de tratamientos"),
            @ApiResponse(responseCode = "404", description = "No existe una mascota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/treatments")
    public List<TreatmentResponse> getTreatmentsByPetId(
            @Parameter(description = "Id de la mascota", example = "1") @PathVariable Long id) {
        petService.getPetById(id);
        return treatmentService.getTreatmentsByPetId(id);
    }

    /**
     * Sirve la foto de la mascota desde el API.
     *
     * @param id id de la mascota
     * @return la imagen, o 404 si no tiene foto o no se pudo descargar
     */
    @Operation(summary = "Foto de una mascota",
            description = "Descarga la foto desde su URL y la sirve desde el API, para usarla donde el host original no permite CORS (p. ej. el PDF de la historia clínica). Solo JPEG, PNG, WebP, GIF o AVIF de hasta 5 MB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen de la mascota"),
            @ApiResponse(responseCode = "404", description = "La mascota no existe, no tiene foto o no se pudo descargar")
    })
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPetPhoto(
            @Parameter(description = "Id de la mascota", example = "1") @PathVariable Long id) {
        return petService.getPetPhoto(id)
                .map(photo -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, photo.contentType())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(photo.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Da de alta a una mascota: queda estable y el alta se registra en su historia clínica.
     *
     * @param id             id de la mascota
     * @param veterinarianId id del veterinario que da el alta
     * @return el registro del alta (sin medicamentos, evolución RECOVERED)
     */
    @Operation(summary = "Dar de alta a una mascota",
            description = "La mascota mejoró y ya no necesita medicamentos: queda estable (sin diagnóstico) y el alta se registra en su historia clínica. Falla con 409 si ya está estable o si el veterinario está inactivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alta registrada"),
            @ApiResponse(responseCode = "404", description = "No existe la mascota o el veterinario",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La mascota ya está estable o el veterinario está inactivo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/discharge")
    public TreatmentResponse dischargePet(
            @Parameter(description = "Id de la mascota", example = "1") @PathVariable Long id,
            @Parameter(description = "Id del veterinario que da el alta", example = "1") @RequestParam("veterinarianId") Long veterinarianId) {
        return treatmentService.dischargePet(id, veterinarianId);
    }
}
