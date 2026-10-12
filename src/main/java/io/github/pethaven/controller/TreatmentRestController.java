package io.github.pethaven.controller;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.service.TreatmentDrugService;
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
    private final TreatmentDrugService treatmentDrugService;

    @Autowired
    public TreatmentRestController(
            TreatmentService treatmentService,
            TreatmentDrugService treatmentDrugService
    ) {
        this.treatmentService = treatmentService;
        this.treatmentDrugService = treatmentDrugService;
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

    @GetMapping("/{id}/drugs")
    public List<TreatmentDrugResponse> getTreatmentDrugsByTreatmentId(@PathVariable Long id) {
        return treatmentDrugService.getTreatmentDrugsByTreatmentId(id);
    }

    @PostMapping("/{id}/drugs")
    public ResponseEntity<TreatmentDrugResponse> createTreatmentDrug(
            @PathVariable Long id,
            @Valid @RequestBody TreatmentDrugRequest request) {
        TreatmentDrugResponse newTreatmentDrug = treatmentDrugService.createTreatmentDrug(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{drugId}")
                .buildAndExpand(newTreatmentDrug.id())
                .toUri();
        return ResponseEntity.created(location).body(newTreatmentDrug);
    }
}
