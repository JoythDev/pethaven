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
@Table(name = "veterinarians")
@Entity
public class Veterinarian {

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

    @Column(name = "specialty", length = 80, nullable = false)
    private String specialty;

    @Column(name = "photo_url", length = 500, nullable = true)
    private String photoUrl;

    @Builder.Default
    // @OneToMany(mappedBy = "veterinarian", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToMany(mappedBy = "veterinarian")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Treatment> treatments = new ArrayList<>();

    @Builder.Default
    @Column(name = "attentions", nullable = false)
    private Integer attentions = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

}
