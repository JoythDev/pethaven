package io.github.pethaven.repository;

import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

@DataJpaTest
public class AppointmentRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 8, 0);
    private static final LocalDateTime SLOT = LocalDateTime.of(2026, 11, 24, 9, 0);

    private final AppointmentRepository appointmentRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;

    private Owner owner;
    private Pet pet;
    private Veterinarian vet;
    private Veterinarian inactiveVet;

    @Autowired
    public AppointmentRepositoryTest(
            AppointmentRepository appointmentRepository,
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VeterinarianRepository veterinarianRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.veterinarianRepository = veterinarianRepository;
    }

    @BeforeEach
    public void setUp() {
        owner = ownerRepository.save(RepositoryTestData.owner());
        pet = petRepository.save(RepositoryTestData.pet(owner));
        vet = veterinarianRepository.save(RepositoryTestData.veterinarian());
        inactiveVet = RepositoryTestData.veterinarian();
        inactiveVet.setActive(false);
        inactiveVet = veterinarianRepository.save(inactiveVet);
    }

    private Appointment save(Veterinarian veterinarian, LocalDateTime start, AppointmentStatus status) {
        Appointment appointment = RepositoryTestData.appointment(owner, pet, veterinarian, start);
        appointment.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) {
            appointment.setOccupiesSlot(null);
        }
        return appointmentRepository.saveAndFlush(appointment);
    }

    @Test
    public void save_SecondAppointmentInTheSameSlotOfTheSameVet_IsRejected() {
        save(vet, SLOT, AppointmentStatus.SCHEDULED);

        Assertions.assertThatThrownBy(() -> save(vet, SLOT, AppointmentStatus.SCHEDULED))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void save_CancelledAppointmentsInTheSameSlot_DoNotBlockANewBooking() {
        save(vet, SLOT, AppointmentStatus.CANCELLED);
        save(vet, SLOT, AppointmentStatus.CANCELLED);

        Assertions.assertThat(save(vet, SLOT, AppointmentStatus.SCHEDULED).getId()).isNotNull();
    }

    @Test
    public void findBusyActiveVeterinarianIds_OnlyActiveVetsWithAnOccupiedSlot() {
        save(vet, SLOT, AppointmentStatus.SCHEDULED);
        save(inactiveVet, SLOT, AppointmentStatus.SCHEDULED);

        Assertions.assertThat(appointmentRepository.findBusyActiveVeterinarianIds(SLOT)).containsExactly(vet.getId());
        Assertions.assertThat(appointmentRepository.findBusyActiveVeterinarianIds(SLOT.plusMinutes(30))).isEmpty();
    }

    @Test
    public void findBusySlotStartsBetween_IgnoresCancelledAndOutOfRange() {
        save(vet, SLOT, AppointmentStatus.SCHEDULED);
        save(vet, SLOT.plusMinutes(30), AppointmentStatus.CANCELLED);
        save(vet, SLOT.plusDays(1), AppointmentStatus.SCHEDULED);

        Assertions.assertThat(appointmentRepository.findBusySlotStartsBetween(SLOT.toLocalDate().atStartOfDay(),
                SLOT.toLocalDate().plusDays(1).atStartOfDay())).containsExactly(SLOT);
    }

    @Test
    public void existsActiveForPet_ScheduledInTheFutureOrInProgress() {
        Assertions.assertThat(appointmentRepository.existsActiveForPet(pet.getId(), NOW)).isFalse();

        save(vet, NOW.minusDays(1), AppointmentStatus.SCHEDULED);
        save(vet, SLOT, AppointmentStatus.CANCELLED);
        Assertions.assertThat(appointmentRepository.existsActiveForPet(pet.getId(), NOW)).isFalse();

        save(vet, SLOT.plusHours(1), AppointmentStatus.SCHEDULED);
        Assertions.assertThat(appointmentRepository.existsActiveForPet(pet.getId(), NOW)).isTrue();
    }

    @Test
    public void existsActiveForProvisionalPet_MatchesOwnerAndNameIgnoringCase() {
        Appointment provisional = RepositoryTestData.appointment(owner, null, vet, SLOT);
        provisional.setProvisionalPetName("Kira");
        provisional.setProvisionalPetSpecies(Species.CAT);
        appointmentRepository.saveAndFlush(provisional);

        Assertions.assertThat(appointmentRepository.existsActiveForProvisionalPet(owner.getId(), "kira", NOW)).isTrue();
        Assertions.assertThat(appointmentRepository.existsActiveForProvisionalPet(owner.getId(), "Luna", NOW)).isFalse();
        Assertions.assertThat(appointmentRepository.existsActiveForPet(pet.getId(), NOW)).isFalse();
    }

    @Test
    public void veterinarianAgendaQueries_OnlyThatVetAndRange() {
        Appointment inRange = save(vet, SLOT, AppointmentStatus.SCHEDULED);
        Appointment cancelled = save(vet, SLOT.plusMinutes(30), AppointmentStatus.CANCELLED);
        save(vet, SLOT.plusDays(1), AppointmentStatus.SCHEDULED);
        save(inactiveVet, SLOT, AppointmentStatus.SCHEDULED);
        LocalDateTime from = SLOT.toLocalDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        Assertions.assertThat(appointmentRepository
                        .findByVeterinarianIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(vet.getId(), from, to))
                .extracting(Appointment::getId).containsExactly(inRange.getId(), cancelled.getId());
        Assertions.assertThat(appointmentRepository.findBusySlotStartsOfVeterinarianBetween(vet.getId(), from, to))
                .containsExactly(SLOT);
        Assertions.assertThat(appointmentRepository.existsByVeterinarianIdAndStartTimeAndOccupiesSlotTrue(vet.getId(), SLOT)).isTrue();
        Assertions.assertThat(appointmentRepository.existsByVeterinarianIdAndStartTimeAndOccupiesSlotTrue(
                vet.getId(), SLOT.plusMinutes(30))).isFalse();
    }

    @Test
    public void reminderQuery_ScheduledOfThatDayWithoutReminder() {
        Appointment pending = save(vet, SLOT, AppointmentStatus.SCHEDULED);
        Appointment reminded = save(vet, SLOT.plusMinutes(30), AppointmentStatus.SCHEDULED);
        reminded.setReminderSentAt(NOW);
        appointmentRepository.saveAndFlush(reminded);
        save(vet, SLOT.plusHours(1), AppointmentStatus.CANCELLED);
        save(vet, SLOT.plusDays(1), AppointmentStatus.SCHEDULED);
        LocalDateTime day = SLOT.toLocalDate().atStartOfDay();

        Assertions.assertThat(appointmentRepository.findByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanAndReminderSentAtIsNull(
                        AppointmentStatus.SCHEDULED, day, day.plusDays(1)))
                .extracting(Appointment::getId).containsExactly(pending.getId());
    }

    @Test
    public void markReminderSent_OnlyOncePerScheduledAppointment() {
        Appointment scheduled = save(vet, SLOT, AppointmentStatus.SCHEDULED);
        Appointment cancelled = save(vet, SLOT.plusMinutes(30), AppointmentStatus.CANCELLED);

        Assertions.assertThat(appointmentRepository.markReminderSent(scheduled.getId(), NOW)).isEqualTo(1);
        Assertions.assertThat(appointmentRepository.markReminderSent(scheduled.getId(), NOW)).isZero();
        Assertions.assertThat(appointmentRepository.markReminderSent(cancelled.getId(), NOW)).isZero();
    }

    @Test
    public void findByOwnerIdOrderByStartTimeAsc_OrdersTheOwnersAppointmentsByStart() {
        Appointment later = save(vet, SLOT.plusDays(2), AppointmentStatus.SCHEDULED);
        Appointment earlier = save(vet, SLOT, AppointmentStatus.CANCELLED);

        Assertions.assertThat(appointmentRepository.findByOwnerIdOrderByStartTimeAsc(owner.getId()))
                .extracting(Appointment::getId).containsExactly(earlier.getId(), later.getId());
    }
}
