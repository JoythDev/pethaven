package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", length = 20, nullable = false)
    private Species species;

    @Column(name = "breed", length = 50, nullable = true)
    private String breed;

    @Column(name = "age", nullable = true)
    private Integer age;

    @Column(name = "weight", nullable = true)
    private Double weight;

    @Column(name = "disease", length = 100, nullable = true)
    private String disease;

    @Column(name = "photo_url", length = 255, nullable = true)
    private String photoUrl;

    private Long ownerId;

}
