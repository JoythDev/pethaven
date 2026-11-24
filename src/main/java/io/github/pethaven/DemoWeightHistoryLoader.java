package io.github.pethaven;

import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.WeightRecord;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.WeightRecordRepository;
import io.github.pethaven.service.WeightHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Historial de pesos de ejemplo (S12.4): unos 12 meses de pesajes mensuales para cada mascota, antes de su
 * primer pesaje real, para que la curva de peso tenga datos desde el arranque. Corre después de los demás
 * cargadores y no se ejecuta con el perfil "test".
 */
@Component
@Profile("!test")
@Order(3)
@Transactional
public class DemoWeightHistoryLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoWeightHistoryLoader.class);

    static final int MONTHS = 12;
    private static final double MIN_WEIGHT = 0.1;
    // Variación mensual máxima de un adulto, por especie.
    private static final Map<Species, Double> VOLATILITY = Map.of(Species.DOG, 0.03, Species.CAT, 0.02);

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private WeightRecordRepository weightRecordRepository;

    @Autowired
    private WeightHistoryService weightHistoryService;

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        long before = weightRecordRepository.count();
        for (Pet pet : petRepository.findAll()) {
            loadHistory(pet, now);
        }
        log.info("Pesajes de ejemplo cargados: {}", weightRecordRepository.count() - before);
    }

    // Hacia atrás desde el primer pesaje real (o desde el peso actual si no tiene): los adultos varían poco;
    // los de un año o menos pesaban menos cuanto más atrás.
    private void loadHistory(Pet pet, LocalDateTime now) {
        Random random = new Random(pet.getId());
        List<WeightRecord> existing = weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(pet.getId());
        // Si la base persiste entre arranques, la mascota ya tiene su año de pesajes: no se duplica.
        if (existing.size() >= MONTHS) {
            return;
        }
        LocalDateTime anchorDate;
        double weight;
        if (existing.isEmpty()) {
            anchorDate = now.minusDays(3 + random.nextInt(18));
            weight = pet.getWeight();
            weightHistoryService.record(pet, weight, anchorDate);
        } else {
            anchorDate = existing.get(0).getRecordedAt();
            weight = existing.get(0).getWeight();
        }
        boolean growing = pet.getAge() <= 1;
        for (int month = 1; month < MONTHS; month++) {
            weight = growing
                    ? weight * (1 - (0.05 + random.nextDouble() * 0.05))
                    : weight * (1 + (random.nextDouble() * 2 - 1) * VOLATILITY.get(pet.getSpecies()));
            weight = Math.max(MIN_WEIGHT, Math.round(weight * 10) / 10.0);
            weightHistoryService.record(pet, weight, anchorDate.minusMonths(month).plusDays(random.nextInt(7) - 3));
        }
    }
}
