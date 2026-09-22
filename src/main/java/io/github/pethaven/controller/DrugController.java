package io.github.pethaven.controller;

import io.github.pethaven.entity.Drug;
import io.github.pethaven.service.DrugService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.math.BigDecimal;

@Controller
@RequestMapping("/drugs")
public class DrugController {

    @Autowired
    private DrugService drugService;

    @GetMapping()
    public String listDrugs(Model model) {
        model.addAttribute("drugs", drugService.getAllDrugs());
        return "drugs_list";
    }

    @GetMapping("/{id}")
    public String getDrug(@PathVariable Long id, Model model) {
        model.addAttribute("drug", drugService.getDrugById(id));
        return "drug_details";
    }

    @GetMapping("/add")
    public String showAddDrugForm(Model model) {
        model.addAttribute("drug", Drug.builder()
                .name("")
                .purchasePrice(BigDecimal.ZERO)
                .salePrice(BigDecimal.ZERO)
                .unitsAvailable(0)
                .unitsSold(0)
                .build());
        return "drug_form";
    }

    @PostMapping("/add")
    public String addDrug(Drug drug) {
        drugService.createDrug(drug);
        return "redirect:/drugs";
    }

    @GetMapping("/update/{id}")
    public String updateDrug(@PathVariable Long id, Model model) {
        model.addAttribute("drug", drugService.getDrugById(id));
        return "drug_form";
    }

    @PostMapping("/update/{id}")
    public String saveUpdatedDrug(@PathVariable Long id, Drug drug) {
        drug.setId(id);
        drugService.createDrug(drug);
        return "redirect:/drugs/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteDrug(@PathVariable Long id) {
        drugService.deleteDrugById(id);
        return "redirect:/drugs";
    }

    /** Convierte los campos de texto vacíos en null al recibir formularios. */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
