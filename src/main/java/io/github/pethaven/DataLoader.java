package io.github.pethaven;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DataLoader implements CommandLineRunner {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Override
    public void run(String... args) throws Exception {

        // ==================================
        // ==== Cargar dueños de ejemplo ====
        // ==================================

        ownerRepository.save(Owner.builder().name("John Doe").document("123456789").phone("1234567890").email("john.doe@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Jane Doe").document("987654321").phone("0987654321").email("jane.doe@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Alice Smith").document("456789123").phone("4567891230").email("alice.smith@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Bob Johnson").document("789123456").phone("7891234560").email("bob.johnson@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Charlie Brown").document("321654987").phone("3216549870").email("charlie.brown@example.com").password("password123").build());


        // ====================================
        // ==== Cargar mascotas de ejemplo ====
        // ====================================

        petRepository.save(Pet.builder().name("Buddy").species(Species.DOG).breed("Golden Retriever").age(3).weight(15.0).disease("None").photoUrl("https://example.com/buddy.jpg").ownerId(1L).build());
        petRepository.save(Pet.builder().name("Whiskers").species(Species.CAT).breed("Persian").age(2).weight(4.5).disease("None").photoUrl("https://example.com/whiskers.jpg").ownerId(2L).build());
        petRepository.save(Pet.builder().name("Max").species(Species.DOG).breed("Labrador").age(4).weight(20.0).disease("None").photoUrl("https://example.com/max.jpg").ownerId(3L).build());
        petRepository.save(Pet.builder().name("Luna").species(Species.CAT).breed("Siamese").age(1).weight(3.0).disease("None").photoUrl("https://example.com/luna.jpg").ownerId(4L).build());
        petRepository.save(Pet.builder().name("Charlie").species(Species.DOG).breed("Beagle").age(5).weight(10.0).disease("None").photoUrl("https://example.com/charlie.jpg").ownerId(5L).build());
    }

}
