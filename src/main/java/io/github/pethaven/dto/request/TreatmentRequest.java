package io.github.pethaven.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TreatmentRequest(
        @NotNull(message = "El ID de la mascota es obligatorio")
        Long petId,

        @NotNull(message = "El ID del veterinario es obligatorio")
        Long veterinarianId,

        // Fecha es opcional, si no se proporciona se asignará la fecha actual
        LocalDateTime date
) { }
