package io.github.pethaven.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.pethaven.entity.Evolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos para registrar un tratamiento.
 *
 * @param petId          id de la mascota (obligatorio; debe existir y estar activa)
 * @param veterinarianId id del veterinario (obligatorio; debe existir y estar activo)
 * @param date           fecha del tratamiento (opcional; por defecto, la fecha actual; no puede ser futura)
 * @param weight         peso en kg medido en la consulta (obligatorio, entre 0.1 y 120; actualiza el peso de la mascota)
 * @param diagnosis      diagnóstico de la consulta (obligatorio, máx. 200 caracteres)
 * @param evolution      evolución (STABLE, IMPROVING o WORSENING). Obligatoria si la mascota tiene un
 *                       diagnóstico activo; null si llega estable (ingreso). Con STABLE el diagnóstico no cambia
 * @param notes          observaciones (opcional, máx. 1000 caracteres)
 * @param drugs          medicamentos a administrar (al menos uno: un tratamiento es formular medicamentos).
 *                       Se registran de forma atómica con el tratamiento
 */
public record TreatmentRequest(
        @Schema(description = "Id de la mascota", example = "1")
        @NotNull(message = "El ID de la mascota es obligatorio")
        Long petId,

        @Schema(description = "Id del veterinario", example = "1")
        @NotNull(message = "El ID del veterinario es obligatorio")
        Long veterinarianId,

        @Schema(description = "Fecha del tratamiento (por defecto, ahora; no puede ser futura)", example = "2026-10-12T10:00:00")
        @PastOrPresent(message = "La fecha del tratamiento no puede ser futura")
        LocalDateTime date,

        @Schema(description = "Peso en kg medido en la consulta", example = "12.4")
        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "0.1", message = "El peso debe ser de al menos 0.1 kg")
        @DecimalMax(value = "120", message = "El peso no puede superar los 120 kg")
        Double weight,

        @Schema(description = "Diagnóstico de la consulta", example = "Otitis externa")
        @NotBlank(message = "El diagnóstico es obligatorio")
        @Size(max = 200, message = "El diagnóstico no puede superar los 200 caracteres")
        String diagnosis,

        @Schema(description = "Evolución (null en el ingreso, cuando la mascota no tiene diagnóstico activo)", example = "IMPROVING")
        Evolution evolution,

        @Schema(description = "Observaciones (puede ser null)", example = "Menos inflamación que en la consulta anterior.")
        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
        String notes,

        @Schema(description = "Medicamentos a administrar (al menos uno)")
        @NotEmpty(message = "El tratamiento debe incluir al menos un medicamento")
        @Valid
        List<TreatmentDrugRequest> drugs
) { }
