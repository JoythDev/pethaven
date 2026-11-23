package io.github.pethaven;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.service.ClinicSchedule;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.VeterinarianService;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Guion de demostración de citas (S16.14) sobre la base de pruebas: el cargador no es un bean con el perfil "test",
// así que se crea a mano con las cuentas que espera de DataLoader.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DemoAppointmentLoaderIntegrationTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private VeterinarianService veterinarianService;
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private DrugRepository drugRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private ClinicSchedule clinicSchedule;

    private Owner john;
    private Owner jane;

    @BeforeEach
    public void setUp() {
        john = owner("123456789", "john.demo@example.com");
        jane = owner("987654321", "jane.demo@example.com");
        veterinarian("328146627", "valentina.demo@pethaven.test");
        for (String document : List.of("241882914", "581736412", "522124823", "192049284")) {
            veterinarian(document, "vet" + document + "@pethaven.test");
        }
        Owner other = owner("555111999", "other.demo@example.com");
        for (int i = 0; i < 6; i++) {
            Pet atHome = RepositoryTestData.pet(other);
            atHome.setActive(false);
            petRepository.save(atHome);
        }
        drugRepository.save(Drug.builder().name("Demo drug " + System.nanoTime()).purchasePrice(new BigDecimal("1000.00"))
                .salePrice(new BigDecimal("2000.00")).unitsAvailable(10).unitsSold(0).build());
    }

    private Owner owner(String document, String email) {
        Long id = ownerService.createOwner(new OwnerRequest(document, "Owner " + document, email, "password123", "3001234567")).id();
        return ownerRepository.findById(id).orElseThrow();
    }

    private void veterinarian(String document, String email) {
        veterinarianService.createVeterinarian(new VeterinarianRequest(document, "Vet " + document, email, "password123", "General", null));
    }

    private List<Appointment> loadDemo() {
        DemoAppointmentLoader loader = new DemoAppointmentLoader();
        context.getAutowireCapableBeanFactory().autowireBean(loader);
        loader.run();
        return appointmentRepository.findAll();
    }

    @Test
    public void run_LoadsAboutTwentyAppointmentsWithoutSharingASlot() {
        List<Appointment> appointments = loadDemo();

        Assertions.assertThat(appointments).hasSizeBetween(18, 22);
        Set<String> occupied = appointments.stream()
                .filter(appointment -> Boolean.TRUE.equals(appointment.getOccupiesSlot()))
                .map(appointment -> appointment.getVeterinarian().getId() + "@" + appointment.getStartTime())
                .collect(Collectors.toSet());
        Assertions.assertThat(occupied).hasSize((int) appointments.stream()
                .filter(appointment -> Boolean.TRUE.equals(appointment.getOccupiesSlot())).count());
        Assertions.assertThat(appointments).extracting(Appointment::getStatus)
                .contains(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED,
                        AppointmentStatus.NO_SHOW);
        Assertions.assertThat(appointments).allSatisfy(appointment ->
                Assertions.assertThat(clinicSchedule.isSlotStart(appointment.getStartTime())).isTrue());
    }

    @Test
    public void run_PreparesTheDemoAccountsScript() {
        List<Appointment> appointments = loadDemo();
        Map<String, Pet> johnPets = petRepository.findByOwnerId(john.getId()).stream()
                .collect(Collectors.toMap(Pet::getName, Function.identity()));
        Map<String, Pet> janePets = petRepository.findByOwnerId(jane.getId()).stream()
                .collect(Collectors.toMap(Pet::getName, Function.identity()));

        Assertions.assertThat(johnPets.keySet()).containsExactlyInAnyOrder("Rex", "Lola", "Bimba");
        Assertions.assertThat(johnPets.values()).noneMatch(Pet::isActive);
        // Bimba queda libre para reservar: no tiene citas.
        Assertions.assertThat(appointments).noneMatch(appointment -> appointment.getPet() != null
                && appointment.getPet().getId().equals(johnPets.get("Bimba").getId()));

        Pet coco = janePets.get("Coco");
        Pet pelusa = janePets.get("Pelusa");
        Assertions.assertThat(coco.isActive()).isTrue();
        Assertions.assertThat(coco.getDisease()).isNull();
        Assertions.assertThat(pelusa.isActive()).isTrue();
        Assertions.assertThat(pelusa.getDisease()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(appointments).anyMatch(appointment -> appointment.getReason() == AppointmentReason.DISCHARGE
                && appointment.getPet().getId().equals(coco.getId()));
        Assertions.assertThat(appointments).anyMatch(appointment -> appointment.getReason() == AppointmentReason.BRIEFING
                && appointment.getPet().getId().equals(pelusa.getId()));
        Assertions.assertThat(appointments).anyMatch(appointment -> appointment.getPet() == null
                && "Milo".equals(appointment.getProvisionalPetName()) && appointment.getOwner().getId().equals(jane.getId()));
        // Una cita ya empezada sin atender, para mostrar «No asistió».
        Assertions.assertThat(appointments).anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.SCHEDULED
                && appointment.getStartTime().isBefore(clinicSchedule.now()));
    }

    @Test
    public void run_Twice_DoesNotDuplicateTheScript() {
        int firstRun = loadDemo().size();
        long pets = petRepository.count();

        Assertions.assertThat(loadDemo()).hasSize(firstRun);
        Assertions.assertThat(petRepository.count()).isEqualTo(pets);
    }

    @Test
    public void run_OneActiveAppointmentPerPetAndPastOnesAlreadyReminded() {
        List<Appointment> appointments = loadDemo();

        Map<Long, Long> futureByPet = appointments.stream()
                .filter(appointment -> appointment.getPet() != null && appointment.getStatus() == AppointmentStatus.SCHEDULED
                        && !appointment.getStartTime().isBefore(clinicSchedule.now()))
                .collect(Collectors.groupingBy(appointment -> appointment.getPet().getId(), Collectors.counting()));
        Assertions.assertThat(futureByPet.values()).allMatch(count -> count == 1);
        Assertions.assertThat(appointments).filteredOn(appointment -> appointment.getStartTime().isBefore(clinicSchedule.now()))
                .allSatisfy(appointment -> Assertions.assertThat(appointment.getReminderSentAt()).isNotNull());
    }
}
