package io.github.pethaven.controller;

import io.github.pethaven.entity.Veterinarian;
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
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

@Controller
@RequestMapping("/veterinarians")
public class VeterinarianController {

    @Autowired
    private VeterinarianService veterinarianService;

    @Autowired
    private TreatmentService treatmentService;

    @GetMapping()
    public String listVeterinarians(Model model) {
        model.addAttribute("veterinarians", veterinarianService.getAllVeterinarians());
        return "veterinarians_list";
    }

    @GetMapping("/{id}")
    public String getVeterinarian(@PathVariable Long id, Model model) {
        model.addAttribute("veterinarian", veterinarianService.getVeterinarianById(id));
        model.addAttribute("treatments", treatmentService.getTreatmentsByVeterinarianId(id));
        return "veterinarian_details";
    }

    @GetMapping("/add")
    public String showAddVeterinarianForm(Model model) {
        model.addAttribute("veterinarian", Veterinarian.builder().name("").document("").email("").password("").specialty("").photoUrl("").build());
        return "veterinarian_form";
    }

    @PostMapping("/add")
    public String addVeterinarian(Veterinarian veterinarian) {
        veterinarianService.createVeterinarian(veterinarian);
        return "redirect:/veterinarians";
    }

    @GetMapping("/update/{id}")
    public String updateVeterinarian(@PathVariable Long id, Model model) {
        model.addAttribute("veterinarian", veterinarianService.getVeterinarianById(id));
        return "veterinarian_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedVeterinarian(@PathVariable Long id, Veterinarian veterinarian) {
        veterinarian.setId(id);
        veterinarianService.createVeterinarian(veterinarian);
        return "redirect:/veterinarians/" + id;
    }

    @PostMapping("/toggle/{id}")
    public String switchVeterinarianActive(@PathVariable Long id,
                                            @RequestParam(required = false, defaultValue = "false") boolean active,
                                            @RequestParam(defaultValue = "false") boolean redirectToDetails) {
        veterinarianService.switchVeterinarianActiveStatus(id, active);
        return redirectToDetails ? "redirect:/veterinarians/" + id : "redirect:/veterinarians";
    }

    /** Convierte los campos de texto vacíos en null al recibir formularios. */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
