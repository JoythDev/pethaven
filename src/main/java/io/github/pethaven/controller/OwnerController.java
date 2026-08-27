package io.github.pethaven.controller;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.service.OwnerService;
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

    @GetMapping()
    public String listOwners(Model model) {
        model.addAttribute("owners", ownerService.getAllOwners());
        return "owners_list";
    }

    @GetMapping("/{id}")
    public String getOwner(@PathVariable Long id, Model model) {
        model.addAttribute("owner", ownerService.getOwnerById(id));
        return "owner_details";
    }

    @GetMapping("/add")
    public String showAddOwnerForm(Model model) {
        Owner owner = new Owner(null, "", "", "", "", "");
        model.addAttribute("owner", owner);
        return "owner_form";
    }

    @PostMapping("/add")
    public String addOwner(Owner owner) {
        ownerService.createOwner(owner);
        return "redirect:/owners";
    }

    @GetMapping("/delete/{id}")
    public String deleteOwner(@PathVariable Long id) {
        ownerService.deleteOwnerById(id);
        return "redirect:/owners";
    }

}
