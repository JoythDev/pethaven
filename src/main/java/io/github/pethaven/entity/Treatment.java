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

    // Datos clínicos de la consulta: forman la historia clínica de la mascota.
    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "diagnosis", length = 200, nullable = false)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(name = "evolution", length = 10, nullable = true) // null = ingreso
    private Evolution evolution;

    @Column(name = "notes", length = 1000, nullable = true)
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TreatmentDrug> treatmentDrugs = new ArrayList<>();

}
