package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import com.bcpme.gestion_reglementaire.repository.GenerationColHistoryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class HistoriqueController {

    private final GenerationColHistoryRepository repository;

    public HistoriqueController(GenerationColHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/historique")
    public String historique(
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String nomFichier,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Model model
    ) {

        String utilisateurClean =
                (utilisateur != null && !utilisateur.trim().isEmpty())
                        ? utilisateur.trim()
                        : null;

        String nomFichierClean =
                (nomFichier != null && !nomFichier.trim().isEmpty())
                        ? nomFichier.trim()
                        : null;

        List<GenerationColHistory> result =
                repository.searchHistory(
                        utilisateurClean,
                        nomFichierClean
                );

        model.addAttribute("historiques", result);
        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("nomFichier", nomFichier);
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);

        return "historique";
    }
    
}