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

import java.util.Random;

@Component
@Transactional
public class DataLoader implements CommandLineRunner {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Override
    public void run(String... args) throws Exception {

        Random random = new Random(42);

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

        petRepository.save(Pet.builder().name("Buddy").species(Species.DOG).breed("Golden Retriever").age(3).weight(15.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVycm8lMjBnb2xkZW4lMjByZXRyaWV2ZXJ8ZW58MHx8MHx8fDA%3D").build());
        petRepository.save(Pet.builder().name("Whiskers").species(Species.CAT).breed("Persian").age(2).weight(4.5).disease("None").isActive(true).photoUrl("https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1").build());
        petRepository.save(Pet.builder().name("Max").species(Species.DOG).breed("German Shepherd").age(4).weight(20.0).disease("None").isActive(true).photoUrl("https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg").build());
        petRepository.save(Pet.builder().name("Luna").species(Species.CAT).breed("Siamese").age(1).weight(3.0).disease("None").isActive(true).photoUrl("https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg").build());
        petRepository.save(Pet.builder().name("Charlie").species(Species.DOG).breed("Beagle").age(5).weight(10.0).disease("None").isActive(true).photoUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10").build());
        petRepository.save(Pet.builder().name("Rocky").species(Species.CAT).breed("Sphynx").age(6).weight(4.0).disease("None").photoUrl("https://images.unsplash.com/photo-1574158622682-e40e69881006?fm=jpg&q=60&w=3000&auto=format&fit=crop").isActive(false).build());

        // ===========================================
        // ==== Asignar mascotas a dueños al azar ====
        // ===========================================

        int ownerCount = (int) ownerRepository.count();
        for (Pet pet : petRepository.findAll()) {
            Owner owner = ownerRepository.findById((long) (random.nextInt(ownerCount) + 1)).orElseThrow();
            pet.setOwner(owner);
            petRepository.save(pet);
        }
    }

}
