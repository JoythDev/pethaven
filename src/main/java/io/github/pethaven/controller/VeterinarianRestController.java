package io.github.pethaven.controller;

import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.service.VeterinarianService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    @GetMapping
    public List<VeterinarianResponse> getAllVeterinarians() {
        return veterinarianService.getAllVeterinarians();
    }

    @GetMapping("/{id}")
    public VeterinarianResponse getVeterinarianById(@PathVariable Long id) {
        return veterinarianService.getVeterinarianById(id);
    }

    @PostMapping
    public ResponseEntity<VeterinarianResponse> createVeterinarian(@Valid @RequestBody VeterinarianRequest request) {
        VeterinarianResponse newVeterinarian = veterinarianService.createVeterinarian(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newVeterinarian.id())
                .toUri();
        return ResponseEntity.created(location).body(newVeterinarian);
    }

    @PutMapping("/{id}")
    public VeterinarianResponse updateVeterinarian(@PathVariable Long id, @Valid @RequestBody VeterinarianUpdateRequest request) {
        return veterinarianService.updateVeterinarian(id, request);
    }

    @PutMapping("/{id}/active")
    public VeterinarianResponse switchVeterinarianActiveStatus(@PathVariable Long id, @RequestParam("active") boolean active) {
        return veterinarianService.switchVeterinarianActiveStatus(id, active);
    }

    // TODO: Implementar GET /api/veterinarians/{id}/treatments (Fase 5)
}
