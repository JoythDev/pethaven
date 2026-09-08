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
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

@Controller
@RequestMapping("/pets")
public class PetController {

    @Autowired
    private PetService petService;

    @Autowired
    private OwnerService ownerService;

    @GetMapping()
    public String listPets(Model model) {
        model.addAttribute("pets", petService.getAllPets());
        return "pets_list";
    }

    @GetMapping("/{id}")
    public String getPet(@PathVariable Long id, Model model) {
        model.addAttribute("pet", petService.getPetById(id));
        return "pet_details";
    }

    @GetMapping("/add")
    public String showAddPetForm(Model model) {
        model.addAttribute("pet", Pet.builder().name("").owner(null).species(null).breed("").age(0).weight(0.0).disease("").photoUrl("").build());
        model.addAttribute("owners", ownerService.getAllOwners());
        return "pet_form";
    }

    @PostMapping("/add")
    public String addPet(Pet pet, @RequestParam("ownerId") Long ownerId) {
        petService.createPet(pet, ownerId);
        return "redirect:/pets";
    }

    @GetMapping("/update/{id}")
    public String updatePet(@PathVariable Long id, Model model) {
        model.addAttribute("pet", petService.getPetById(id));
        model.addAttribute("owners", ownerService.getAllOwners());
        return "pet_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedPet(@PathVariable Long id, Pet pet, @RequestParam("ownerId") Long ownerId) {
        pet.setId(id);
        petService.createPet(pet, ownerId);
        return "redirect:/pets/" + id;
    }

    @PostMapping("/toggle/{id}")
    public String switchPetActive(@PathVariable Long id,
                                  @RequestParam(required = false, defaultValue = "false") boolean active,
                                  @RequestParam(defaultValue = "false") boolean redirectToDetails) {
        petService.switchPetActiveStatus(id, active);
        return redirectToDetails ? "redirect:/pets/" + id : "redirect:/pets";
    }

    /** Convierte los campos de texto vacíos en null al recibir formularios. */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
