package io.github.pethaven.service;

import io.github.pethaven.dto.request.AppointmentRequest;
import io.github.pethaven.dto.request.AppointmentCompletionRequest;
import io.github.pethaven.dto.request.AppointmentRescheduleRequest;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.request.ProvisionalPetRequest;
import io.github.pethaven.dto.request.VeterinarianAppointmentRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentOutcome;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.AppointmentMapper;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final String SLOT_TAKEN_MESSAGE = "La franja elegida ya no tiene veterinarios disponibles: elige otra.";
    private static final String VET_SLOT_TAKEN_MESSAGE = "Ya tienes una cita en esa franja: elige otra.";
    private static final String REASON_NOT_BOOKABLE_MESSAGE = "Ese motivo no se puede reservar desde la app.";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final ClinicSchedule clinicSchedule;
    private final OwnerService ownerService;
    private final PetService petService;
    private final VeterinarianService veterinarianService;
    private final VeterinarianRepository veterinarianRepository;
    private final TreatmentRepository treatmentRepository;

    @Autowired
    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            AppointmentMapper appointmentMapper,
            ClinicSchedule clinicSchedule,
            OwnerService ownerService,
            PetService petService,
            VeterinarianService veterinarianService,
            VeterinarianRepository veterinarianRepository,
            TreatmentRepository treatmentRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
        this.clinicSchedule = clinicSchedule;
        this.ownerService = ownerService;
        this.petService = petService;
        this.veterinarianService = veterinarianService;
        this.veterinarianRepository = veterinarianRepository;
        this.treatmentRepository = treatmentRepository;
    }

    @Override
    public Appointment getAppointmentEntity(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return appointmentMapper.toResponse(getAppointmentEntity(id));
    }

    // Una franja está libre si al menos un veterinario activo no tiene cita en ella.
    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityDayResponse> getAvailability() {
        LocalDate today = clinicSchedule.today();
        LocalDate lastDay = clinicSchedule.lastBookableDay();
        long activeVeterinarians = veterinarianRepository.countByActiveTrue();
        Map<LocalDateTime, Long> busyBySlot = appointmentRepository
                .findBusySlotStartsBetween(today.atStartOfDay(), lastDay.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<AvailabilityDayResponse> days = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(lastDay); date = date.plusDays(1)) {
            List<LocalTime> freeSlots = clinicSchedule.slotsOf(date).stream()
                    .filter(clinicSchedule::isWithinOwnerWindow)
                    .filter(start -> busyBySlot.getOrDefault(start, 0L) < activeVeterinarians)
                    .map(LocalDateTime::toLocalTime)
                    .toList();
            days.add(new AvailabilityDayResponse(date, clinicSchedule.isOpen(date), freeSlots));
        }
        return days;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByOwnerId(Long ownerId) {
        ownerService.getOwnerEntity(ownerId);
        return appointmentRepository.findByOwnerIdOrderByStartTimeAsc(ownerId).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(Long ownerId, AppointmentRequest request) {
        Owner owner = ownerService.getOwnerEntity(ownerId);
        LocalDateTime now = clinicSchedule.now();
        Appointment appointment = Appointment.builder()
                .owner(owner)
                .startTime(request.startTime())
                .ownerNotes(clean(request.ownerNotes()))
                .createdAt(now)
                .build();

        List<Long> preferredVeterinarians = new ArrayList<>();
        if (request.petId() != null) {
            Pet pet = getOwnedPet(ownerId, request.petId());
            ensureNotHospitalized(pet);
            if (request.reason() == null || !request.reason().isBookableByOwner()) {
                throw new IllegalOperationException(REASON_NOT_BOOKABLE_MESSAGE);
            }
            if (appointmentRepository.existsActiveForPet(pet.getId(), now)) {
                throw new IllegalOperationException(
                        "La mascota '" + pet.getName() + "' ya tiene una cita activa: reprográmala o cancélala.");
            }
            appointment.setPet(pet);
            appointment.setReason(request.reason());
            treatmentRepository.findFirstByPetIdOrderByDateDesc(pet.getId())
                    .ifPresent(last -> preferredVeterinarians.add(last.getVeterinarian().getId()));
        } else {
            if (request.reason() != null && request.reason() != AppointmentReason.FIRST_VISIT) {
                throw new IllegalOperationException(REASON_NOT_BOOKABLE_MESSAGE);
            }
            ProvisionalPetRequest provisional = request.provisionalPet();
            String name = provisional.name().trim();
            if (appointmentRepository.existsActiveForProvisionalPet(ownerId, name, now)) {
                throw new IllegalOperationException(
                        "La mascota '" + name + "' ya tiene una cita activa: reprográmala o cancélala.");
            }
            appointment.setProvisionalPetName(name);
            appointment.setProvisionalPetSpecies(provisional.species());
            appointment.setProvisionalPetBreed(clean(provisional.breed()));
            appointment.setProvisionalPetAge(provisional.age());
            appointment.setReason(AppointmentReason.FIRST_VISIT);
        }

        ensureOwnerCanBook(request.startTime());
        appointment.setVeterinarian(assignVeterinarian(request.startTime(), preferredVeterinarians, false));
        return appointmentMapper.toResponse(saveOccupyingSlot(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long id, AppointmentActor actor) {
        Appointment appointment = getAppointmentEntity(id);
        ensureScheduled(appointment, "cancelar");
        if (actor == AppointmentActor.OWNER) {
            ensureOwnerNotice(appointment, "cancelar");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setOccupiesSlot(null);
        appointment.setCancelledBy(actor);
        appointment.setCancelledAt(clinicSchedule.now());
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    // Reprogramar mueve la misma cita: conserva al veterinario si sigue libre en la nueva franja.
    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long id, AppointmentRescheduleRequest request, AppointmentActor actor) {
        Appointment appointment = getAppointmentEntity(id);
        ensureScheduled(appointment, "reprogramar");
        LocalDateTime newStart = request.startTime();
        if (actor == AppointmentActor.OWNER) {
            ensureOwnerNotice(appointment, "reprogramar");
            if (appointment.getPet() != null && !appointment.getReason().isForHospitalizedPet()) {
                ensureNotHospitalized(appointment.getPet());
            }
            ensureOwnerCanBook(newStart);
            if (request.ownerNotes() != null) {
                appointment.setOwnerNotes(clean(request.ownerNotes()));
            }
        } else {
            if (request.ownerNotes() != null) {
                throw new IllegalOperationException("Solo el dueño puede cambiar sus notas de la cita.");
            }
            ensureSlotStart(newStart);
            if (newStart.isBefore(clinicSchedule.now())) {
                throw new IllegalOperationException("La nueva hora de la cita ya pasó.");
            }
        }

        if (!newStart.equals(appointment.getStartTime())) {
            List<Long> preferredVeterinarians = new ArrayList<>();
            preferredVeterinarians.add(appointment.getVeterinarian().getId());
            // Las citas de mascotas hospitalizadas son del veterinario que las agendó.
            boolean keepVeterinarian = appointment.getReason().isForHospitalizedPet();
            if (!keepVeterinarian && appointment.getPet() != null) {
                treatmentRepository.findFirstByPetIdOrderByDateDesc(appointment.getPet().getId())
                        .ifPresent(last -> preferredVeterinarians.add(last.getVeterinarian().getId()));
            }
            appointment.setVeterinarian(assignVeterinarian(newStart, preferredVeterinarians, keepVeterinarian));
            appointment.setStartTime(newStart);
        }
        return appointmentMapper.toResponse(saveOccupyingSlot(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByVeterinarianId(Long veterinarianId, LocalDate from, LocalDate to) {
        veterinarianService.getVeterinarianEntity(veterinarianId);
        return appointmentRepository
                .findByVeterinarianIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        veterinarianId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    // Franjas del veterinario que aún no empiezan y que no tiene ocupadas; sin la ventana ni la antelación del dueño.
    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityDayResponse> getVeterinarianAvailability(Long veterinarianId, LocalDate from, LocalDate to) {
        veterinarianService.getVeterinarianEntity(veterinarianId);
        Set<LocalDateTime> busy = new HashSet<>(appointmentRepository.findBusySlotStartsOfVeterinarianBetween(
                veterinarianId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        LocalDateTime now = clinicSchedule.now();
        List<AvailabilityDayResponse> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<LocalTime> freeSlots = clinicSchedule.slotsOf(date).stream()
                    .filter(start -> !start.isBefore(now) && !busy.contains(start))
                    .map(LocalDateTime::toLocalTime)
                    .toList();
            days.add(new AvailabilityDayResponse(date, clinicSchedule.isOpen(date), freeSlots));
        }
        return days;
    }

    @Override
    @Transactional
    public AppointmentResponse scheduleForHospitalizedPet(Long veterinarianId, VeterinarianAppointmentRequest request) {
        Veterinarian veterinarian = veterinarianService.getVeterinarianEntity(veterinarianId);
        if (!veterinarian.isActive()) {
            throw new IllegalOperationException(
                    "El veterinario '" + veterinarian.getName() + "' no está activo: no puede agendar citas.");
        }
        if (!request.reason().isForHospitalizedPet()) {
            throw new IllegalOperationException("El veterinario solo agenda citas de alta o reuniones informativas.");
        }
        Pet pet = petService.getPetEntity(request.petId());
        if (!pet.isActive()) {
            throw new IllegalOperationException(
                    "La mascota '" + pet.getName() + "' no está hospitalizada: su dueño reserva sus citas.");
        }
        LocalDateTime start = request.startTime();
        ensureSlotStart(start);
        LocalDateTime now = clinicSchedule.now();
        if (start.isBefore(now)) {
            throw new IllegalOperationException("La hora de la cita ya pasó.");
        }
        if (appointmentRepository.existsByVeterinarianIdAndStartTimeAndOccupiesSlotTrue(veterinarianId, start)) {
            throw new IllegalOperationException(VET_SLOT_TAKEN_MESSAGE);
        }
        Appointment appointment = Appointment.builder()
                .owner(pet.getOwner())
                .pet(pet)
                .veterinarian(veterinarian)
                .startTime(start)
                .reason(request.reason())
                .createdAt(now)
                .build();
        return appointmentMapper.toResponse(saveOccupyingSlot(appointment, VET_SLOT_TAKEN_MESSAGE));
    }

    @Override
    @Transactional
    public AppointmentResponse startAppointment(Long id) {
        Appointment appointment = getAppointmentEntity(id);
        ensureScheduled(appointment, "iniciar");
        if (!appointment.getStartTime().toLocalDate().equals(clinicSchedule.today())) {
            throw new IllegalOperationException("Solo se puede iniciar una cita el día en que está programada.");
        }
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse markNoShow(Long id) {
        Appointment appointment = getAppointmentEntity(id);
        ensureScheduled(appointment, "marcar como no asistida");
        if (clinicSchedule.now().isBefore(appointment.getStartTime())) {
            throw new IllegalOperationException("Solo se puede marcar «No asistió» cuando ya pasó la hora de la cita.");
        }
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    // La mascota nueva queda en su casa (no hospitalizada): si la consulta termina en ingreso, se hospitaliza aparte.
    @Override
    @Transactional
    public AppointmentResponse registerProvisionalPet(Long id, PetRequest request) {
        Appointment appointment = getAppointmentEntity(id);
        if (appointment.getPet() != null) {
            throw new IllegalOperationException("La mascota de esta cita ya está registrada.");
        }
        ensureInProgress(appointment, "Registra la mascota durante la consulta: inicia la cita primero.");
        if (!appointment.getOwner().getId().equals(request.ownerId())) {
            throw new IllegalOperationException("La mascota debe quedar a nombre del dueño de la cita.");
        }
        Pet pet = petService.getPetEntity(petService.createPet(request).id());
        pet.setActive(false);
        appointment.setPet(pet);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Long id, AppointmentCompletionRequest request) {
        Appointment appointment = getAppointmentEntity(id);
        ensureInProgress(appointment, "Solo se puede completar una cita en curso.");
        String notes = clean(request.clinicalNotes());
        if (appointment.getReason().isForHospitalizedPet()) {
            completeHospitalizedPetAppointment(appointment, request, notes);
        } else {
            completeConsultation(appointment, request, notes);
        }
        appointment.setClinicalNotes(notes);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    // Alta / entrega: exige el alta médica y saca a la mascota de hospitalización. Reunión: exige la nota.
    private static void completeHospitalizedPetAppointment(
            Appointment appointment, AppointmentCompletionRequest request, String notes) {
        if (request.outcome() != null || request.treatmentId() != null) {
            throw new IllegalOperationException("Las citas de mascotas hospitalizadas no llevan resultado de consulta.");
        }
        Pet pet = appointment.getPet();
        if (appointment.getReason() == AppointmentReason.DISCHARGE) {
            if (pet.getDisease() != null) {
                throw new IllegalOperationException(
                        "La mascota '" + pet.getName() + "' aún no tiene el alta médica: dásela antes de entregarla.");
            }
            pet.setActive(false);
        } else if (notes == null) {
            throw new IllegalOperationException("Registra la nota clínica de la reunión.");
        }
    }

    private void completeConsultation(Appointment appointment, AppointmentCompletionRequest request, String notes) {
        if (appointment.getPet() == null) {
            throw new IllegalOperationException("Registra la mascota antes de completar la cita.");
        }
        AppointmentOutcome outcome = request.outcome();
        if (outcome == null) {
            throw new IllegalOperationException("Indica si la consulta fue ambulatoria o terminó en ingreso.");
        }
        if (outcome == AppointmentOutcome.AMBULATORY) {
            if (request.treatmentId() != null) {
                throw new IllegalOperationException("Una consulta ambulatoria no lleva tratamiento.");
            }
            if (notes == null) {
                throw new IllegalOperationException("Registra la nota clínica de la consulta.");
            }
        } else {
            appointment.setTreatment(getAdmissionTreatment(appointment, request.treatmentId()));
        }
        appointment.setOutcome(outcome);
    }

    private Treatment getAdmissionTreatment(Appointment appointment, Long treatmentId) {
        if (treatmentId == null) {
            throw new IllegalOperationException("Registra el tratamiento del ingreso antes de completar la cita.");
        }
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", treatmentId));
        if (!treatment.getPet().getId().equals(appointment.getPet().getId())) {
            throw new IllegalOperationException("El tratamiento no es de la mascota de la cita.");
        }
        if (treatment.getDate().toLocalDate().isBefore(appointment.getStartTime().toLocalDate())) {
            throw new IllegalOperationException("El tratamiento es anterior a la cita.");
        }
        if (appointmentRepository.existsByTreatmentId(treatmentId)) {
            throw new IllegalOperationException("El tratamiento ya está enlazado a otra cita.");
        }
        return treatment;
    }

    private Pet getOwnedPet(Long ownerId, Long petId) {
        Pet pet = petService.getPetEntity(petId);
        if (!pet.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Pet", "id", petId);
        }
        return pet;
    }

    private static void ensureNotHospitalized(Pet pet) {
        if (pet.isActive()) {
            throw new IllegalOperationException(
                    "La mascota '" + pet.getName() + "' está hospitalizada: el veterinario agenda sus citas.");
        }
    }

    private void ensureOwnerCanBook(LocalDateTime start) {
        ensureSlotStart(start);
        if (!clinicSchedule.isWithinOwnerWindow(start)) {
            throw new IllegalOperationException("Las citas se reservan con al menos " + clinicSchedule.minNoticeHours()
                    + " horas de antelación y hasta el " + clinicSchedule.lastBookableDay() + ".");
        }
    }

    private void ensureSlotStart(LocalDateTime start) {
        if (!clinicSchedule.isSlotStart(start)) {
            throw new IllegalOperationException("La hora elegida no corresponde a una franja del horario de atención.");
        }
    }

    private static void ensureInProgress(Appointment appointment, String message) {
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new IllegalOperationException(message);
        }
    }

    private static void ensureScheduled(Appointment appointment, String action) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalOperationException("Solo se puede " + action + " una cita programada.");
        }
    }

    private void ensureOwnerNotice(Appointment appointment, String action) {
        if (!clinicSchedule.hasOwnerNotice(appointment.getStartTime())) {
            throw new IllegalOperationException("Las citas se pueden " + action + " hasta " + clinicSchedule.minNoticeHours()
                    + " horas antes: comunícate con la clínica.");
        }
    }

    // Elige, entre los veterinarios activos libres en la franja, el primero de los preferidos que esté libre;
    // si ninguno lo está, cualquiera libre (salvo que deba ser uno de los preferidos).
    private Veterinarian assignVeterinarian(LocalDateTime start, List<Long> preferredIds, boolean onlyPreferred) {
        Set<Long> busy = new HashSet<>(appointmentRepository.findBusyActiveVeterinarianIds(start));
        List<Veterinarian> free = veterinarianRepository.findByActiveTrueOrderByIdAsc().stream()
                .filter(veterinarian -> !busy.contains(veterinarian.getId()))
                .toList();
        for (Long preferredId : preferredIds) {
            for (Veterinarian veterinarian : free) {
                if (Objects.equals(veterinarian.getId(), preferredId)) {
                    return veterinarian;
                }
            }
        }
        if (onlyPreferred) {
            throw new IllegalOperationException("El veterinario de la cita no tiene libre esa franja: elige otra.");
        }
        return free.stream().findFirst().orElseThrow(() -> new IllegalOperationException(SLOT_TAKEN_MESSAGE));
    }

    // La restricción única de la franja resuelve dos reservas simultáneas: la segunda falla al guardar.
    private Appointment saveOccupyingSlot(Appointment appointment) {
        return saveOccupyingSlot(appointment, SLOT_TAKEN_MESSAGE);
    }

    private Appointment saveOccupyingSlot(Appointment appointment, String slotTakenMessage) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalOperationException(slotTakenMessage);
        }
    }

    private static String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
