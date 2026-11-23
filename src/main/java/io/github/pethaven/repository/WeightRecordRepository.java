package io.github.pethaven.repository;

import io.github.pethaven.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    // Del más antiguo al más reciente; a igual fecha, en el orden en que se registraron.
    List<WeightRecord> findByPetIdOrderByRecordedAtAscIdAsc(Long petId);
}
