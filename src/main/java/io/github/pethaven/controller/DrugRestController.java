package io.github.pethaven.controller;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.service.DrugService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del módulo de medicamentos.
 */
@Tag(name = "Drugs", description = "Gestión de medicamentos")
@RestController
@RequestMapping("/api/drugs")
public class DrugRestController {

    private final DrugService drugService;

    @Autowired
    public DrugRestController(DrugService drugService) {
        this.drugService = drugService;
    }

    @GetMapping
    public List<DrugResponse> getAllDrugs() {
        return drugService.getAllDrugs();
    }

    @GetMapping("/{id}")
    public DrugResponse getDrugById(@PathVariable Long id) {
        return drugService.getDrugById(id);
    }

    @PostMapping
    public ResponseEntity<DrugResponse> createDrug(@Valid @RequestBody DrugRequest request) {
        DrugResponse newDrug = drugService.createDrug(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newDrug.id())
                .toUri();
        return ResponseEntity.created(location).body(newDrug);
    }

    @PutMapping("/{id}")
    public DrugResponse updateDrug(@PathVariable Long id, @Valid @RequestBody DrugRequest request) {
        return drugService.updateDrug(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDrug(@PathVariable Long id) {
        drugService.deleteDrugById(id);
        return ResponseEntity.noContent().build();
    }
}
