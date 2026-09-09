package io.github.pethaven.repository;

import io.github.pethaven.entity.TreatmentDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreatmentDrugRepository extends JpaRepository<TreatmentDrug, Long> {
    List<TreatmentDrug> findByTreatmentId(Long treatmentId);
    List<TreatmentDrug> findByDrugId(Long drugId);
}
