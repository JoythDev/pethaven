package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByDocument(String document);
    Optional<Owner> findByEmail(String email);

    /** Perfil asociado a la cuenta de login con el username indicado. */
    Optional<Owner> findByUserUsername(String username);

    boolean existsByDocument(String document);
    boolean existsByEmail(String email);
    boolean existsByDocumentAndIdNot(String document, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    // Comprobaciones de propiedad de la matriz de permisos (ResourceAccess): el recurso cruzado con el usuario de login.
    boolean existsByIdAndUserUsername(Long id, String username);

    boolean existsByIdAndUserUsernameAndDocumentAndEmail(Long id, String username, String document, String email);
}
