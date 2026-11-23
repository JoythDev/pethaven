package io.github.pethaven.dto.request;

import io.github.pethaven.entity.AppointmentReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos con los que un dueño reserva una cita: para una mascota registrada ({@code petId}) o para una aún no
 * registrada ({@code provisionalPet}), nunca las dos.
 *
 * @param petId          id de una mascota del dueño que no esté hospitalizada
 * @param provisionalPet datos de una mascota aún no registrada (el motivo es siempre FIRST_VISIT)
 * @param reason         motivo (obligatorio con {@code petId}: CONTROL, VACCINATION, CONSULTATION, DEWORMING o NUTRITION)
 * @param startTime      inicio de la franja, en hora local de la clínica
 * @param ownerNotes     notas del dueño (opcional, máx. 500 caracteres)
 */
public record AppointmentRequest(
        @Schema(description = "Id de la mascota registrada (null si es una mascota nueva)", example = "1")
        Long petId,

        @Schema(description = "Mascota aún no registrada (null si se indica petId)")
        @Valid
        ProvisionalPetRequest provisionalPet,

        @Schema(description = "Motivo de la cita (se ignora con una mascota nueva)", example = "CONTROL")
        AppointmentReason reason,

        @Schema(description = "Inicio de la franja en hora local de la clínica", example = "2026-11-24T09:30:00")
        @NotNull(message = "La fecha y hora de la cita es obligatoria")
        LocalDateTime startTime,

        @Schema(description = "Notas del dueño (opcional)", example = "Cojea un poco de la pata trasera.")
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
        String ownerNotes
) {

    @Schema(hidden = true)
    @AssertTrue(message = "Indica una mascota registrada o los datos de una mascota nueva, pero no ambos")
    public boolean isPetChoiceValid() {
        return (petId == null) != (provisionalPet == null);
    }
}
