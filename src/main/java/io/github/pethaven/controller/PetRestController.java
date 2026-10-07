package io.github.pethaven.controller;

import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import io.github.pethaven.service.TreatmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
public class PetRestController {

    private final PetService petService;
    private final OwnerService ownerService;
    private final TreatmentService treatmentService;

    @Autowired
    public PetRestController(
            PetService petService,
            OwnerService ownerService,
            TreatmentService treatmentService
    ) {
        this.petService = petService;
        this.ownerService = ownerService;
        this.treatmentService = treatmentService;
    }
}
