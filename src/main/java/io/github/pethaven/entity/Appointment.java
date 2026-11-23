package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "appointments", uniqueConstraints = @UniqueConstraint(
        name = "uk_appointment_veterinarian_slot",
        columnNames = {"veterinarian_id", "start_time", "occupies_slot"}))
@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null mientras la mascota no esté registrada: la cita lleva entonces sus datos provisionales.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pet pet;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Owner owner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private Veterinarian veterinarian;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 20, nullable = false)
    private AppointmentReason reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    // TRUE mientras la cita ocupa la franja del veterinario y null cuando la libera (cancelada). La restricción
    // única ignora los null, así que impide dos citas en la misma franja sin bloquear las canceladas.
    @Builder.Default
    @Column(name = "occupies_slot", nullable = true)
    private Boolean occupiesSlot = Boolean.TRUE;

    @Column(name = "provisional_pet_name", length = 60, nullable = true)
    private String provisionalPetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provisional_pet_species", length = 10, nullable = true)
    private Species provisionalPetSpecies;

    @Column(name = "provisional_pet_breed", length = 60, nullable = true)
    private String provisionalPetBreed;

    @Column(name = "provisional_pet_age", nullable = true)
    private Integer provisionalPetAge;

    @Column(name = "owner_notes", length = 500, nullable = true)
    private String ownerNotes;

    @Column(name = "clinical_notes", length = 1000, nullable = true)
    private String clinicalNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20, nullable = true)
    private AppointmentOutcome outcome;

    // Tratamiento del ingreso, cuando la consulta terminó hospitalizando a la mascota.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_id", nullable = true)
    private Treatment treatment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 20, nullable = true)
    private AppointmentActor cancelledBy;

    @Column(name = "cancelled_at", nullable = true)
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 200, nullable = true)
    private String cancellationReason;

    // Bloqueo optimista: dos cambios simultáneos de la misma cita (cancelar y reprogramar) no se pisan.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
