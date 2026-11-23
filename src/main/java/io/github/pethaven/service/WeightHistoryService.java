package io.github.pethaven.service;

import io.github.pethaven.dto.response.WeightRecordResponse;
import io.github.pethaven.entity.Pet;

import java.time.LocalDateTime;
import java.util.List;

public interface WeightHistoryService {

    void record(Pet pet, Double weight, LocalDateTime recordedAt);
    List<WeightRecordResponse> getWeightsByPetId(Long petId);
}
