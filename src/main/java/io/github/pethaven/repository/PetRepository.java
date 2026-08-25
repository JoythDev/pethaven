package io.github.pethaven.repository;

import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PetRepository {

    Map<Long, Pet> pets = new HashMap<>();

    public PetRepository() {
        pets.put(1L, new Pet(1L, "Buddy", Species.DOG, "Golden Retriever", 3, 30.0, "None", "https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVycm8lMjBnb2xkZW4lMjByZXRyaWV2ZXJ8ZW58MHx8MHx8fDA%3D"));
        pets.put(2L, new Pet(2L, "Mittens", Species.CAT, "Tabby Cat", 2, 4.5, "Feline Leukemia", "https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1"));
        pets.put(3L, new Pet(3L, "Charlie", Species.DOG, "Beagle", 5, 20.0, "Arthritis", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10"));
        pets.put(4L, new Pet(4L, "Luna", Species.CAT, "Siamese Cat", 1, 3.0, "None", "https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg"));
        pets.put(5L, new Pet(5L, "Max", Species.DOG, "German Shepherd", 4, 35.0, "Hip Dysplasia", "https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg"));
        pets.put(6L, new Pet(6L, "Coco", Species.DOG, "Poodle", 2, 1.0, "Sarcoptic Mange", "https://www.dogfoodadvisor.com/wp-content/uploads/2025/02/Poodles.jpg"));
        pets.put(7L, new Pet(7L, "Bella", Species.CAT, "Persian Cat", 3, 5.0, "None", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTpAWZAL82dg9c1330zuD7qOTbGdU1u2XUo2Dt2JgMf-Keq7YlnR9mDuBu&s=10"));
    }

    public List<Pet> findAll(String search, String status) {
        return pets.values().stream()
                .filter(pet -> search == null || search.isEmpty() || 
                        pet.getName().toLowerCase().contains(search.toLowerCase()) || 
                        pet.getBreed().toLowerCase().contains(search.toLowerCase()))
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
        if (pet.getId() == null) {
            pet.setId(nextId());
        }
        pets.put(pet.getId(), pet);
    }

    private Long nextId() {
        return pets.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    public void deleteById(Long id) {
        pets.remove(id);
    }

}
