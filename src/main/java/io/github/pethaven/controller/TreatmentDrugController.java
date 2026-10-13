package io.github.pethaven.controller;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.service.TreatmentDrugService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

    @PostMapping("/add")
    public String addTreatmentDrug(@RequestParam("treatmentId") Long treatmentId,
                                    @RequestParam("drugId") Long drugId,
                                    @RequestParam("units") Integer units) {
        treatmentDrugService.createTreatmentDrug(treatmentId, new TreatmentDrugRequest(drugId, units));
        return "redirect:/treatments/" + treatmentId;
    }

}
