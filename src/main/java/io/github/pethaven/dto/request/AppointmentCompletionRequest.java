package io.github.pethaven.dto.request;

import io.github.pethaven.entity.AppointmentOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Cierre de una cita en curso. Las citas del dueño llevan su resultado; las de mascotas hospitalizadas no.
 *
 * @param outcome       AMBULATORY o ADMISSION (obligatorio en citas del dueño; null en DISCHARGE y BRIEFING)
 * @param treatmentId   tratamiento del ingreso (obligatorio con ADMISSION; null en los demás casos)
 * @param clinicalNotes nota clínica (obligatoria en consultas ambulatorias y reuniones; máx. 1000 caracteres)
 */
public record AppointmentCompletionRequest(
        @Schema(description = "Resultado de la consulta (solo citas del dueño)", example = "AMBULATORY")
        AppointmentOutcome outcome,

        @Schema(description = "Tratamiento del ingreso (solo con ADMISSION)", example = "16")
        Long treatmentId,

        @Schema(description = "Nota clínica", example = "Control sin novedades; volver en un mes.")
        @Size(max = 1000, message = "La nota clínica no puede superar los 1000 caracteres")
        String clinicalNotes
) { }
