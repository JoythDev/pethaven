package io.github.pethaven.controller;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.service.OwnerService;
import jakarta.servlet.http.HttpSession;
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
                         HttpSession session,
                         Model model) {
        
        // Delegamos toda la validación al Service
        Owner owner = ownerService.authenticate(email, password);

        if (owner == null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "login";
        }

        session.setAttribute("loggedOwner", owner);
        return "redirect:/owners/" + owner.getId();
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}