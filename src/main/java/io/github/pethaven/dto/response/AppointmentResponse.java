package io.github.pethaven.dto.response;

import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentOutcome;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Species;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Cita con su mascota (registrada o provisional), dueño y veterinario asignado.
 *
 * @param id                 id de la cita
 * @param startTime          inicio, en hora local de la clínica
 * @param endTime            fin (inicio + duración de la franja)
 * @param reason             motivo
 * @param status             estado
 * @param petId              id de la mascota (null si aún no está registrada)
 * @param petName            nombre de la mascota (el provisional si aún no está registrada)
 * @param species            especie de la mascota
 * @param provisionalPet     la mascota aún no está registrada
 * @param provisionalBreed   raza provisional (solo mascota no registrada; puede ser null)
 * @param provisionalAge     edad provisional (solo mascota no registrada; puede ser null)
 * @param ownerId            id del dueño
 * @param ownerName          nombre del dueño
 * @param ownerPhone         teléfono del dueño
 * @param veterinarianId     id del veterinario asignado
 * @param veterinarianName   nombre del veterinario asignado
 * @param ownerNotes         notas del dueño (puede ser null)
 * @param clinicalNotes      nota clínica del veterinario (puede ser null)
 * @param outcome            resultado de la consulta (solo citas del dueño completadas; puede ser null)
 * @param treatmentId        tratamiento del ingreso (solo con outcome ADMISSION; puede ser null)
 * @param createdAt          fecha de la reserva
 * @param cancelledBy        quién la canceló (null si no está cancelada)
 * @param cancelledAt        cuándo se canceló (null si no está cancelada)
 * @param cancellationReason motivo de la cancelación (puede ser null)
 */
@Schema(description = "Cita")
public record AppointmentResponse(
        @Schema(description = "Id de la cita", example = "1")
        Long id,

        @Schema(description = "Inicio en hora local de la clínica", example = "2026-11-24T09:30:00")
        LocalDateTime startTime,

        @Schema(description = "Fin en hora local de la clínica", example = "2026-11-24T10:00:00")
        LocalDateTime endTime,

        @Schema(description = "Motivo", example = "CONTROL")
        AppointmentReason reason,

        @Schema(description = "Estado", example = "SCHEDULED")
        AppointmentStatus status,

        @Schema(description = "Id de la mascota (null si aún no está registrada)", example = "1")
        Long petId,

        @Schema(description = "Nombre de la mascota", example = "Buddy")
        String petName,

        @Schema(description = "Especie de la mascota", example = "DOG")
        Species species,

        @Schema(description = "La mascota aún no está registrada", example = "false")
        boolean provisionalPet,

        @Schema(description = "Raza provisional (solo mascota no registrada)", example = "Criollo")
        String provisionalBreed,

        @Schema(description = "Edad provisional en años (solo mascota no registrada)", example = "2")
        Integer provisionalAge,

        @Schema(description = "Id del dueño", example = "1")
        Long ownerId,

        @Schema(description = "Nombre del dueño", example = "Laura Gómez")
        String ownerName,

        @Schema(description = "Teléfono del dueño", example = "3001234567")
        String ownerPhone,

        @Schema(description = "Id del veterinario asignado", example = "1")
        Long veterinarianId,

        @Schema(description = "Nombre del veterinario asignado", example = "Dra. Valentina Soler")
        String veterinarianName,

        @Schema(description = "Notas del dueño (puede ser null)", example = "Cojea un poco de la pata trasera.")
        String ownerNotes,

        @Schema(description = "Nota clínica del veterinario (puede ser null)", example = "Control sin novedades.")
        String clinicalNotes,

        @Schema(description = "Resultado de la consulta (null si no aplica)", example = "AMBULATORY")
        AppointmentOutcome outcome,

        @Schema(description = "Tratamiento del ingreso (null si no aplica)", example = "16")
        Long treatmentId,

        @Schema(description = "Fecha de la reserva", example = "2026-11-22T15:04:00")
        LocalDateTime createdAt,

        @Schema(description = "Quién canceló la cita (null si no está cancelada)", example = "OWNER")
        AppointmentActor cancelledBy,

        @Schema(description = "Cuándo se canceló (null si no está cancelada)", example = "2026-11-23T08:00:00")
        LocalDateTime cancelledAt,

        @Schema(description = "Motivo de la cancelación (puede ser null)", example = "Veterinario no disponible")
        String cancellationReason
) { }
