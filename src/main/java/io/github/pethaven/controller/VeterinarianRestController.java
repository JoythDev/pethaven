package io.github.pethaven.controller;

import io.github.pethaven.service.VeterinarianService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del módulo de veterinarios.
 */
@Tag(name = "Veterinarians", description = "Gestión de veterinarios")
@RestController
@RequestMapping("/api/veterinarians")
public class VeterinarianRestController {

    private final VeterinarianService veterinarianService;

    @Autowired
    public VeterinarianRestController(VeterinarianService veterinarianService) {
        this.veterinarianService = veterinarianService;
    }
}
