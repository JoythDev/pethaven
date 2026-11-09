package io.github.pethaven.e2e;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.service.UserAccountService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales de las pruebas E2E: cada reinicio de contexto (DirtiesContext) parte de este estado.
 * Perfil propio "e2e" para no sembrar datos en las pruebas de integración, que esperan una base vacía.
 */
@Component
@Profile("e2e")
public class DatabaseInit implements CommandLineRunner {

    public static final String VET_EMAIL = "vet@pethaven.com";
    public static final String VET_PASSWORD = "password123";
    public static final String ADMIN_EMAIL = "admin@pethaven.com";
    public static final String ADMIN_PASSWORD = "password123";
    public static final String PET_NAME = "Max";
    public static final String DRUG_NAME = "Amoxicilina";
    public static final int DRUG_PURCHASE_PRICE = 1000;
    public static final int DRUG_SALE_PRICE = 2500;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private DrugRepository drugRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private UserAccountService userAccountService;

    // Se ejecuta después de SecuritySeeder (máxima precedencia), así que los roles ya existen.
    // Las credenciales viven en la cuenta de login y UserAccountService las guarda con hash BCrypt.
    @Override
    public void run(String... args) {
        veterinarianRepository.save(Veterinarian.builder()
                .name("Ana Gómez").document("111222333").email(VET_EMAIL)
                .user(userAccountService.createAccount(VET_EMAIL, VET_PASSWORD, Role.VETERINARIAN))
                .specialty("Medicina general").build());
        Owner owner = ownerRepository.save(Owner.builder()
                .name("John Doe").document("123456789").phone("1234567890")
                .email("john.doe@example.com")
                .user(userAccountService.createAccount("john.doe@example.com", "password123", Role.OWNER))
                .build());
        petRepository.save(Pet.builder()
                .name(PET_NAME).owner(owner).species(Species.DOG).breed("Beagle")
                .age(4).weight(10.0).photoUrl("https://example.com/max.jpg").build());
        drugRepository.save(Drug.builder()
                .name(DRUG_NAME).purchasePrice(BigDecimal.valueOf(DRUG_PURCHASE_PRICE))
                .salePrice(BigDecimal.valueOf(DRUG_SALE_PRICE)).unitsAvailable(50).unitsSold(0).build());
        administratorRepository.save(Administrator.builder()
                .name("Admin").email(ADMIN_EMAIL)
                .user(userAccountService.createAccount(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN))
                .build());
    }
}
