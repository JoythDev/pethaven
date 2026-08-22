package io.github.pethaven.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pet {

    private Long id;
    private String name;
    private String breed;
    private Integer age;
    private Double weight;
    private String disease;
    private String photoUrl;

}
