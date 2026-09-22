package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "treatments")
@Entity
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private Veterinarian veterinarian;

    @Column(name = "treatment_date", nullable = false)
    private LocalDateTime date;

    @Builder.Default
    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TreatmentDrug> treatmentDrugs = new ArrayList<>();

}
