package io.github.pethaven.service;

import io.github.pethaven.config.ClinicScheduleProperties;
import io.github.pethaven.dto.request.AppointmentRequest;
import io.github.pethaven.dto.request.AppointmentRescheduleRequest;
import io.github.pethaven.dto.request.ProvisionalPetRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.AppointmentMapper;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Pruebas del servicio con mocks: el horario y el mapper son reales; el reloj está fijo en el lunes 2026-11-23 a
// las 08:00 de Bogotá, así que la primera franja reservable por el dueño (2 h de antelación) es la de las 10:00.
@ExtendWith(MockitoExtension.class)
public class AppointmentServiceMockTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalDateTime MONDAY_10 = MONDAY.atTime(10, 0);
    private static final LocalDateTime TUESDAY_9 = MONDAY.plusDays(1).atTime(9, 0);
    private static final String SLOT_TAKEN_MESSAGE = "La franja elegida ya no tiene veterinarios disponibles: elige otra.";

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private OwnerService ownerService;
    @Mock
    private PetService petService;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private TreatmentRepository treatmentRepository;

    private AppointmentServiceImpl appointmentService;

    private Owner owner;
    private Pet pet;
    private Veterinarian vet1;
    private Veterinarian vet2;

    @BeforeEach
    public void setUp() {
        ClinicSchedule schedule = new ClinicSchedule(new ClinicScheduleProperties(
                ZoneId.of("America/Bogota"), 30,
                Map.of(DayOfWeek.MONDAY, "08:00-17:00", DayOfWeek.TUESDAY, "08:00-17:00",
                        DayOfWeek.WEDNESDAY, "08:00-17:00", DayOfWeek.THURSDAY, "08:00-17:00",
                        DayOfWeek.FRIDAY, "08:00-17:00", DayOfWeek.SATURDAY, "08:00-13:00"),
                14, 2),
                Clock.fixed(Instant.parse("2026-11-23T13:00:00Z"), ZoneId.of("UTC")));
        appointmentService = new AppointmentServiceImpl(appointmentRepository, new AppointmentMapper(schedule), schedule,
                ownerService, petService, veterinarianRepository, treatmentRepository);

        owner = RepositoryTestData.owner();
        owner.setId(1L);
        pet = RepositoryTestData.pet(owner);
        pet.setId(10L);
        pet.setActive(false);
        vet1 = RepositoryTestData.veterinarian();
        vet1.setId(21L);
        vet2 = RepositoryTestData.veterinarian();
        vet2.setId(22L);
    }

    // ---------- Helpers ----------

    private AppointmentRequest petRequest(AppointmentReason reason, LocalDateTime start) {
        return new AppointmentRequest(10L, null, reason, start, "  Cojea un poco  ");
    }

    private AppointmentRequest provisionalRequest(LocalDateTime start) {
        return new AppointmentRequest(null, new ProvisionalPetRequest(" Kira ", Species.CAT, " ", 2), null, start, null);
    }

    private Appointment scheduled(Long id, LocalDateTime start, Veterinarian veterinarian) {
        Appointment appointment = RepositoryTestData.appointment(owner, pet, veterinarian, start);
        appointment.setId(id);
        return appointment;
    }

    private void stubOwnerAndPet() {
        Mockito.when(ownerService.getOwnerEntity(1L)).thenReturn(owner);
        Mockito.when(petService.getPetEntity(10L)).thenReturn(pet);
    }

    private void stubActiveVets(List<Long> busyIds, LocalDateTime start) {
        Mockito.when(veterinarianRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(vet1, vet2));
        Mockito.when(appointmentRepository.findBusyActiveVeterinarianIds(start)).thenReturn(busyIds);
    }

    private void stubLastVet(Veterinarian veterinarian) {
        Treatment last = RepositoryTestData.treatment(pet, veterinarian, MONDAY.minusDays(3).atTime(9, 0), null);
        Mockito.when(treatmentRepository.findFirstByPetIdOrderByDateDesc(10L)).thenReturn(Optional.of(last));
    }

    private void stubSaveReturningArgument() {
        Mockito.when(appointmentRepository.saveAndFlush(ArgumentMatchers.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertConflict(Runnable action, String message) {
        Assertions.assertThatThrownBy(action::run)
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage(message);
    }

    // ---------- Disponibilidad ----------

    @Test
    public void getAvailability_FourteenDays_ClosedSundaysAndOwnerNoticeApplied() {
        Mockito.when(veterinarianRepository.countByActiveTrue()).thenReturn(2L);
        Mockito.when(appointmentRepository.findBusySlotStartsBetween(MONDAY.atStartOfDay(), MONDAY.plusDays(14).atStartOfDay()))
                .thenReturn(List.of());

        List<AvailabilityDayResponse> days = appointmentService.getAvailability();

        Assertions.assertThat(days).hasSize(14);
        Assertions.assertThat(days.getFirst().date()).isEqualTo(MONDAY);
        Assertions.assertThat(days.getLast().date()).isEqualTo(MONDAY.plusDays(13));
        // Hoy, con 2 h de antelación, la primera franja es la de las 10:00 y la última empieza a las 16:30.
        Assertions.assertThat(days.getFirst().freeSlots()).hasSize(14)
                .startsWith(LocalTime.of(10, 0)).endsWith(LocalTime.of(16, 30));
        Assertions.assertThat(days.get(1).freeSlots()).hasSize(18);
        Assertions.assertThat(days.get(5).freeSlots()).hasSize(10).endsWith(LocalTime.of(12, 30));
        AvailabilityDayResponse sunday = days.get(6);
        Assertions.assertThat(sunday.open()).isFalse();
        Assertions.assertThat(sunday.freeSlots()).isEmpty();
    }

    @Test
    public void getAvailability_SlotTakenByEveryActiveVet_IsNotFree() {
        Mockito.when(veterinarianRepository.countByActiveTrue()).thenReturn(2L);
        Mockito.when(appointmentRepository.findBusySlotStartsBetween(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(List.of(TUESDAY_9, TUESDAY_9, TUESDAY_9.plusMinutes(30)));

        List<LocalTime> tuesday = appointmentService.getAvailability().get(1).freeSlots();

        Assertions.assertThat(tuesday).doesNotContain(LocalTime.of(9, 0)).contains(LocalTime.of(9, 30)).hasSize(17);
    }

    @Test
    public void getAvailability_NoActiveVeterinarians_NoFreeSlots() {
        Mockito.when(veterinarianRepository.countByActiveTrue()).thenReturn(0L);
        Mockito.when(appointmentRepository.findBusySlotStartsBetween(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(List.of());

        Assertions.assertThat(appointmentService.getAvailability())
                .allSatisfy(day -> Assertions.assertThat(day.freeSlots()).isEmpty());
    }

    // ---------- Reservar ----------

    @Test
    public void bookAppointment_RegisteredPet_AssignsLastVetWhenFreeAndSavesTheBooking() {
        stubOwnerAndPet();
        stubLastVet(vet2);
        stubActiveVets(List.of(), TUESDAY_9);
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.bookAppointment(1L, petRequest(AppointmentReason.VACCINATION, TUESDAY_9));

        Assertions.assertThat(response.veterinarianId()).isEqualTo(22L);
        Assertions.assertThat(response.reason()).isEqualTo(AppointmentReason.VACCINATION);
        Assertions.assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        Assertions.assertThat(response.startTime()).isEqualTo(TUESDAY_9);
        Assertions.assertThat(response.endTime()).isEqualTo(TUESDAY_9.plusMinutes(30));
        Assertions.assertThat(response.petId()).isEqualTo(10L);
        Assertions.assertThat(response.provisionalPet()).isFalse();
        Assertions.assertThat(response.ownerNotes()).isEqualTo("Cojea un poco");
        Assertions.assertThat(response.createdAt()).isEqualTo(MONDAY.atTime(8, 0));
        Mockito.verify(appointmentRepository).saveAndFlush(ArgumentMatchers.argThat(saved -> saved.getOccupiesSlot()));
    }

    @Test
    public void bookAppointment_LastVetBusy_AssignsFirstFreeVet() {
        stubOwnerAndPet();
        stubLastVet(vet2);
        stubActiveVets(List.of(22L), TUESDAY_9);
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, TUESDAY_9));

        Assertions.assertThat(response.veterinarianId()).isEqualTo(21L);
    }

    @Test
    public void bookAppointment_EveryVetBusy_Returns409() {
        stubOwnerAndPet();
        stubActiveVets(List.of(21L, 22L), TUESDAY_9);

        assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, TUESDAY_9)),
                SLOT_TAKEN_MESSAGE);
    }

    @Test
    public void bookAppointment_SimultaneousBookingHitsUniqueSlot_Returns409() {
        stubOwnerAndPet();
        stubActiveVets(List.of(), TUESDAY_9);
        Mockito.when(appointmentRepository.saveAndFlush(ArgumentMatchers.any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("uk_appointment_veterinarian_slot"));

        assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, TUESDAY_9)),
                SLOT_TAKEN_MESSAGE);
    }

    @Test
    public void bookAppointment_HospitalizedPet_Returns409() {
        pet.setActive(true);
        stubOwnerAndPet();

        assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, TUESDAY_9)),
                "La mascota '" + pet.getName() + "' está hospitalizada: el veterinario agenda sus citas.");
    }

    @Test
    public void bookAppointment_PetOfAnotherOwner_Returns404() {
        Owner other = RepositoryTestData.owner();
        other.setId(2L);
        pet.setOwner(other);
        stubOwnerAndPet();

        Assertions.assertThatThrownBy(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, TUESDAY_9)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void bookAppointment_ReasonReservedForStaffOrMissing_Returns409() {
        stubOwnerAndPet();

        for (AppointmentReason reason : new AppointmentReason[]{AppointmentReason.DISCHARGE, AppointmentReason.BRIEFING,
                AppointmentReason.FIRST_VISIT, null}) {
            assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(reason, TUESDAY_9)),
                    "Ese motivo no se puede reservar desde la app.");
        }
    }

    @Test
    public void bookAppointment_PetWithActiveAppointment_Returns409() {
        stubOwnerAndPet();
        Mockito.when(appointmentRepository.existsActiveForPet(10L, MONDAY.atTime(8, 0))).thenReturn(true);

        assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, TUESDAY_9)),
                "La mascota '" + pet.getName() + "' ya tiene una cita activa: reprográmala o cancélala.");
    }

    @Test
    public void bookAppointment_TimeOutsideTheSchedule_Returns409() {
        stubOwnerAndPet();

        for (LocalDateTime start : new LocalDateTime[]{TUESDAY_9.plusMinutes(15), MONDAY.plusDays(6).atTime(9, 0),
                MONDAY.plusDays(5).atTime(13, 0), TUESDAY_9.minusHours(2)}) {
            assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, start)),
                    "La hora elegida no corresponde a una franja del horario de atención.");
        }
    }

    @Test
    public void bookAppointment_WithoutNoticeOrBeyondTheWindow_Returns409() {
        stubOwnerAndPet();
        String message = "Las citas se reservan con al menos 2 horas de antelación y hasta el 2026-12-06.";

        assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, MONDAY.atTime(9, 30))),
                message);
        assertConflict(() -> appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, MONDAY.plusDays(15).atTime(9, 0))),
                message);
    }

    @Test
    public void bookAppointment_EarliestAndLastSlotOfTheWindow_AreBookable() {
        stubOwnerAndPet();
        stubActiveVets(List.of(), MONDAY_10);
        stubActiveVets(List.of(), MONDAY.plusDays(12).atTime(12, 30));
        stubSaveReturningArgument();

        Assertions.assertThat(appointmentService.bookAppointment(1L, petRequest(AppointmentReason.CONTROL, MONDAY_10)).startTime())
                .isEqualTo(MONDAY_10);
        // El último día reservable (2026-12-06) es domingo; el sábado 2026-12-05 cierra a las 13:00.
        Assertions.assertThat(appointmentService.bookAppointment(1L,
                petRequest(AppointmentReason.CONTROL, MONDAY.plusDays(12).atTime(12, 30))).startTime())
                .isEqualTo(MONDAY.plusDays(12).atTime(12, 30));
    }

    @Test
    public void bookAppointment_ProvisionalPet_SavesItsDataAsFirstVisitWithoutPet() {
        Mockito.when(ownerService.getOwnerEntity(1L)).thenReturn(owner);
        stubActiveVets(List.of(), TUESDAY_9);
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.bookAppointment(1L, provisionalRequest(TUESDAY_9));

        Assertions.assertThat(response.reason()).isEqualTo(AppointmentReason.FIRST_VISIT);
        Assertions.assertThat(response.provisionalPet()).isTrue();
        Assertions.assertThat(response.petId()).isNull();
        Assertions.assertThat(response.petName()).isEqualTo("Kira");
        Assertions.assertThat(response.species()).isEqualTo(Species.CAT);
        Assertions.assertThat(response.provisionalBreed()).isNull();
        Assertions.assertThat(response.provisionalAge()).isEqualTo(2);
        Assertions.assertThat(response.veterinarianId()).isEqualTo(21L);
        Mockito.verifyNoInteractions(petService, treatmentRepository);
    }

    @Test
    public void bookAppointment_ProvisionalPetWithActiveAppointment_Returns409() {
        Mockito.when(ownerService.getOwnerEntity(1L)).thenReturn(owner);
        Mockito.when(appointmentRepository.existsActiveForProvisionalPet(1L, "Kira", MONDAY.atTime(8, 0))).thenReturn(true);

        assertConflict(() -> appointmentService.bookAppointment(1L, provisionalRequest(TUESDAY_9)),
                "La mascota 'Kira' ya tiene una cita activa: reprográmala o cancélala.");
    }

    @Test
    public void bookAppointment_ProvisionalPetWithAnotherReason_Returns409() {
        Mockito.when(ownerService.getOwnerEntity(1L)).thenReturn(owner);

        assertConflict(() -> appointmentService.bookAppointment(1L, new AppointmentRequest(null,
                        new ProvisionalPetRequest("Kira", Species.CAT, null, null), AppointmentReason.CONTROL, TUESDAY_9, null)),
                "Ese motivo no se puede reservar desde la app.");
    }

    // ---------- Cancelar ----------

    @Test
    public void cancelAppointment_OwnerWithNotice_ReleasesSlotAndRecordsWhoAndWhen() {
        Appointment appointment = scheduled(5L, MONDAY_10, vet1);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        Mockito.when(appointmentRepository.save(appointment)).thenReturn(appointment);

        AppointmentResponse response = appointmentService.cancelAppointment(5L, AppointmentActor.OWNER);

        Assertions.assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        Assertions.assertThat(response.cancelledBy()).isEqualTo(AppointmentActor.OWNER);
        Assertions.assertThat(response.cancelledAt()).isEqualTo(MONDAY.atTime(8, 0));
        Assertions.assertThat(appointment.getOccupiesSlot()).isNull();
    }

    @Test
    public void cancelAppointment_OwnerWithoutNotice_Returns409() {
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(scheduled(5L, MONDAY.atTime(9, 30), vet1)));

        assertConflict(() -> appointmentService.cancelAppointment(5L, AppointmentActor.OWNER),
                "Las citas se pueden cancelar hasta 2 horas antes: comunícate con la clínica.");
    }

    @Test
    public void cancelAppointment_VeterinarianWithoutNotice_IsAllowed() {
        Appointment appointment = scheduled(5L, MONDAY.atTime(8, 30), vet1);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        Mockito.when(appointmentRepository.save(appointment)).thenReturn(appointment);

        AppointmentResponse response = appointmentService.cancelAppointment(5L, AppointmentActor.VETERINARIAN);

        Assertions.assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        Assertions.assertThat(response.cancelledBy()).isEqualTo(AppointmentActor.VETERINARIAN);
    }

    @Test
    public void cancelAppointment_NotScheduled_Returns409() {
        Appointment appointment = scheduled(5L, TUESDAY_9, vet1);
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));

        assertConflict(() -> appointmentService.cancelAppointment(5L, AppointmentActor.VETERINARIAN),
                "Solo se puede cancelar una cita programada.");
    }

    @Test
    public void cancelAppointment_Missing_Returns404() {
        Mockito.when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> appointmentService.cancelAppointment(99L, AppointmentActor.OWNER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El recurso 'Appointment' con id: '99' no fue encontrado");
    }

    // ---------- Reprogramar ----------

    @Test
    public void rescheduleAppointment_OwnerToFreeSlot_KeepsVetAndUpdatesNotes() {
        Appointment appointment = scheduled(5L, TUESDAY_9, vet2);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        stubActiveVets(List.of(), TUESDAY_9.plusHours(1));
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.rescheduleAppointment(5L,
                new AppointmentRescheduleRequest(TUESDAY_9.plusHours(1), "Mejor más tarde"), AppointmentActor.OWNER);

        Assertions.assertThat(response.startTime()).isEqualTo(TUESDAY_9.plusHours(1));
        Assertions.assertThat(response.veterinarianId()).isEqualTo(22L);
        Assertions.assertThat(response.ownerNotes()).isEqualTo("Mejor más tarde");
        Assertions.assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    public void rescheduleAppointment_CurrentVetBusy_MovesToLastVetOfThePet() {
        Appointment appointment = scheduled(5L, TUESDAY_9, vet1);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        stubLastVet(vet2);
        stubActiveVets(List.of(21L), TUESDAY_9.plusHours(1));
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.rescheduleAppointment(5L,
                new AppointmentRescheduleRequest(TUESDAY_9.plusHours(1), null), AppointmentActor.OWNER);

        Assertions.assertThat(response.veterinarianId()).isEqualTo(22L);
    }

    @Test
    public void rescheduleAppointment_HospitalizedPetReasonAndVetBusy_Returns409() {
        Appointment appointment = scheduled(5L, TUESDAY_9, vet1);
        appointment.setReason(AppointmentReason.DISCHARGE);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        // El último veterinario de la mascota está libre, pero la cita de alta no puede pasar a él.
        Mockito.lenient().when(treatmentRepository.findFirstByPetIdOrderByDateDesc(10L)).thenReturn(Optional.of(
                RepositoryTestData.treatment(pet, vet2, MONDAY.minusDays(3).atTime(9, 0), null)));
        stubActiveVets(List.of(21L), TUESDAY_9.plusHours(1));

        assertConflict(() -> appointmentService.rescheduleAppointment(5L,
                        new AppointmentRescheduleRequest(TUESDAY_9.plusHours(1), null), AppointmentActor.VETERINARIAN),
                "El veterinario de la cita no tiene libre esa franja: elige otra.");
    }

    @Test
    public void rescheduleAppointment_VeterinarianWithoutNotice_KeepsOwnerNotes() {
        Appointment appointment = scheduled(5L, MONDAY.atTime(8, 30), vet1);
        appointment.setOwnerNotes("Notas originales");
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        stubActiveVets(List.of(), MONDAY.atTime(9, 0));
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.rescheduleAppointment(5L,
                new AppointmentRescheduleRequest(MONDAY.atTime(9, 0), null), AppointmentActor.VETERINARIAN);

        Assertions.assertThat(response.startTime()).isEqualTo(MONDAY.atTime(9, 0));
        Assertions.assertThat(response.ownerNotes()).isEqualTo("Notas originales");
    }

    @Test
    public void rescheduleAppointment_VeterinarianSendingOwnerNotes_Returns409() {
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(scheduled(5L, TUESDAY_9, vet1)));

        assertConflict(() -> appointmentService.rescheduleAppointment(5L,
                        new AppointmentRescheduleRequest(TUESDAY_9.plusHours(1), "Cambiadas"), AppointmentActor.VETERINARIAN),
                "Solo el dueño puede cambiar sus notas de la cita.");
    }

    @Test
    public void rescheduleAppointment_OwnerOfAPetHospitalizedAfterBooking_Returns409() {
        pet.setActive(true);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(scheduled(5L, TUESDAY_9, vet1)));

        assertConflict(() -> appointmentService.rescheduleAppointment(5L,
                        new AppointmentRescheduleRequest(TUESDAY_9.plusHours(1), null), AppointmentActor.OWNER),
                "La mascota '" + pet.getName() + "' está hospitalizada: el veterinario agenda sus citas.");
    }

    @Test
    public void rescheduleAppointment_VeterinarianToPastSlot_Returns409() {
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(scheduled(5L, TUESDAY_9, vet1)));

        assertConflict(() -> appointmentService.rescheduleAppointment(5L,
                        new AppointmentRescheduleRequest(MONDAY.atTime(8, 0).minusDays(2), null), AppointmentActor.VETERINARIAN),
                "La nueva hora de la cita ya pasó.");
    }

    @Test
    public void rescheduleAppointment_OwnerWithoutNotice_Returns409() {
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(scheduled(5L, MONDAY.atTime(9, 0), vet1)));

        assertConflict(() -> appointmentService.rescheduleAppointment(5L,
                        new AppointmentRescheduleRequest(TUESDAY_9, null), AppointmentActor.OWNER),
                "Las citas se pueden reprogramar hasta 2 horas antes: comunícate con la clínica.");
    }

    @Test
    public void rescheduleAppointment_SameSlot_DoesNotReassignTheVet() {
        Appointment appointment = scheduled(5L, TUESDAY_9, vet1);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        stubSaveReturningArgument();

        AppointmentResponse response = appointmentService.rescheduleAppointment(5L,
                new AppointmentRescheduleRequest(TUESDAY_9, "Solo cambio notas"), AppointmentActor.OWNER);

        Assertions.assertThat(response.veterinarianId()).isEqualTo(21L);
        Assertions.assertThat(response.ownerNotes()).isEqualTo("Solo cambio notas");
        Mockito.verifyNoInteractions(veterinarianRepository);
    }

    @Test
    public void rescheduleAppointment_Cancelled_Returns409() {
        Appointment appointment = scheduled(5L, TUESDAY_9, vet1);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));

        assertConflict(() -> appointmentService.rescheduleAppointment(5L,
                        new AppointmentRescheduleRequest(TUESDAY_9.plusHours(1), null), AppointmentActor.OWNER),
                "Solo se puede reprogramar una cita programada.");
    }

    // ---------- Consultas ----------

    @Test
    public void getAppointmentsByOwnerId_ChecksTheOwnerAndMapsEachAppointment() {
        Mockito.when(ownerService.getOwnerEntity(1L)).thenReturn(owner);
        Mockito.when(appointmentRepository.findByOwnerIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(scheduled(5L, TUESDAY_9, vet1), scheduled(6L, TUESDAY_9.plusDays(1), vet2)));

        List<AppointmentResponse> response = appointmentService.getAppointmentsByOwnerId(1L);

        Assertions.assertThat(response).extracting(AppointmentResponse::id).containsExactly(5L, 6L);
        Assertions.assertThat(response.getFirst().ownerName()).isEqualTo(owner.getName());
        Assertions.assertThat(response.getFirst().ownerPhone()).isEqualTo(owner.getPhone());
    }

    @Test
    public void getAppointmentsByOwnerId_UnknownOwner_PropagatesNotFound() {
        Mockito.when(ownerService.getOwnerEntity(99L)).thenThrow(new ResourceNotFoundException("Owner", "id", 99L));

        Assertions.assertThatThrownBy(() -> appointmentService.getAppointmentsByOwnerId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        Mockito.verifyNoInteractions(appointmentRepository);
    }

    @Test
    public void getAppointmentById_Exists_ReturnsIt() {
        Mockito.when(appointmentRepository.findById(5L)).thenReturn(Optional.of(scheduled(5L, TUESDAY_9, vet1)));

        Assertions.assertThat(appointmentService.getAppointmentById(5L).veterinarianName()).isEqualTo(vet1.getName());
    }
}
