package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByDocument(String document);
    Optional<Owner> findByEmail(String email);

    boolean existsByDocument(String document);
    boolean existsByEmail(String email);
    boolean existsByDocumentAndIdNot(String document, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
}
