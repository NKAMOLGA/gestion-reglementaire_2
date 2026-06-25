package com.bcpme.gestion_reglementaire.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(Model model, HttpSession session, Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        Object errorMessage = session.getAttribute("error");

        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            session.removeAttribute("error");
        }

        return "login";
    }
}