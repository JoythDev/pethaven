package io.github.pethaven;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.WeightRecordResponse;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.WeightRecord;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.WeightRecordRepository;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import io.github.pethaven.service.WeightHistoryService;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Historial de pesos (S12.1, S12.4) sobre la base de pruebas: puntos automáticos, borrado en cascada con el
// dueño y el historial de ejemplo (el cargador no es un bean con el perfil "test", así que se crea a mano).
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WeightHistoryIntegrationTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private PetService petService;
    @Autowired
    private WeightHistoryService weightHistoryService;
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private WeightRecordRepository weightRecordRepository;

    private Long ownerId;

    @BeforeEach
    public void setUp() {
        ownerId = ownerService.createOwner(
                new OwnerRequest("555222111", "Owner Weights", "owner.weights@example.com", "password123", "3001234567")).id();
    }

    private Long createPet(double weight) {
        return petService.createPet(new PetRequest("Toby", ownerId, Species.DOG, "Beagle", 3, weight, null)).id();
    }

    private void loadDemo() {
        DemoWeightHistoryLoader loader = new DemoWeightHistoryLoader();
        context.getAutowireCapableBeanFactory().autowireBean(loader);
        loader.run();
    }

    @Test
    public void registrationAndEdits_RecordOnlyRealWeightChanges() {
        Long petId = createPet(10.0);
        petService.updatePet(petId, new PetRequest("Toby", ownerId, Species.DOG, "Beagle", 4, 10.0, null));
        petService.updatePet(petId, new PetRequest("Toby", ownerId, Species.DOG, "Beagle", 4, 10.6, null));

        Assertions.assertThat(weightHistoryService.getWeightsByPetId(petId))
                .extracting(WeightRecordResponse::weight)
                .containsExactly(10.0, 10.6);
    }

    @Test
    public void deletingTheOwner_RemovesTheWeightHistory() {
        Long petId = createPet(10.0);
        // Como en peticiones separadas: el borrado no ve la mascota ya cargada en el contexto.
        entityManager.flush();
        entityManager.clear();

        ownerService.deleteOwnerById(ownerId);
        entityManager.flush();
        entityManager.clear();

        Assertions.assertThat(petRepository.findById(petId)).isEmpty();
        Assertions.assertThat(weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(petId)).isEmpty();
    }

    @Test
    public void demoLoader_AddsAYearOfMonthlyWeightsEndingAtTheRealOnes() {
        Long registeredId = createPet(10.0);
        Pet withoutHistory = petRepository.save(Pet.builder().name("Kira").species(Species.CAT).breed("Bengalí").age(2)
                .weight(4.2).owner(ownerRepository.findById(ownerId).orElseThrow()).build());
        LocalDateTime start = LocalDateTime.now();

        loadDemo();

        for (Long petId : List.of(registeredId, withoutHistory.getId())) {
            List<WeightRecord> history = weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(petId);
            Assertions.assertThat(history).hasSize(DemoWeightHistoryLoader.MONTHS);
            Assertions.assertThat(history).extracting(WeightRecord::getRecordedAt).isSorted();
            Assertions.assertThat(history.get(0).getRecordedAt()).isAfter(start.minusMonths(DemoWeightHistoryLoader.MONTHS).minusDays(5));
            Assertions.assertThat(history.get(history.size() - 1).getRecordedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
        Assertions.assertThat(weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(registeredId))
                .last().extracting(WeightRecord::getWeight).isEqualTo(10.0);
        Assertions.assertThat(weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(withoutHistory.getId()))
                .last().extracting(WeightRecord::getWeight).isEqualTo(4.2);
    }

    @Test
    public void demoLoader_PetWhoseFirstRealWeightIsOld_StillGetsItsYear() {
        Long petId = createPet(10.0);
        Pet pet = petRepository.findById(petId).orElseThrow();
        weightHistoryService.record(pet, 9.5, LocalDateTime.now().minusMonths(8));

        loadDemo();

        Assertions.assertThat(weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(petId))
                .hasSize(DemoWeightHistoryLoader.MONTHS + 1);
    }

    @Test
    public void demoLoader_RunTwice_DoesNotDuplicateTheHistory() {
        createPet(10.0);
        loadDemo();
        long loaded = weightRecordRepository.count();

        loadDemo();

        Assertions.assertThat(weightRecordRepository.count()).isEqualTo(loaded);
    }
}
