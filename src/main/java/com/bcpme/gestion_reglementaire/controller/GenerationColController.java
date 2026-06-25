package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.service.GenerationColService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class GenerationColController {

    private final GenerationColService generationColService;

    public GenerationColController(GenerationColService generationColService) {
        this.generationColService = generationColService;
    }

    @GetMapping("/generation-col")
    public String pageGeneration() {
        return "generation-col";
    }

    @PostMapping("/generation-col/lancer")
    public String lancerGeneration(Authentication authentication) {

        String username = authentication.getName();
        generationColService.lancerGeneration(username);

        return "redirect:/historique";
    }
}