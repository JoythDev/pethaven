package io.github.pethaven.repository;

import io.github.pethaven.entity.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {
    Optional<Administrator> findByEmail(String email);

    /** Perfil asociado a la cuenta de login con el username indicado. */
    Optional<Administrator> findByUserUsername(String username);
}
