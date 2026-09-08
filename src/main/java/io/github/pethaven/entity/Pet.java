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

    // Muchas mascotas pueden pertenecer a un dueño, pero cada mascota tiene un solo dueño.
    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY para evitar cargar el owner cuando no es necesario
    @JoinColumn(name = "owner_id") // Nombre explícito de la columna de la clave foránea
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

    @Builder.Default // Builder tiene en cuenta el inicializador del campo
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
