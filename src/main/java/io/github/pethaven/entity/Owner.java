package io.github.pethaven.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Owner {

    private Long id;
    private String document;
    private String name;
    private String email;
    private String password;
    private String phone;

}
