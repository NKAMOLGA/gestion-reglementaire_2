package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.repository.GenerationColHistoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final GenerationColHistoryRepository repository;

    public DashboardController(GenerationColHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // KPI
        long total = repository.count();
        long success = repository.countByStatut("SUCCESS");
        long error = repository.countByStatut("ERROR");

        // activité récente
        model.addAttribute("latest", repository.findTop10ByOrderByDateGenerationDesc());

        // utilisateurs actifs
        model.addAttribute("users", repository.findDistinctUtilisateurs().size());

        // KPI
        model.addAttribute("total", total);
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        return "dashboard";
    }
}