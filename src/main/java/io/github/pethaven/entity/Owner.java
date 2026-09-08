package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "pets")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document", length = 20, unique = true, nullable = false)
    private String document;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 120, unique = true, nullable = false)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Builder.Default // Builder tiene en cuenta el inicializador de la lista
    // @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true) // JPA - Introduce bug al actualizar un Owner (elimina sus mascotas)
    @OneToMany(mappedBy = "owner")
    @OnDelete(action = OnDeleteAction.CASCADE) // Hibernate
    private List<Pet> pets = new ArrayList<>();

}
