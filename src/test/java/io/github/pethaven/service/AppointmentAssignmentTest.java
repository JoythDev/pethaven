package io.github.pethaven.service;

import io.github.pethaven.config.ClinicScheduleProperties;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.notification.AppointmentNotifier;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Reasignación al desactivar un veterinario (S16.11). Reloj fijo: lunes 2026-11-23 08:00 en Bogotá.
@ExtendWith(MockitoExtension.class)
public class AppointmentAssignmentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 8, 0);
    private static final LocalDateTime SLOT_A = NOW.plusDays(1).withHour(9);
    private static final LocalDateTime SLOT_B = NOW.plusDays(1).withHour(10);

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private AppointmentNotifier appointmentNotifier;

    private AppointmentAssignment assignment;

    private Owner owner;
    private Pet pet;
    private Veterinarian leaving;
    private Veterinarian vet2;
    private Veterinarian vet3;

    @BeforeEach
    public void setUp() {
        ClinicSchedule schedule = new ClinicSchedule(new ClinicScheduleProperties(ZoneId.of("America/Bogota"), 30,
                Map.of(DayOfWeek.TUESDAY, "08:00-17:00"), 14, 2),
                Clock.fixed(Instant.parse("2026-11-23T13:00:00Z"), ZoneId.of("UTC")));
        assignment = new AppointmentAssignment(appointmentRepository, veterinarianRepository, treatmentRepository, schedule,
                appointmentNotifier);
        owner = RepositoryTestData.owner();
        pet = RepositoryTestData.pet(owner);
        pet.setId(10L);
        leaving = vet(1L, false);
        vet2 = vet(2L, true);
        vet3 = vet(3L, true);
    }

    private static Veterinarian vet(Long id, boolean active) {
        Veterinarian veterinarian = RepositoryTestData.veterinarian();
        veterinarian.setId(id);
        veterinarian.setActive(active);
        return veterinarian;
    }

    private Appointment appointmentOf(Veterinarian veterinarian, LocalDateTime start) {
        return RepositoryTestData.appointment(owner, pet, veterinarian, start);
    }

    private void stubFutureAppointments(Appointment... appointments) {
        Mockito.when(appointmentRepository.findByVeterinarianIdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                1L, AppointmentStatus.SCHEDULED, NOW)).thenReturn(List.of(appointments));
        Mockito.when(appointmentRepository.save(ArgumentMatchers.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void releaseFutureAppointmentsOf_FreeVet_ReassignsKeepingTheAppointmentScheduled() {
        Appointment appointment = appointmentOf(leaving, SLOT_A);
        stubFutureAppointments(appointment);
        Mockito.when(veterinarianRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(vet2, vet3));
        Mockito.when(appointmentRepository.findBusyActiveVeterinarianIds(SLOT_A)).thenReturn(List.of(2L));

        List<Appointment> changed = assignment.releaseFutureAppointmentsOf(leaving);

        Assertions.assertThat(changed).containsExactly(appointment);
        Assertions.assertThat(appointment.getVeterinarian()).isSameAs(vet3);
        Assertions.assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        Assertions.assertThat(appointment.getOccupiesSlot()).isTrue();
        Mockito.verifyNoInteractions(appointmentNotifier);
    }

    @Test
    public void releaseFutureAppointmentsOf_PrefersTheLastVetOfThePet() {
        Appointment appointment = appointmentOf(leaving, SLOT_A);
        stubFutureAppointments(appointment);
        Mockito.when(treatmentRepository.findFirstByPetIdOrderByDateDesc(10L))
                .thenReturn(Optional.of(RepositoryTestData.treatment(pet, vet3, NOW.minusDays(5), null)));
        Mockito.when(veterinarianRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(vet2, vet3));

        assignment.releaseFutureAppointmentsOf(leaving);

        Assertions.assertThat(appointment.getVeterinarian()).isSameAs(vet3);
    }

    @Test
    public void releaseFutureAppointmentsOf_NoFreeVet_CancelsAsSystemWithTheReason() {
        Appointment appointment = appointmentOf(leaving, SLOT_B);
        stubFutureAppointments(appointment);
        // El veterinario que se desactiva aún figura como activo en la consulta: nunca se le reasigna su propia cita.
        Veterinarian stillListed = vet(1L, true);
        Mockito.when(veterinarianRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(stillListed, vet2));
        Mockito.when(appointmentRepository.findBusyActiveVeterinarianIds(SLOT_B)).thenReturn(List.of(2L));

        assignment.releaseFutureAppointmentsOf(leaving);

        Assertions.assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        Assertions.assertThat(appointment.getOccupiesSlot()).isNull();
        Assertions.assertThat(appointment.getCancelledBy()).isEqualTo(AppointmentActor.SYSTEM);
        Assertions.assertThat(appointment.getCancelledAt()).isEqualTo(NOW);
        Assertions.assertThat(appointment.getCancellationReason()).isEqualTo("Veterinario no disponible");
        Assertions.assertThat(appointment.getVeterinarian()).isSameAs(leaving);
        Mockito.verify(appointmentNotifier).cancelled(appointment);
    }

    @Test
    public void releaseFutureAppointmentsOf_NothingScheduled_ChangesNothing() {
        Mockito.when(appointmentRepository.findByVeterinarianIdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                1L, AppointmentStatus.SCHEDULED, NOW)).thenReturn(List.of());

        Assertions.assertThat(assignment.releaseFutureAppointmentsOf(leaving)).isEmpty();
        Mockito.verify(appointmentRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    public void pickFreeVeterinarian_OnlyPreferredAndBusy_IsEmpty() {
        Mockito.when(veterinarianRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(vet2, vet3));
        Mockito.when(appointmentRepository.findBusyActiveVeterinarianIds(SLOT_A)).thenReturn(List.of(2L));

        Assertions.assertThat(assignment.pickFreeVeterinarian(SLOT_A, List.of(2L), true)).isEmpty();
        Assertions.assertThat(assignment.pickFreeVeterinarian(SLOT_A, List.of(2L), false)).contains(vet3);
    }
}
