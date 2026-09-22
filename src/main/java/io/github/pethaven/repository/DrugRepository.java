package io.github.pethaven.repository;

import io.github.pethaven.entity.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {
    Optional<Drug> findByName(String name);
}
