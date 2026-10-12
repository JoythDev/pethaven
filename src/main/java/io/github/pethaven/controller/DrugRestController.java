package io.github.pethaven.controller;

import io.github.pethaven.service.DrugService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
