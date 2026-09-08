package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 60, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false)
    private Species species;

    @Column(name = "breed", length = 60, nullable = false)
    private String breed;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "weight", nullable = true)
    private Double weight;

    @Column(name = "disease", length = 120, nullable = true)
    private String disease;

    @Column(name = "photo_url", length = 500, nullable = true)
    private String photoUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
