package io.github.pethaven.entity;

// Quién hizo un cambio en una cita. SYSTEM cubre los cambios automáticos (por ejemplo, al desactivar un veterinario).
public enum AppointmentActor {
    OWNER,
    VETERINARIAN,
    SYSTEM
}
