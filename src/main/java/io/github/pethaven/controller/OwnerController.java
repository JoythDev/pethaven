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
        
        // Si hay un dueño en sesión, no puede ver la lista de todos. Lo encerramos en su perfil.
        if (loggedOwner != null) {
            return "redirect:/owners/" + loggedOwner.getId();
        }
        
        // Si NO hay sesión (loggedOwner es null), es el Veterinario. Le mostramos la lista.
        model.addAttribute("owners", ownerService.getAllOwners());
        return "owners_list";
    }

    @GetMapping("/{id}")
    public String getOwner(@PathVariable Long id, HttpSession session, Model model) {
        Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
        
        // Si el que navega es un dueño, pero intenta ver el ID de OTRO dueño, lo devolvemos al suyo.
        if (loggedOwner != null && !loggedOwner.getId().equals(id)) {
            return "redirect:/owners/" + loggedOwner.getId();
        }
        
        // Si es el Veterinario (null) o el dueño viendo su propio perfil, lo dejamos pasar.
        model.addAttribute("owner", ownerService.getOwnerById(id));
        model.addAttribute("pets", petService.getPetsByOwnerId(id));
        return "owner_details";
    }

    @GetMapping("/add")
    public String showAddOwnerForm(HttpSession session, Model model) {
        // Bloqueo: Un cliente no debería poder ver el formulario de registro
        if (session.getAttribute("loggedOwner") != null) {
            Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
            return "redirect:/owners/" + loggedOwner.getId(); 
        }

        Owner owner = new Owner(null, "", "", "", "", "");
        model.addAttribute("owner", owner);
        return "owner_form";
    }

    @PostMapping("/add")
    public String addOwner(Owner owner, HttpSession session) {
        // Bloqueo: Evitar que un cliente inyecte un POST manual para crear dueños
        if (session.getAttribute("loggedOwner") != null) {
            Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
            return "redirect:/owners/" + loggedOwner.getId(); 
        }

        ownerService.createOwner(owner);
        return "redirect:/owners";
    }

    @GetMapping("/update/{id}")
    public String updateOwner(@PathVariable Long id, HttpSession session, Model model) {
        // Solo el Veterinario (sin sesión) puede editar
        if (session.getAttribute("loggedOwner") != null) {
            Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
            return "redirect:/owners/" + loggedOwner.getId(); // Lo expulsamos a su perfil
        }
        
        Owner owner = ownerService.getOwnerById(id);
        model.addAttribute("owner", owner);
        return "owner_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedOwner(@PathVariable Long id, Owner owner, HttpSession session) {
        // Bloqueo de seguridad: Si es un cliente, lo expulsamos antes de que guarde nada
        if (session.getAttribute("loggedOwner") != null) {
            Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
            return "redirect:/owners/" + loggedOwner.getId(); 
        }

        // Si es el Veterinario implícito (sesión vacía), procedemos a actualizar
        owner.setId(id);
        ownerService.createOwner(owner);
        return "redirect:/owners/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteOwner(@PathVariable Long id, HttpSession session) {
        // Solo el Veterinario (sin sesión) puede eliminar
        if (session.getAttribute("loggedOwner") != null) {
            Owner loggedOwner = (Owner) session.getAttribute("loggedOwner");
            return "redirect:/owners/" + loggedOwner.getId(); // Lo expulsamos a su perfil
        }
        
        ownerService.deleteOwnerById(id);
        return "redirect:/owners";
    }

}