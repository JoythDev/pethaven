package io.github.pethaven.mapper;

import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.service.ClinicSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {

    private final ClinicSchedule clinicSchedule;

    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) return null;
        Pet pet = appointment.getPet();
        boolean provisional = pet == null;
        return new AppointmentResponse(
            appointment.getId(),
            appointment.getStartTime(),
            appointment.getStartTime().plus(clinicSchedule.slotDuration()),
            appointment.getReason(),
            appointment.getStatus(),
            provisional ? null : pet.getId(),
            provisional ? appointment.getProvisionalPetName() : pet.getName(),
            provisional ? appointment.getProvisionalPetSpecies() : pet.getSpecies(),
            provisional,
            provisional ? appointment.getProvisionalPetBreed() : null,
            provisional ? appointment.getProvisionalPetAge() : null,
            appointment.getOwner().getId(),
            appointment.getOwner().getName(),
            appointment.getOwner().getPhone(),
            appointment.getVeterinarian().getId(),
            appointment.getVeterinarian().getName(),
            appointment.getOwnerNotes(),
            appointment.getClinicalNotes(),
            appointment.getOutcome(),
            appointment.getTreatment() == null ? null : appointment.getTreatment().getId(),
            appointment.getCreatedAt(),
            appointment.getCancelledBy(),
            appointment.getCancelledAt(),
            appointment.getCancellationReason()
        );
    }
}
