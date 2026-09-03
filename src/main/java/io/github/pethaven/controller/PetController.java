package io.github.pethaven.controller;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import jakarta.servlet.http.HttpSession;

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
                           HttpSession session, Model model) {
        
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        // Regla: Solo el veterinario ve la lista completa de pacientes
        if (!ownerService.isVeterinarian(loggedOwner)) {
            return "redirect:/owners/" + loggedOwner.getId();
        }

        model.addAttribute("pets", petService.getAllPets(search, status));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        return "pets_list";
    }

    @GetMapping("/{id}")
    public String getPet(@PathVariable Long id, HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        Pet pet = petService.getPetById(id);

        // Regla: El dueño solo puede ver la ficha de la mascota si es suya
        if (loggedOwner != null && !pet.getOwnerId().equals(loggedOwner.getId())) {
            return "redirect:/owners/" + loggedOwner.getId();
        }

        model.addAttribute("pet", pet);
        return "pet_details";
    }

    @GetMapping("/add")
    public String showAddPetForm(HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        model.addAttribute("pet", new Pet(null, "", null, "", 0, 0.0, "", "", null));
        model.addAttribute("owners", ownerService.getAllOwners());
        return "pet_form";
    }

    @PostMapping("/add")
    public String addPet(Pet pet, HttpSession session) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        petService.createPet(pet);
        return "redirect:/pets";
    }

    @GetMapping("/update/{id}")
    public String updatePet(@PathVariable Long id, HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        model.addAttribute("pet", petService.getPetById(id));
        model.addAttribute("owners", ownerService.getAllOwners());
        return "pet_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedPet(@PathVariable Long id, Pet pet, HttpSession session) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        pet.setId(id);
        petService.createPet(pet);
        return "redirect:/pets/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deletePet(@PathVariable Long id, HttpSession session) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        petService.deletePetById(id);
        return "redirect:/pets";
    }
}