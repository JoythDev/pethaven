package io.github.pethaven.repository;

import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.repository.projection.MedicationTreatmentCountProjection;
import io.github.pethaven.repository.projection.TopMedicationUnitsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentDrugRepository extends JpaRepository<TreatmentDrug, Long> {
    List<TreatmentDrug> findByTreatmentId(Long treatmentId);
    boolean existsByTreatmentIdAndDrugId(Long treatmentId, Long drugId);
    boolean existsByDrugId(Long drugId);

    // === Queries para KPIs del dashboard de administrador ===

    @Query("""
            SELECT d.id AS medicationId,
                   d.name AS medicationName,
                   COUNT(DISTINCT t.id) AS treatmentCount
            FROM TreatmentDrug td
            JOIN td.treatment t
            JOIN td.drug d
            WHERE t.date >= :startInclusive
              AND t.date < :endExclusive
              AND (t.evolution IS NULL OR t.evolution <> :excludedEvolution)
            GROUP BY d.id, d.name
            ORDER BY d.name ASC, d.id ASC
            """)
    List<MedicationTreatmentCountProjection> findMedicationTreatmentCountsBetween(
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("excludedEvolution") Evolution excludedEvolution);

    @Query("SELECT SUM(td.unitSalePrice * td.units) FROM TreatmentDrug td")
    BigDecimal sumAllTimeSales();

    @Query("SELECT SUM((td.unitSalePrice - td.unitPurchasePrice) * td.units) FROM TreatmentDrug td")
    BigDecimal sumAllTimeGrossProfit();

    @Query("""
            SELECT d.id AS medicationId,
                   d.name AS medicationName,
                   SUM(td.units) AS unitsSold
            FROM TreatmentDrug td
            JOIN td.drug d
            GROUP BY d.id, d.name
            ORDER BY SUM(td.units) DESC, d.name ASC, d.id ASC
            """)
    List<TopMedicationUnitsProjection> findTopMedicationsByAllTimeUnitsSold(Pageable pageable);
}
