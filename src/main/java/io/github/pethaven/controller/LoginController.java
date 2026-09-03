package io.github.pethaven.controller;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.service.OwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private OwnerService ownerService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                         @RequestParam String password,
                         Model model) {

        // Delegamos toda la validación al Service
        try {
            Owner owner = ownerService.authenticate(email, password);
            return "redirect:/owners/" + owner.getId();
        } catch (Exception ex){
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "login";
        }
        
    }

}