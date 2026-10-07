package io.github.pethaven.controller;

import io.github.pethaven.entity.Pet;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import io.github.pethaven.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetRestController {

    private final PetService petService;
    private final OwnerService ownerService;
    private final TreatmentService treatmentService;

    @Autowired
    public PetRestController(
            PetService petService,
            OwnerService ownerService,
            TreatmentService treatmentService
    ) {
        this.petService = petService;
        this.ownerService = ownerService;
        this.treatmentService = treatmentService;
    }

    @GetMapping
    public List<Pet> getAllPets() {
        return petService.getAllPets();
    }

    @GetMapping("/{id}")
    public Pet getPetById(@PathVariable Long id) {
        return petService.getPetById(id);
    }

    @PostMapping
    public Pet createPet(@RequestBody Pet pet, @RequestParam("ownerId") Long ownerId) {
        return petService.createPet(pet, ownerId);
    }

    @PutMapping("/{id}")
    public Pet updatePet(@PathVariable Long id, @RequestBody Pet updatedPet) {
        return petService.updatePet(id, updatedPet);
    }

    @PutMapping("/{id}/active")
    public void switchPetActiveStatus(@PathVariable Long id, @RequestParam("active") boolean active) {
        petService.switchPetActiveStatus(id, active);
    }

}
