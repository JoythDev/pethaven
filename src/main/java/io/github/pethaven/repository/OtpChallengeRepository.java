package io.github.pethaven.repository;

import io.github.pethaven.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {

    /** Lee el desafío con bloqueo de escritura, para que dos verificaciones simultáneas no se salten el límite de intentos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpChallenge> findWithLockById(String id);

    @Transactional
    void deleteByUsername(String username);

    @Transactional
    void deleteByExpiresAtBefore(Instant instant);
}
