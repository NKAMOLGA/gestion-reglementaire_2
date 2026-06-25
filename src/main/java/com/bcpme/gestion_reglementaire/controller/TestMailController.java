package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.service.MailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestMailController {

    private final MailService mailService;

    public TestMailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/test-mail")
    public String testMail() {

        mailService.envoyerMail(
                "nkamolga@gmail.com",
                "Test BKCOM",
                "Test d'envoi de mail depuis Spring Boot");

        return "Mail envoyé";
    }
}