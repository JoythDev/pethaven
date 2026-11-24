package io.github.pethaven.service;

import io.github.pethaven.dto.response.WeightRecordResponse;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.WeightRecord;
import io.github.pethaven.repository.WeightRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WeightHistoryServiceImpl implements WeightHistoryService {

    private final WeightRecordRepository weightRecordRepository;

    @Autowired
    public WeightHistoryServiceImpl(WeightRecordRepository weightRecordRepository) {
        this.weightRecordRepository = weightRecordRepository;
    }

    @Override
    @Transactional
    public void record(Pet pet, Double weight, LocalDateTime recordedAt) {
        weightRecordRepository.save(WeightRecord.builder().pet(pet).weight(weight).recordedAt(recordedAt).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightRecordResponse> getWeightsByPetId(Long petId) {
        return weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(petId).stream()
                .map(record -> new WeightRecordResponse(record.getId(), record.getRecordedAt(), record.getWeight()))
                .toList();
    }
}
