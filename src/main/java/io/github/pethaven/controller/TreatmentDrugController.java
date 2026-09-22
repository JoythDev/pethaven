package io.github.pethaven.controller;

import io.github.pethaven.service.TreatmentDrugService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// NOTE: Un TreatmentDrug solo se puede crear (al administrar una droga) y consultar; nunca se
// actualiza ni se elimina de forma independiente (ver TreatmentDrugService).
@Controller
@RequestMapping("/treatment-drugs")
public class TreatmentDrugController {

    @Autowired
    private TreatmentDrugService treatmentDrugService;

    @GetMapping("/{id}")
    public String getTreatmentDrug(@PathVariable Long id, Model model) {
        model.addAttribute("treatmentDrug", treatmentDrugService.getTreatmentDrugById(id));
        return "treatment_drug_details";
    }

    @PostMapping("/add")
    public String addTreatmentDrug(@RequestParam("treatmentId") Long treatmentId,
                                    @RequestParam("drugId") Long drugId,
                                    @RequestParam("units") Integer units) {
        treatmentDrugService.createTreatmentDrug(treatmentId, drugId, units);
        return "redirect:/treatments/" + treatmentId;
    }

}
