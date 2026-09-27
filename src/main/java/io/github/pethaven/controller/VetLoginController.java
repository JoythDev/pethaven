package io.github.pethaven.controller;

import io.github.pethaven.service.VeterinarianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Login aparte para el veterinario: todavía no hay roles ni sesión HTTP, así que
// cada tipo de usuario tiene su propia pantalla y su propio flujo de autenticación
// (ver LoginController para el equivalente del dueño).
@Controller
@RequestMapping("/vet")
public class VetLoginController {

    @Autowired
    private VeterinarianService veterinarianService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "vet_login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                         @RequestParam String password,
                         Model model) {

        try {
            veterinarianService.authenticate(email, password);
            return "redirect:/pets";
        } catch (Exception ex) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "vet_login";
        }

    }

}
