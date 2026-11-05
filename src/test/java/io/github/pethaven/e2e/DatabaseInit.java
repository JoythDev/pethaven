package io.github.pethaven.e2e;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Datos iniciales del perfil "test": cada reinicio de contexto (DirtiesContext) parte de este estado. */
@Component
@Profile("test")
public class DatabaseInit implements CommandLineRunner {

    public static final String VET_EMAIL = "vet@pethaven.com";
    public static final String VET_PASSWORD = "password123";

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Override
    public void run(String... args) {
        veterinarianRepository.save(Veterinarian.builder()
                .name("Ana Gómez").document("111222333").email(VET_EMAIL)
                .password(VET_PASSWORD).specialty("Medicina general").build());
        ownerRepository.save(Owner.builder()
                .name("John Doe").document("123456789").phone("1234567890")
                .email("john.doe@example.com").password("password123").build());
    }
}
