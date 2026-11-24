package io.github.pethaven.service;

import io.github.pethaven.dto.response.WeightRecordResponse;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.WeightRecord;
import io.github.pethaven.repository.WeightRecordRepository;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class WeightHistoryServiceMockTest {

    @Mock
    private WeightRecordRepository weightRecordRepository;

    private WeightHistoryServiceImpl weightHistoryService;

    @BeforeEach
    public void setUp() {
        weightHistoryService = new WeightHistoryServiceImpl(weightRecordRepository);
    }

    @Test
    public void weightHistoryService_record_SavesThePetWeightAndDate() {
        // Arrange
        Pet pet = RepositoryTestData.pet(RepositoryTestData.owner());
        LocalDateTime date = LocalDateTime.of(2026, 10, 12, 10, 30);

        // Act
        weightHistoryService.record(pet, 12.5, date);

        // Assert
        ArgumentCaptor<WeightRecord> captor = ArgumentCaptor.forClass(WeightRecord.class);
        Mockito.verify(weightRecordRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getPet()).isSameAs(pet);
        Assertions.assertThat(captor.getValue().getWeight()).isEqualTo(12.5);
        Assertions.assertThat(captor.getValue().getRecordedAt()).isEqualTo(date);
    }

    @Test
    public void weightHistoryService_getWeightsByPetId_ReturnsTheRecordsInRepositoryOrder() {
        // Arrange
        Mockito.when(weightRecordRepository.findByPetIdOrderByRecordedAtAscIdAsc(1L)).thenReturn(List.of(
                WeightRecord.builder().id(3L).recordedAt(LocalDateTime.of(2026, 5, 2, 9, 0)).weight(11.8).build(),
                WeightRecord.builder().id(7L).recordedAt(LocalDateTime.of(2026, 10, 12, 10, 30)).weight(12.5).build()));

        // Act
        List<WeightRecordResponse> result = weightHistoryService.getWeightsByPetId(1L);

        // Assert
        Assertions.assertThat(result).containsExactly(
                new WeightRecordResponse(3L, LocalDateTime.of(2026, 5, 2, 9, 0), 11.8),
                new WeightRecordResponse(7L, LocalDateTime.of(2026, 10, 12, 10, 30), 12.5));
    }
}
