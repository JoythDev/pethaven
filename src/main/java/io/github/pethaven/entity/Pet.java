package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "pets")
@Entity
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 60, nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    // TODO: Revisar el cascade y orphanRemoval (hacer borrado explícito en OwnerService)
    // TODO: En OwnerService.deleteOwnerById borrar treatments, luego pets, luego owner.

    @Builder.Default
    // @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToMany(mappedBy = "pet")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Treatment> treatments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false, length = 10)
    private Species species;

    @Column(name = "breed", length = 60, nullable = false)
    private String breed;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "disease", length = 120, nullable = true)
    private String disease;

    @Column(name = "photo_url", length = 500, nullable = true)
    private String photoUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

}
