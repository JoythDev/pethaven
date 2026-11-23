package io.github.pethaven.service;

import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Asignación de veterinarios a las franjas: la usan la reserva, la reprogramación y la desactivación de un veterinario.
 */
@Component
public class AppointmentAssignment {

    static final String UNAVAILABLE_VETERINARIAN_REASON = "Veterinario no disponible";

    private final AppointmentRepository appointmentRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final TreatmentRepository treatmentRepository;
    private final ClinicSchedule clinicSchedule;

    public AppointmentAssignment(
            AppointmentRepository appointmentRepository,
            VeterinarianRepository veterinarianRepository,
            TreatmentRepository treatmentRepository,
            ClinicSchedule clinicSchedule
    ) {
        this.appointmentRepository = appointmentRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.treatmentRepository = treatmentRepository;
        this.clinicSchedule = clinicSchedule;
    }

    /** Id del último veterinario que atendió a la mascota, si tiene historial. */
    public Optional<Long> lastVeterinarianIdOf(Pet pet) {
        if (pet == null) {
            return Optional.empty();
        }
        return treatmentRepository.findFirstByPetIdOrderByDateDesc(pet.getId())
                .map(last -> last.getVeterinarian().getId());
    }

    /**
     * Entre los veterinarios activos libres en la franja, el primero de los preferidos que esté libre; si ninguno lo
     * está, el libre de menor id (salvo que deba ser uno de los preferidos).
     */
    public Optional<Veterinarian> pickFreeVeterinarian(LocalDateTime start, List<Long> preferredIds, boolean onlyPreferred) {
        Set<Long> busy = new HashSet<>(appointmentRepository.findBusyActiveVeterinarianIds(start));
        List<Veterinarian> free = veterinarianRepository.findByActiveTrueOrderByIdAsc().stream()
                .filter(Veterinarian::isActive)
                .filter(veterinarian -> !busy.contains(veterinarian.getId()))
                .toList();
        for (Long preferredId : preferredIds) {
            for (Veterinarian veterinarian : free) {
                if (Objects.equals(veterinarian.getId(), preferredId)) {
                    return Optional.of(veterinarian);
                }
            }
        }
        return onlyPreferred ? Optional.empty() : free.stream().findFirst();
    }

    /**
     * Libera las citas futuras programadas de un veterinario que se desactiva: cada una pasa a otro veterinario activo
     * libre en su franja o, si no hay ninguno, se cancela. Devuelve las citas que cambiaron.
     */
    public List<Appointment> releaseFutureAppointmentsOf(Veterinarian veterinarian) {
        LocalDateTime now = clinicSchedule.now();
        List<Appointment> changed = new ArrayList<>();
        for (Appointment appointment : appointmentRepository
                .findByVeterinarianIdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                        veterinarian.getId(), AppointmentStatus.SCHEDULED, now)) {
            List<Long> preferred = lastVeterinarianIdOf(appointment.getPet()).stream().toList();
            Optional<Veterinarian> replacement = pickFreeVeterinarian(appointment.getStartTime(), preferred, false)
                    .filter(candidate -> !candidate.getId().equals(veterinarian.getId()));
            if (replacement.isPresent()) {
                appointment.setVeterinarian(replacement.get());
            } else {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setOccupiesSlot(null);
                appointment.setCancelledBy(AppointmentActor.SYSTEM);
                appointment.setCancelledAt(now);
                appointment.setCancellationReason(UNAVAILABLE_VETERINARIAN_REASON);
            }
            changed.add(appointmentRepository.save(appointment));
        }
        return changed;
    }
}
