package io.github.pethaven.controller;

import io.github.pethaven.service.TreatmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
