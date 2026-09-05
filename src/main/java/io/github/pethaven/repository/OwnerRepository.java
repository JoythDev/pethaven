package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByDocument(String document);
    Optional<Owner> findByEmail(String email);
}
