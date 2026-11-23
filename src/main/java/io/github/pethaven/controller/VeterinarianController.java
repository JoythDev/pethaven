package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.VeterinarianAppointmentRequest;
import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.service.AppointmentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controlador REST del módulo de veterinarios.
 */
@Tag(name = "Veterinarians", description = "Gestión de veterinarios")
@RestController
@RequestMapping("/api/veterinarians")
public class VeterinarianController {

    private static final int MAX_AGENDA_DAYS = 31;

    private final VeterinarianService veterinarianService;
    private final TreatmentService treatmentService;
    private final AppointmentService appointmentService;

    @Autowired
    public VeterinarianController(
            VeterinarianService veterinarianService,
            TreatmentService treatmentService,
            AppointmentService appointmentService
    ) {
        this.veterinarianService = veterinarianService;
        this.treatmentService = treatmentService;
        this.appointmentService = appointmentService;
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
    @PreAuthorize("hasRole('ADMIN') or @access.isVeterinarian(#id, authentication.name)")
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
    @PreAuthorize("hasRole('ADMIN') or @access.isVeterinarianKeepingIdentity(#id, #request.document(), #request.email(), authentication.name)")
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
    @PreAuthorize("hasRole('ADMIN') or @access.isVeterinarian(#id, authentication.name)")
    @GetMapping("/{id}/treatments")
    public List<TreatmentResponse> getTreatmentsByVeterinarianId(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id) {
        veterinarianService.getVeterinarianById(id);
        return treatmentService.getTreatmentsByVeterinarianId(id);
    }

    /**
     * Lista la agenda del veterinario entre dos fechas (ambas incluidas), en todos los estados.
     *
     * @param id   id del veterinario
     * @param from primer día
     * @param to   último día (máximo 31 días después de {@code from})
     * @return citas del veterinario ordenadas por hora
     */
    @Operation(summary = "Agenda de un veterinario",
            description = "Devuelve las citas asignadas al veterinario entre dos fechas (ambas incluidas, máximo 31 días), en cualquier estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Citas del veterinario"),
            @ApiResponse(responseCode = "400", description = "Rango de fechas inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un veterinario con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.isVeterinarian(#id, authentication.name)")
    @GetMapping("/{id}/appointments")
    public List<AppointmentResponse> getAppointmentsByVeterinarianId(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id,
            @Parameter(description = "Primer día", example = "2026-11-23") @RequestParam LocalDate from,
            @Parameter(description = "Último día", example = "2026-11-29") @RequestParam LocalDate to) {
        ensureValidRange(from, to);
        return appointmentService.getAppointmentsByVeterinarianId(id, from, to);
    }

    /**
     * Lista las franjas libres del veterinario entre dos fechas, para agendar citas de mascotas hospitalizadas.
     *
     * @param id   id del veterinario
     * @param from primer día
     * @param to   último día (máximo 31 días después de {@code from})
     * @return un elemento por día con sus franjas libres
     */
    @Operation(summary = "Disponibilidad de un veterinario",
            description = "Devuelve, para cada día del rango, las franjas del horario que aún no empiezan y que el veterinario tiene libres.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Franjas libres por día"),
            @ApiResponse(responseCode = "400", description = "Rango de fechas inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un veterinario con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.isVeterinarian(#id, authentication.name)")
    @GetMapping("/{id}/availability")
    public List<AvailabilityDayResponse> getVeterinarianAvailability(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id,
            @Parameter(description = "Primer día", example = "2026-11-23") @RequestParam LocalDate from,
            @Parameter(description = "Último día", example = "2026-11-29") @RequestParam LocalDate to) {
        ensureValidRange(from, to);
        return appointmentService.getVeterinarianAvailability(id, from, to);
    }

    /**
     * Agenda, a nombre del propio veterinario, una cita de alta o una reunión informativa de una mascota hospitalizada.
     *
     * @param id      id del veterinario
     * @param request mascota, motivo y franja
     * @return la cita creada (201 + cabecera Location)
     */
    @Operation(summary = "Agendar cita de mascota hospitalizada",
            description = "Agenda con el dueño una cita de alta / entrega (DISCHARGE) o una reunión informativa (BRIEFING) de una mascota hospitalizada, asignada al propio veterinario y en una franja suya libre.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cita agendada (incluye cabecera Location)"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el veterinario o la mascota indicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Motivo no permitido, mascota no hospitalizada, franja inválida, pasada u ocupada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.isVeterinarian(#id, authentication.name)")
    @PostMapping("/{id}/appointments")
    public ResponseEntity<AppointmentResponse> scheduleForHospitalizedPet(
            @Parameter(description = "Id del veterinario", example = "1") @PathVariable Long id,
            @Valid @RequestBody VeterinarianAppointmentRequest request) {
        AppointmentResponse newAppointment = appointmentService.scheduleForHospitalizedPet(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/appointments/{id}")
                .buildAndExpand(newAppointment.id())
                .toUri();
        return ResponseEntity.created(location).body(newAppointment);
    }

    private static void ensureValidRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_AGENDA_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El rango de fechas no puede superar los " + MAX_AGENDA_DAYS + " días.");
        }
    }
}
