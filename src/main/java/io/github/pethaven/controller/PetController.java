package io.github.pethaven.controller;

import io.github.pethaven.entity.Pet;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/pets")
public class PetController {

    @Autowired
    private PetService petService;

    @Autowired
    private OwnerService ownerService;

    @GetMapping()
    public String listPets(@RequestParam(required = false) String search, 
                           @RequestParam(required = false) String status, 
                           Model model) {
        model.addAttribute("pets", petService.getAllPets(search, status));
        // Devolvemos los valores al HTML para que la barra de búsqueda no se borre al recargar
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        return "pets_list";
    }

    @GetMapping("/{id}")
    public String getPet(@PathVariable Long id, Model model) {
        model.addAttribute("pet", petService.getPetById(id));
        return "pet_details";
    }

    @GetMapping("/add")
    public String showAddPetForm(Model model) {
        Pet pet = new Pet(null, "", null, "", 0, 0.0, "", "",null);
        model.addAttribute("owners", ownerService.getAllOwners());
        model.addAttribute("pet", pet);
        return "pet_form";
    }

    @PostMapping("/add")
    public String addPet(Pet pet) {
        petService.createPet(pet);
        return "redirect:/pets";
    }

    @GetMapping("/update/{id}")
    public String updatePet(@PathVariable Long id, Model model) {
        Pet pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        model.addAttribute("owners", ownerService.getAllOwners());
        return "pet_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedPet(@PathVariable Long id, Pet pet) {
        pet.setId(id);
        petService.createPet(pet);
        return "redirect:/pets/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deletePet(@PathVariable Long id) {
        petService.deletePetById(id);
        return "redirect:/pets";
    }

}
