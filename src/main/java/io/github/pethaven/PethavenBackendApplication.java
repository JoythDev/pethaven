package io.github.pethaven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PethavenBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PethavenBackendApplication.class, args);
    }

}
