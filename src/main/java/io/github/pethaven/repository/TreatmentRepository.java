package io.github.pethaven.repository;

import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Evolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByPetId(Long petId);
    List<Treatment> findByVeterinarianId(Long veterinarianId);
    boolean existsByPetOwnerId(Long ownerId);

    // === Query para KPIs del dashboard de administrador ===

    @Query("""
            SELECT COUNT(t)
            FROM Treatment t
            WHERE t.date >= :startInclusive
              AND t.date < :endExclusive
              AND (t.evolution IS NULL OR t.evolution <> :excludedEvolution)
            """)
    long countAdministeredTreatmentsBetween(
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("excludedEvolution") Evolution excludedEvolution);
}
