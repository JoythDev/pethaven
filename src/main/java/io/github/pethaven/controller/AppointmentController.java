package io.github.pethaven.controller;

import io.github.pethaven.dto.common.ApiErrorResponse;
import io.github.pethaven.dto.request.AppointmentCompletionRequest;
import io.github.pethaven.dto.request.AppointmentRescheduleRequest;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.Role;
import io.github.pethaven.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del módulo de citas. La reserva y el listado del dueño están en {@link OwnerController}.
 */
@Tag(name = "Appointments", description = "Agendamiento de citas")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Lista las franjas libres de cada día de la ventana de reserva del dueño.
     *
     * @return un elemento por día, de hoy al último día reservable
     */
    @Operation(summary = "Consultar disponibilidad",
            description = "Devuelve, para cada día de la ventana de reserva, si la clínica atiende y las franjas con al menos un veterinario activo libre. No expone qué veterinario está libre.")
    @GetMapping("/availability")
    public List<AvailabilityDayResponse> getAvailability() {
        return appointmentService.getAvailability();
    }

    /**
     * Obtiene el detalle de una cita por su id.
     *
     * @param id id de la cita
     * @return la cita encontrada
     */
    @Operation(summary = "Obtener una cita", description = "Devuelve el detalle de una cita. Solo su dueño o su veterinario asignado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe una cita con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.ownsAppointment(#id, authentication.name) or @access.attendsAppointment(#id, authentication.name)")
    @GetMapping("/{id}")
    public AppointmentResponse getAppointmentById(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }

    /**
     * Cancela una cita programada y libera su franja.
     *
     * @param id             id de la cita
     * @param authentication usuario que cancela (dueño o veterinario asignado)
     * @return la cita cancelada
     */
    @Operation(summary = "Cancelar una cita",
            description = "Cancela una cita programada y libera su franja. El dueño puede hacerlo hasta 2 horas antes; el veterinario asignado, en cualquier momento antes de iniciarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita cancelada"),
            @ApiResponse(responseCode = "404", description = "No existe una cita con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La cita no está programada o el dueño ya no tiene la antelación mínima",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.ownsAppointment(#id, authentication.name) or @access.attendsAppointment(#id, authentication.name)")
    @PutMapping("/{id}/cancel")
    public AppointmentResponse cancelAppointment(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id,
            Authentication authentication) {
        return appointmentService.cancelAppointment(id, actorOf(authentication));
    }

    /**
     * Mueve una cita programada a otra franja.
     *
     * @param id             id de la cita
     * @param request        nueva franja y, para el dueño, sus notas
     * @param authentication usuario que reprograma (dueño o veterinario asignado)
     * @return la cita reprogramada
     */
    @Operation(summary = "Reprogramar una cita",
            description = "Mueve una cita programada a otra franja libre, conservando su motivo e historial. El dueño puede hacerlo hasta 2 horas antes y dentro de su ventana de reserva; el veterinario asignado, en cualquier momento antes de iniciarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita reprogramada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una cita con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Franja inválida u ocupada, cita no programada o sin antelación mínima",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.ownsAppointment(#id, authentication.name) or @access.attendsAppointment(#id, authentication.name)")
    @PutMapping("/{id}/reschedule")
    public AppointmentResponse rescheduleAppointment(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id,
            @Valid @RequestBody AppointmentRescheduleRequest request,
            Authentication authentication) {
        return appointmentService.rescheduleAppointment(id, request, actorOf(authentication));
    }

    /**
     * Inicia la consulta de una cita programada para hoy.
     *
     * @param id id de la cita
     * @return la cita en curso
     */
    @Operation(summary = "Iniciar una cita", description = "Pasa a «en curso» una cita programada para hoy. Solo el veterinario asignado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita en curso"),
            @ApiResponse(responseCode = "409", description = "La cita no está programada o no es de hoy",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.attendsAppointment(#id, authentication.name)")
    @PutMapping("/{id}/start")
    public AppointmentResponse startAppointment(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id) {
        return appointmentService.startAppointment(id);
    }

    /**
     * Completa una cita en curso con su resultado y su nota clínica.
     *
     * @param id      id de la cita
     * @param request resultado, tratamiento del ingreso y nota clínica
     * @return la cita completada
     */
    @Operation(summary = "Completar una cita",
            description = "Cierra una cita en curso. Consulta del dueño: ambulatoria (con nota clínica) o ingreso (con el tratamiento registrado). Alta / entrega: exige el alta médica y saca a la mascota de hospitalización. Reunión informativa: con nota clínica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita completada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe la cita o el tratamiento indicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La cita no está en curso o faltan datos de su resultado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.attendsAppointment(#id, authentication.name)")
    @PutMapping("/{id}/complete")
    public AppointmentResponse completeAppointment(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id,
            @Valid @RequestBody AppointmentCompletionRequest request) {
        return appointmentService.completeAppointment(id, request);
    }

    /**
     * Marca como no asistida una cita programada cuya hora ya pasó.
     *
     * @param id id de la cita
     * @return la cita marcada
     */
    @Operation(summary = "Marcar «No asistió»", description = "Marca una cita programada cuya hora ya pasó. Solo el veterinario asignado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita marcada como no asistida"),
            @ApiResponse(responseCode = "409", description = "La cita no está programada o su hora aún no llega",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.attendsAppointment(#id, authentication.name)")
    @PutMapping("/{id}/no-show")
    public AppointmentResponse markNoShow(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id) {
        return appointmentService.markNoShow(id);
    }

    /**
     * Registra la mascota provisional de una cita en curso y la enlaza a la cita.
     *
     * @param id      id de la cita
     * @param request datos completos de la mascota (a nombre del dueño de la cita)
     * @return la cita con su mascota registrada
     */
    @Operation(summary = "Registrar la mascota de una cita",
            description = "Durante la consulta, registra la mascota que llegó con datos provisionales (no hospitalizada) y la enlaza a la cita.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota registrada y enlazada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (validación)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La mascota ya está registrada, la cita no está en curso o el dueño no coincide",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@access.attendsAppointment(#id, authentication.name)")
    @PostMapping("/{id}/pet")
    public AppointmentResponse registerProvisionalPet(
            @Parameter(description = "Id de la cita", example = "1") @PathVariable Long id,
            @Valid @RequestBody PetRequest request) {
        return appointmentService.registerProvisionalPet(id, request);
    }

    private static AppointmentActor actorOf(Authentication authentication) {
        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + Role.OWNER).equals(authority.getAuthority()));
        return isOwner ? AppointmentActor.OWNER : AppointmentActor.VETERINARIAN;
    }
}
