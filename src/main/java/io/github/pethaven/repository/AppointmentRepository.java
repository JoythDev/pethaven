package io.github.pethaven.repository;

import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByOwnerIdOrderByStartTimeAsc(Long ownerId);

    List<Appointment> findByVeterinarianIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Long veterinarianId, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    // Inicio de cada franja que el veterinario tiene ocupada en el rango.
    @Query("""
            SELECT a.startTime
            FROM Appointment a
            WHERE a.veterinarian.id = :veterinarianId
              AND a.startTime >= :fromInclusive
              AND a.startTime < :toExclusive
              AND a.occupiesSlot = TRUE
            """)
    List<LocalDateTime> findBusySlotStartsOfVeterinarianBetween(
            @Param("veterinarianId") Long veterinarianId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);

    boolean existsByVeterinarianIdAndStartTimeAndOccupiesSlotTrue(Long veterinarianId, LocalDateTime startTime);

    boolean existsByTreatmentId(Long treatmentId);

    List<Appointment> findByVeterinarianIdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            Long veterinarianId, AppointmentStatus status, LocalDateTime fromInclusive);

    // Veterinarios activos con la franja ocupada.
    @Query("""
            SELECT a.veterinarian.id
            FROM Appointment a
            WHERE a.startTime = :start
              AND a.occupiesSlot = TRUE
              AND a.veterinarian.active = TRUE
            """)
    List<Long> findBusyActiveVeterinarianIds(@Param("start") LocalDateTime start);

    // Inicio de cada franja ocupada por un veterinario activo en el rango (una fila por veterinario ocupado).
    @Query("""
            SELECT a.startTime
            FROM Appointment a
            WHERE a.startTime >= :fromInclusive
              AND a.startTime < :toExclusive
              AND a.occupiesSlot = TRUE
              AND a.veterinarian.active = TRUE
            """)
    List<LocalDateTime> findBusySlotStartsBetween(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);

    // Una mascota registrada tiene una cita activa: programada a futuro o en curso.
    @Query("""
            SELECT COUNT(a) > 0
            FROM Appointment a
            WHERE a.pet.id = :petId
              AND ((a.status = io.github.pethaven.entity.AppointmentStatus.SCHEDULED AND a.startTime >= :now)
                   OR a.status = io.github.pethaven.entity.AppointmentStatus.IN_PROGRESS)
            """)
    boolean existsActiveForPet(@Param("petId") Long petId, @Param("now") LocalDateTime now);

    // Lo mismo para una mascota aún no registrada, identificada por su dueño y su nombre provisional.
    @Query("""
            SELECT COUNT(a) > 0
            FROM Appointment a
            WHERE a.pet IS NULL
              AND a.owner.id = :ownerId
              AND LOWER(a.provisionalPetName) = LOWER(:petName)
              AND ((a.status = io.github.pethaven.entity.AppointmentStatus.SCHEDULED AND a.startTime >= :now)
                   OR a.status = io.github.pethaven.entity.AppointmentStatus.IN_PROGRESS)
            """)
    boolean existsActiveForProvisionalPet(
            @Param("ownerId") Long ownerId,
            @Param("petName") String petName,
            @Param("now") LocalDateTime now);

    // Comprobaciones de propiedad de la matriz de permisos (ResourceAccess): la cita cruzada con el usuario de login.
    boolean existsByIdAndOwnerUserUsername(Long id, String username);

    boolean existsByIdAndVeterinarianUserUsername(Long id, String username);
}
