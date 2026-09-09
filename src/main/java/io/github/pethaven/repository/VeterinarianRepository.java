package io.github.pethaven.repository;

import io.github.pethaven.entity.Veterinarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {
    Veterinarian findByDocument(String document);
    Veterinarian findByEmail(String email);
}
