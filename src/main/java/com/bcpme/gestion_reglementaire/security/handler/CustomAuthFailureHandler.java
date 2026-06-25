package com.bcpme.gestion_reglementaire.security.handler;

import com.bcpme.gestion_reglementaire.entity.Utilisateur;
import com.bcpme.gestion_reglementaire.repository.UtilisateurRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bcpme.gestion_reglementaire.service.MailService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    private final UtilisateurRepository utilisateurRepository;
    private final MailService mailService;

    public CustomAuthFailureHandler(
            UtilisateurRepository utilisateurRepository,
            MailService mailService) {

        this.utilisateurRepository = utilisateurRepository;
        this.mailService = mailService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String username = request.getParameter("username");

        String errorMessage;

        Utilisateur utilisateur =
                utilisateurRepository.findByUsername(username);

        if (utilisateur != null
                && exception instanceof BadCredentialsException) {

            int tentatives =
                    utilisateur.getTentativesConnexion() + 1;

            utilisateur.setTentativesConnexion(tentatives);

            if (tentatives >= 3) {

                utilisateur.setActif(false);

                utilisateurRepository.save(utilisateur);

                try {
                    mailService.envoyerAlerte(utilisateur.getUsername());
                } catch (Exception e) {

                    System.out.println(
                            "Erreur envoi mail : "
                                    + e.getMessage());
                }

                errorMessage =
                        "Compte bloqué après 3 tentatives échouées. Contactez l'administrateur.";

            } else {

                utilisateurRepository.save(utilisateur);

                errorMessage =
                        "Nom d'utilisateur ou mot de passe incorrect. Veuillez réessayer";
            }
        }
        else if (exception instanceof DisabledException) {

            errorMessage =
                    "Votre compte est bloqué. Veuillez contacter l'administrateur.";
        }
        else {

            errorMessage =
                    "Erreur de connexion";
        }

        request.getSession().setAttribute(
                "error",
                errorMessage);

        response.sendRedirect("/login");
    }
}