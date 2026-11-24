package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// Un pesaje de la mascota: lo dejan su registro, cada cambio de peso y cada tratamiento (incluida el alta).
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "weight_records", indexes = @Index(name = "idx_weight_records_pet", columnList = "pet_id, recorded_at"))
@Entity
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El historial se va con la mascota cuando se elimina a su dueño.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pet pet;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "weight", nullable = false)
    private Double weight;
}
