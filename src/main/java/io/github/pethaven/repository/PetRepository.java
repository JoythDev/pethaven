package io.github.pethaven.repository;

import io.github.pethaven.entity.Pet;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PetRepository {

    Map<Long, Pet> pets = new HashMap<>();

    public PetRepository() {
        pets.put(1L, new Pet(1L, "Buddy", "Golden Retriever", 3, 30.0, "None", "https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVycm8lMjBnb2xkZW4lMjByZXRyaWV2ZXJ8ZW58MHx8MHx8fDA%3D"));
        pets.put(2L, new Pet(2L, "Mittens", "Tabby Cat", 2, 4.5, "Feline Leukemia", "https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1"));
        pets.put(3L, new Pet(3L, "Charlie", "Beagle", 5, 20.0, "Arthritis", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10"));
    }

    public List<Pet> findAll(String search, String status) {
        return pets.values().stream()
                .filter(pet -> search == null || search.isEmpty() || 
                        pet.getName().toLowerCase().contains(search.toLowerCase()) || 
                        pet.getBreed().toLowerCase().contains(search.toLowerCase())) // <-- ¡Aquí agregamos la raza!
                .filter(pet -> {
                    if (status == null || status.equals("todos")) return true;
                    if (status.equals("estable")) return pet.getDisease().equals("None");
                    if (status.equals("enfermo")) return !pet.getDisease().equals("None");
                    return true;
                })
                .toList();
    }

    public Pet findById(Long id) {
        return pets.get(id);
    }

    public void save(Pet pet) {
        pets.put(pet.getId(), pet);
    }

    public void deleteById(Long id) {
        pets.remove(id);
    }

}
