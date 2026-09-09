package io.github.pethaven.repository;

import io.github.pethaven.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByPetId(Long petId);
    List<Treatment> findByVeterinarianId(Long veterinarianId);
    void deleteByPetId(Long petId);
    List<Treatment> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
