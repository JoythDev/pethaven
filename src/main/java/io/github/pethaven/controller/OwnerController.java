package io.github.pethaven.controller;

import io.github.pethaven.entity.Owner;
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

@Controller
@RequestMapping("/owners")
public class OwnerController {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private PetService petService;

    @GetMapping()
    public String listOwners(HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        
        // Regla: Solo el veterinario ve la lista completa
        if (!ownerService.isVeterinarian(loggedOwner)) {
            return "redirect:/owners/" + loggedOwner.getId();
        }
        
        model.addAttribute("owners", ownerService.getAllOwners());
        return "owners_list";
    }

    @GetMapping("/{id}")
    public String getOwner(@PathVariable Long id, HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        
        // Regla: Validar si este usuario tiene permiso de ver este ID
        if (!ownerService.canAccessOwner(loggedOwner, id)) {
            return "redirect:/owners/" + loggedOwner.getId();
        }
        
        model.addAttribute("owner", ownerService.getOwnerById(id));
        model.addAttribute("pets", petService.getPetsByOwnerId(id));
        return "owner_details";
    }

    @GetMapping("/add")
    public String showAddOwnerForm(HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        model.addAttribute("owner", new Owner(null, "", "", "", "", ""));
        return "owner_form";
    }

    @PostMapping("/add")
    public String addOwner(Owner owner, HttpSession session) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        ownerService.createOwner(owner);
        return "redirect:/owners";
    }

    @GetMapping("/update/{id}")
    public String updateOwner(@PathVariable Long id, HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 
        
        model.addAttribute("owner", ownerService.getOwnerById(id));
        return "owner_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedOwner(@PathVariable Long id, Owner owner, HttpSession session) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 

        owner.setId(id);
        ownerService.createOwner(owner);
        return "redirect:/owners/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteOwner(@PathVariable Long id, HttpSession session) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        if (!ownerService.isVeterinarian(loggedOwner)) return "redirect:/owners/" + loggedOwner.getId(); 
        
        ownerService.deleteOwnerById(id);
        return "redirect:/owners";
    }
}