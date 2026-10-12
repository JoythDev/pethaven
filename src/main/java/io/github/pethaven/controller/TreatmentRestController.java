package io.github.pethaven.controller;

import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.service.TreatmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del módulo de tratamientos.
 */
@Tag(name = "Treatments", description = "Gestión de tratamientos")
@RestController
@RequestMapping("/api/treatments")
public class TreatmentRestController {

    private final TreatmentService treatmentService;

    @Autowired
    public TreatmentRestController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping
    public List<TreatmentResponse> getAllTreatments() {
        return treatmentService.getAllTreatments();
    }

    @GetMapping("/{id}")
    public TreatmentResponse getTreatmentById(@PathVariable Long id) {
        return treatmentService.getTreatmentById(id);
    }

    @PostMapping
    public ResponseEntity<TreatmentResponse> createTreatment(@Valid @RequestBody TreatmentRequest request) {
        TreatmentResponse newTreatment = treatmentService.createTreatment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newTreatment.id())
                .toUri();
        return ResponseEntity.created(location).body(newTreatment);
    }
}
