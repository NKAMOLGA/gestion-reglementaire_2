package com.bcpme.gestion_reglementaire.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExportController {

    @GetMapping("/export")
    public String export() {
        return "export";
    }
}