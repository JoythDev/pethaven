package io.github.pethaven.entity;

// Motivo de una cita. El dueño reserva consultas ambulatorias; el veterinario agenda las citas de
// mascotas hospitalizadas; FIRST_VISIT se asigna solo a una mascota aún no registrada.
public enum AppointmentReason {
    CONTROL,
    VACCINATION,
    CONSULTATION,
    DEWORMING,
    NUTRITION,
    FIRST_VISIT,
    DISCHARGE,
    BRIEFING;

    public boolean isBookableByOwner() {
        return this != FIRST_VISIT && !isForHospitalizedPet();
    }

    public boolean isForHospitalizedPet() {
        return this == DISCHARGE || this == BRIEFING;
    }
}
