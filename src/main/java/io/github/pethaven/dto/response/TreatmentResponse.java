package io.github.pethaven.dto.response;

import io.github.pethaven.entity.Evolution;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tratamiento registrado, con su mascota, veterinario y medicamentos administrados.
 *
 * @param id               id del tratamiento
 * @param date             fecha del tratamiento
 * @param petId            id de la mascota tratada
 * @param petName          nombre de la mascota tratada
 * @param veterinarianId   id del veterinario que atendió
 * @param veterinarianName nombre del veterinario que atendió
 * @param weight           peso en kg medido en la consulta
 * @param diagnosis        diagnóstico de la consulta
 * @param evolution        evolución de la mascota (null = ingreso)
 * @param notes            observaciones (puede ser null)
 * @param drugs            medicamentos administrados (puede ser lista vacía)
 */
@Schema(description = "Tratamiento registrado")
public record TreatmentResponse(
        @Schema(description = "Id del tratamiento", example = "1")
        Long id,

        @Schema(description = "Fecha del tratamiento", example = "2026-10-12T10:00:00")
        LocalDateTime date,

        @Schema(description = "Id de la mascota", example = "1")
        Long petId,

        @Schema(description = "Nombre de la mascota", example = "Buddy")
        String petName,

        @Schema(description = "Id del veterinario", example = "1")
        Long veterinarianId,

        @Schema(description = "Nombre del veterinario", example = "Dra. Valentina Soler")
        String veterinarianName,

        @Schema(description = "Peso en kg medido en la consulta", example = "12.4")
        Double weight,

        @Schema(description = "Diagnóstico de la consulta", example = "Otitis externa")
        String diagnosis,

        @Schema(description = "Evolución de la mascota (null = ingreso)", example = "IMPROVING")
        Evolution evolution,

        @Schema(description = "Observaciones (puede ser null)", example = "Menos inflamación que en la consulta anterior.")
        String notes,

        @Schema(description = "Medicamentos administrados")
        List<TreatmentDrugResponse> drugs
) { }
