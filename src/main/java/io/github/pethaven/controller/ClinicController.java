package io.github.pethaven.controller;

import io.github.pethaven.dto.response.ClinicResponse;
import io.github.pethaven.service.ClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST con los datos públicos de la clínica.
 */
@Tag(name = "Clinic", description = "Sede y horario de atención")
@RestController
@RequestMapping("/api/clinic")
public class ClinicController {

    private final ClinicService clinicService;

    @Autowired
    public ClinicController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    /**
     * Devuelve la sede y su horario de atención.
     *
     * @return nombre, dirección, teléfono, zona horaria, duración de franja y horario por día
     */
    @Operation(summary = "Datos de la clínica",
            description = "Devuelve el nombre, la dirección, el teléfono y el horario de atención para citas, de lunes a domingo. Es público.")
    @SecurityRequirements
    @GetMapping
    public ClinicResponse getClinic() {
        return clinicService.getClinic();
    }
}
