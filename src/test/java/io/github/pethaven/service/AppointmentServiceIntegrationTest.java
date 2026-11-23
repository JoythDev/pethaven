package io.github.pethaven.service;

import io.github.pethaven.dto.request.AppointmentCompletionRequest;
import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentOutcome;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.notification.AppointmentReminderScheduler;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Flujos completos de la consulta contra la base de datos: mascota provisional que termina en ingreso y alta / entrega.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AppointmentServiceIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private PetService petService;
    @Autowired
    private TreatmentService treatmentService;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private VeterinarianService veterinarianService;
    @Autowired
    private ClinicSchedule clinicSchedule;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    @Autowired
    private DrugRepository drugRepository;
    @Autowired
    private AppointmentReminderScheduler reminderScheduler;
    @Autowired
    private EntityManager entityManager;

    private Owner owner;
    private Veterinarian vet;

    @BeforeEach
    public void setUp() {
        Long ownerId = ownerService.createOwner(
                new OwnerRequest("555000555", "Integration Owner", "appointment.owner@pethaven.test", "password123", "3001234567")).id();
        owner = ownerRepository.findById(ownerId).orElseThrow();
        Long vetId = veterinarianService.createVeterinarian(
                new VeterinarianRequest("666000666", "Integration Vet", "appointment.vet@pethaven.test", "password123", "General", null)).id();
        vet = veterinarianRepository.findById(vetId).orElseThrow();
    }

    // Cita de hoy en curso: iniciar exige que sea del día, así que se fija a hoy sin pasar por el horario.
    private Appointment inProgressToday(Pet pet, AppointmentReason reason) {
        Appointment appointment = RepositoryTestData.appointment(owner, pet, vet, clinicSchedule.today().atTime(8, 0));
        appointment.setReason(reason);
        appointment = appointmentRepository.save(appointment);
        appointmentService.startAppointment(appointment.getId());
        return appointment;
    }

    @Test
    public void provisionalPetConsultation_RegisteredThenAdmitted_LinksPetAndTreatment() {
        Appointment appointment = inProgressToday(null, AppointmentReason.FIRST_VISIT);
        appointment.setProvisionalPetName("Kira");
        appointment.setProvisionalPetSpecies(Species.CAT);

        AppointmentResponse registered = appointmentService.registerProvisionalPet(appointment.getId(),
                new PetRequest("Kira", owner.getId(), Species.CAT, "Criollo", 2, 3.4, null));
        Pet pet = petRepository.findById(registered.petId()).orElseThrow();
        Assertions.assertThat(pet.isActive()).isFalse();
        Assertions.assertThat(pet.getOwner().getId()).isEqualTo(owner.getId());

        // Ingreso: el veterinario hospitaliza a la mascota y registra su tratamiento antes de cerrar la cita.
        petService.switchPetActiveStatus(pet.getId(), true);
        Drug drug = drugRepository.save(Drug.builder().name("Integration drug " + System.nanoTime())
                .purchasePrice(new BigDecimal("1000.00")).salePrice(new BigDecimal("2000.00"))
                .unitsAvailable(10).unitsSold(0).build());
        Long treatmentId = treatmentService.createTreatment(new TreatmentRequest(pet.getId(), vet.getId(), null, 3.4,
                "Gastroenteritis", null, null, List.of(new TreatmentDrugRequest(drug.getId(), 1)))).id();

        AppointmentResponse completed = appointmentService.completeAppointment(appointment.getId(),
                new AppointmentCompletionRequest(AppointmentOutcome.ADMISSION, treatmentId, "Ingresa por deshidratación"));

        Assertions.assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
        Assertions.assertThat(completed.outcome()).isEqualTo(AppointmentOutcome.ADMISSION);
        Assertions.assertThat(completed.treatmentId()).isEqualTo(treatmentId);
        Assertions.assertThat(completed.petId()).isEqualTo(pet.getId());
        Assertions.assertThat(appointmentRepository.findById(appointment.getId()).orElseThrow().getProvisionalPetName())
                .isEqualTo("Kira");
    }

    @Test
    public void dischargeAppointment_AfterMedicalDischarge_TakesThePetOutOfHospital() {
        Pet pet = petRepository.save(RepositoryTestData.pet(owner));
        Appointment appointment = inProgressToday(pet, AppointmentReason.DISCHARGE);

        appointmentService.completeAppointment(appointment.getId(), new AppointmentCompletionRequest(null, null, null));

        Assertions.assertThat(petRepository.findById(pet.getId()).orElseThrow().isActive()).isFalse();
        Assertions.assertThat(appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    public void deactivatingAVet_ReassignsFutureAppointmentsOrCancelsWhenNoVetIsFree() {
        Long otherVetId = veterinarianService.createVeterinarian(
                new VeterinarianRequest("777000777", "Other Vet", "appointment.vet2@pethaven.test", "password123", "General", null)).id();
        Veterinarian otherVet = veterinarianRepository.findById(otherVetId).orElseThrow();
        Pet pet = petRepository.save(RepositoryTestData.pet(owner));
        LocalDateTime freeSlot = clinicSchedule.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fullSlot = freeSlot.plusHours(1);
        LocalDateTime past = clinicSchedule.now().minusDays(1).withSecond(0).withNano(0);
        Appointment toReassign = appointmentRepository.save(RepositoryTestData.appointment(owner, pet, vet, freeSlot));
        Appointment toCancel = appointmentRepository.save(RepositoryTestData.appointment(owner, pet, vet, fullSlot));
        Appointment pastOne = appointmentRepository.save(RepositoryTestData.appointment(owner, pet, vet, past));
        // El otro veterinario ya está ocupado en la segunda franja.
        appointmentRepository.save(RepositoryTestData.appointment(owner, null, otherVet, fullSlot));
        // Los demás veterinarios activos del contexto compartido también podrían estar libres: se excluyen de la prueba.
        veterinarianRepository.findByActiveTrueOrderByIdAsc().stream()
                .filter(other -> !other.getId().equals(vet.getId()) && !other.getId().equals(otherVetId))
                .forEach(other -> other.setActive(false));

        veterinarianService.switchVeterinarianActiveStatus(vet.getId(), false);

        Appointment reassigned = appointmentRepository.findById(toReassign.getId()).orElseThrow();
        Assertions.assertThat(reassigned.getVeterinarian().getId()).isEqualTo(otherVetId);
        Assertions.assertThat(reassigned.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        Appointment cancelled = appointmentRepository.findById(toCancel.getId()).orElseThrow();
        Assertions.assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        Assertions.assertThat(cancelled.getCancelledBy()).isEqualTo(AppointmentActor.SYSTEM);
        Assertions.assertThat(cancelled.getCancellationReason()).isEqualTo("Veterinario no disponible");
        Assertions.assertThat(cancelled.getOccupiesSlot()).isNull();
        Assertions.assertThat(appointmentRepository.findById(pastOne.getId()).orElseThrow().getVeterinarian().getId())
                .isEqualTo(vet.getId());
    }

    @Test
    public void reminderScheduler_MarksTomorrowsAppointmentsOnlyOnce() {
        Pet pet = petRepository.save(RepositoryTestData.pet(owner));
        Appointment tomorrow = appointmentRepository.save(RepositoryTestData.appointment(
                owner, pet, vet, clinicSchedule.today().plusDays(1).atTime(9, 0)));

        Assertions.assertThat(reminderScheduler.sendTomorrowReminders()).isGreaterThanOrEqualTo(1);
        // La marca es una actualización directa en la base: se relee sin la copia en memoria.
        entityManager.clear();
        Assertions.assertThat(appointmentRepository.findById(tomorrow.getId()).orElseThrow().getReminderSentAt()).isNotNull();
        Assertions.assertThat(reminderScheduler.sendTomorrowReminders()).isZero();
    }
}
