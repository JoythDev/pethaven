package io.github.pethaven.repository;

import io.github.pethaven.entity.Veterinarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {
    Optional<Veterinarian> findByDocument(String document);
    Optional<Veterinarian> findByEmail(String email);
}
