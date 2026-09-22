package io.github.pethaven.controller;

import io.github.pethaven.entity.Treatment;
import io.github.pethaven.service.DrugService;
import io.github.pethaven.service.PetService;
import io.github.pethaven.service.TreatmentDrugService;
import io.github.pethaven.service.TreatmentService;
import io.github.pethaven.service.VeterinarianService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

// NOTE: Un tratamiento solo se puede crear y consultar (nunca actualizar ni eliminar):
// es un registro médico permanente, incluso si la mascota deja de existir.
@Controller
@RequestMapping("/treatments")
public class TreatmentController {

    @Autowired
    private TreatmentService treatmentService;

    @Autowired
    private TreatmentDrugService treatmentDrugService;

    @Autowired
    private PetService petService;

    @Autowired
    private VeterinarianService veterinarianService;

    @Autowired
    private DrugService drugService;

    @GetMapping()
    public String listTreatments(Model model) {
        model.addAttribute("treatments", treatmentService.getAllTreatments());
        return "treatments_list";
    }

    @GetMapping("/{id}")
    public String getTreatment(@PathVariable Long id, Model model) {
        model.addAttribute("treatment", treatmentService.getTreatmentById(id));
        model.addAttribute("treatmentDrugs", treatmentDrugService.getTreatmentDrugsByTreatmentId(id));
        model.addAttribute("drugs", drugService.getAllDrugs());
        return "treatment_details";
    }

    @GetMapping("/add")
    public String showAddTreatmentForm(@RequestParam(required = false) Long petId, Model model) {
        model.addAttribute("treatment", Treatment.builder().date(LocalDateTime.now()).build());
        model.addAttribute("pets", petService.getAllPets());
        model.addAttribute("veterinarians", veterinarianService.getAllVeterinarians());
        model.addAttribute("preselectedPetId", petId);
        return "treatment_form";
    }

    @PostMapping("/add")
    public String addTreatment(Treatment treatment,
                                @RequestParam("petId") Long petId,
                                @RequestParam("veterinarianId") Long veterinarianId) {
        treatmentService.createTreatment(treatment, petId, veterinarianId);
        return "redirect:/treatments/" + treatment.getId();
    }

}
