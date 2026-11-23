package io.github.pethaven;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
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
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.service.ClinicSchedule;
import io.github.pethaven.service.TreatmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Guion de demostración del agendamiento (S16.14): mascotas fijas para las cuentas de demo y unas 20 citas
 * calculadas desde el arranque, así cada flujo del dueño y del veterinario se puede mostrar de punta a punta.
 * Se guardan directamente en la base, sin avisos por correo. No se ejecuta con el perfil "test".
 */
@Component
@Profile("!test")
@Order(2)
@Transactional
public class DemoAppointmentLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAppointmentLoader.class);

    // Documentos de los perfiles de DataLoader: el correo de las cuentas de demo puede cambiar por configuración.
    private static final String JOHN_DOCUMENT = "123456789";
    private static final String JANE_DOCUMENT = "987654321";
    private static final String VALENTINA_DOCUMENT = "328146627";
    private static final List<String> OTHER_VET_DOCUMENTS = List.of("241882914", "581736412", "522124823", "192049284");

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DrugRepository drugRepository;

    @Autowired
    private TreatmentService treatmentService;

    @Autowired
    private ClinicSchedule clinicSchedule;

    private LocalDateTime now;

    @Override
    public void run(String... args) {
        Optional<Owner> john = ownerRepository.findByDocument(JOHN_DOCUMENT);
        Optional<Owner> jane = ownerRepository.findByDocument(JANE_DOCUMENT);
        Optional<Veterinarian> valentina = veterinarianRepository.findByDocument(VALENTINA_DOCUMENT);
        if (john.isEmpty() || jane.isEmpty() || valentina.isEmpty()) {
            log.warn("Faltan las cuentas de demostración: no se cargaron citas de ejemplo.");
            return;
        }
        now = clinicSchedule.now();
        long before = appointmentRepository.count();
        loadDemoScript(john.get(), jane.get(), valentina.get());
        loadFiller(john.get(), jane.get());
        log.info("Citas de ejemplo cargadas: {}", appointmentRepository.count() - before);
    }

    // Cuentas de demo: cada flujo del guion tiene su cita (ver L40-L42 del documento de la funcionalidad).
    private void loadDemoScript(Owner john, Owner jane, Veterinarian valentina) {
        Pet rex = pet("Rex", Species.DOG, "Labrador Retriever", 4, 24.5, john, false, null, "https://placedog.net/500/400?id=26");
        Pet lola = pet("Lola", Species.CAT, "Siamés", 3, 3.6, john, false, null,
                "https://images.unsplash.com/photo-1592194996308-7b43878e84a6?fm=jpg&q=60&w=800&auto=format&fit=crop");
        pet("Bimba", Species.DOG, "Beagle", 2, 9.8, john, false, null, "https://placedog.net/500/400?id=27");
        Pet coco = pet("Coco", Species.CAT, "Persa", 5, 4.1, jane, true, null,
                "https://images.unsplash.com/photo-1495360010541-f48722b34f7d?fm=jpg&q=60&w=800&auto=format&fit=crop");
        Pet pelusa = pet("Pelusa", Species.CAT, "Angora Turco", 2, 3.4, jane, true, null,
                "https://images.unsplash.com/photo-1543852786-1cf6624b9987?fm=jpg&q=60&w=800&auto=format&fit=crop");
        loadHospitalHistory(coco, pelusa, valentina);

        // Agenda del día de demostración de la Dra. Valentina Soler.
        List<LocalDateTime> demoSlots = demoDaySlots();
        save(scheduled(john, rex, valentina, demoSlots.get(0), AppointmentReason.CONSULTATION, "Cojea un poco de la pata trasera."));
        Appointment provisional = scheduled(jane, null, valentina, demoSlots.get(1), AppointmentReason.FIRST_VISIT,
                "Lo adopté la semana pasada.");
        provisional.setProvisionalPetName("Milo");
        provisional.setProvisionalPetSpecies(Species.DOG);
        provisional.setProvisionalPetBreed("Mestizo");
        provisional.setProvisionalPetAge(1);
        save(provisional);
        save(scheduled(jane, coco, valentina, demoSlots.get(2), AppointmentReason.DISCHARGE, null));
        save(scheduled(jane, pelusa, valentina, openDay(demoSlots.get(0).toLocalDate(), 1).atTime(10, 0),
                AppointmentReason.BRIEFING, null));

        // Lola: historial con una consulta completada y una próxima cita para reprogramar o cancelar.
        save(completed(john, lola, valentina, openDay(now.toLocalDate(), -7).atTime(9, 0), AppointmentReason.VACCINATION,
                "Vacuna triple felina aplicada; sin reacciones. Refuerzo en un año."));
        save(scheduled(john, lola, valentina, openDay(now.toLocalDate(), 3).atTime(11, 0), AppointmentReason.DEWORMING, null));
    }

    // Coco ya tiene el alta médica (lista para la entrega) y Pelusa sigue en tratamiento, con su historia clínica.
    private void loadHospitalHistory(Pet coco, Pet pelusa, Veterinarian valentina) {
        Optional<Drug> drug = drugRepository.findByUnitsAvailableGreaterThan(0).stream().findFirst();
        if (drug.isEmpty()) {
            pelusa.setDisease("Gastroenteritis");
            return;
        }
        List<TreatmentDrugRequest> oneUnit = List.of(new TreatmentDrugRequest(drug.get().getId(), 1));
        treatmentService.createTreatment(new TreatmentRequest(coco.getId(), valentina.getId(), now.minusDays(3), 4.1,
                "Infección urinaria", null, "Ingreso: se inicia tratamiento.", oneUnit));
        treatmentService.dischargePet(coco.getId(), valentina.getId());
        treatmentService.createTreatment(new TreatmentRequest(pelusa.getId(), valentina.getId(), now.minusDays(1), 3.4,
                "Gastroenteritis", null, "Ingreso: se inicia tratamiento.", oneUnit));
    }

    // Relleno para los demás veterinarios: citas pasadas en cada estado de cierre y próximas en la semana.
    private void loadFiller(Owner john, Owner jane) {
        List<Veterinarian> vets = OTHER_VET_DOCUMENTS.stream()
                .map(veterinarianRepository::findByDocument)
                .flatMap(Optional::stream)
                .toList();
        List<Pet> homePets = petRepository.findAll().stream()
                .filter(pet -> !pet.isActive())
                .filter(pet -> !pet.getOwner().getId().equals(john.getId()) && !pet.getOwner().getId().equals(jane.getId()))
                .toList();
        if (vets.isEmpty() || homePets.isEmpty()) {
            return;
        }
        AppointmentReason[] reasons = {AppointmentReason.CONTROL, AppointmentReason.VACCINATION, AppointmentReason.CONSULTATION,
                AppointmentReason.DEWORMING, AppointmentReason.NUTRITION};

        // Una cita de hoy (o del último día hábil) que ya empezó y nadie atendió: para «No asistió».
        Pet late = homePets.getFirst();
        Veterinarian valentina = veterinarianRepository.findByDocument(VALENTINA_DOCUMENT).orElseThrow();
        lastSlotBeforeNow().ifPresent(start ->
                save(scheduled(late.getOwner(), late, valentina, start, AppointmentReason.CONTROL, null)));

        for (int i = 0; i < 7; i++) {
            Pet pet = homePets.get(i % homePets.size());
            Veterinarian vet = vets.get(i % vets.size());
            LocalDateTime start = openDay(now.toLocalDate(), -(1 + i % 5)).atTime(8, 30).plusMinutes(30L * i);
            AppointmentReason reason = reasons[i % reasons.length];
            if (i < 4) {
                save(completed(pet.getOwner(), pet, vet, start, reason, "Consulta sin novedades; seguir con la dieta habitual."));
            } else if (i < 6) {
                save(cancelled(pet.getOwner(), pet, vet, start, reason));
            } else {
                Appointment noShow = scheduled(pet.getOwner(), pet, vet, start, reason, null);
                noShow.setStatus(AppointmentStatus.NO_SHOW);
                save(noShow);
            }
        }

        // Próximas citas: una por mascota, para respetar la regla de una cita activa.
        for (int i = 0; i < Math.min(5, homePets.size() - 1); i++) {
            Pet pet = homePets.get(i + 1);
            Veterinarian vet = vets.get(i % vets.size());
            LocalDateTime start = openDay(now.toLocalDate(), 1 + i).atTime(9, 0).plusMinutes(30L * i);
            save(scheduled(pet.getOwner(), pet, vet, start, reasons[(i + 2) % reasons.length], null));
        }
        Appointment newcomer = scheduled(homePets.getFirst().getOwner(), null, vets.getFirst(),
                openDay(now.toLocalDate(), 2).atTime(12, 0), AppointmentReason.FIRST_VISIT, null);
        newcomer.setProvisionalPetName("Pipo");
        newcomer.setProvisionalPetSpecies(Species.CAT);
        save(newcomer);
    }

    private Pet pet(String name, Species species, String breed, int age, double weight, Owner owner, boolean hospitalized,
                    String disease, String photoUrl) {
        return petRepository.save(Pet.builder().name(name).species(species).breed(breed).age(age).weight(weight)
                .owner(owner).active(hospitalized).disease(disease).photoUrl(photoUrl).build());
    }

    private Appointment scheduled(Owner owner, Pet pet, Veterinarian vet, LocalDateTime start, AppointmentReason reason,
                                  String ownerNotes) {
        LocalDateTime createdAt = start.isBefore(now) ? start.minusDays(3) : now.minusDays(1);
        return Appointment.builder()
                .owner(owner)
                .pet(pet)
                .veterinarian(vet)
                .startTime(start)
                .reason(reason)
                .ownerNotes(ownerNotes)
                .createdAt(createdAt)
                // Las citas pasadas no deben recibir recordatorio.
                .reminderSentAt(start.isBefore(now) ? start.minusDays(1) : null)
                .build();
    }

    private Appointment completed(Owner owner, Pet pet, Veterinarian vet, LocalDateTime start, AppointmentReason reason,
                                  String clinicalNotes) {
        Appointment appointment = scheduled(owner, pet, vet, start, reason, null);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setOutcome(AppointmentOutcome.AMBULATORY);
        appointment.setClinicalNotes(clinicalNotes);
        return appointment;
    }

    private Appointment cancelled(Owner owner, Pet pet, Veterinarian vet, LocalDateTime start, AppointmentReason reason) {
        Appointment appointment = scheduled(owner, pet, vet, start, reason, null);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setOccupiesSlot(null);
        appointment.setCancelledBy(AppointmentActor.OWNER);
        appointment.setCancelledAt(start.minusDays(1));
        return appointment;
    }

    private void save(Appointment appointment) {
        appointmentRepository.save(appointment);
    }

    // Día de la demo: hoy si aún quedan al menos tres franjas; si no, el siguiente día hábil.
    private List<LocalDateTime> demoDaySlots() {
        List<LocalDateTime> today = clinicSchedule.slotsOf(now.toLocalDate()).stream()
                .filter(start -> start.isAfter(now))
                .toList();
        if (today.size() >= 3) {
            return today;
        }
        return clinicSchedule.slotsOf(openDay(now.toLocalDate(), 1));
    }

    // El día hábil número {@code offset} contando desde {@code from} (negativo hacia atrás); con 0, el mismo día o el siguiente hábil.
    private LocalDate openDay(LocalDate from, int offset) {
        LocalDate day = from;
        int step = offset >= 0 ? 1 : -1;
        for (int remaining = Math.abs(offset); remaining > 0; ) {
            day = day.plusDays(step);
            if (clinicSchedule.isOpen(day)) {
                remaining--;
            }
        }
        while (!clinicSchedule.isOpen(day)) {
            day = day.plusDays(1);
        }
        return day;
    }

    private Optional<LocalDateTime> lastSlotBeforeNow() {
        for (int back = 0; back < 7; back++) {
            List<LocalDateTime> passed = clinicSchedule.slotsOf(now.toLocalDate().minusDays(back)).stream()
                    .filter(start -> start.isBefore(now))
                    .toList();
            if (!passed.isEmpty()) {
                return Optional.of(passed.getLast());
            }
        }
        return Optional.empty();
    }
}
