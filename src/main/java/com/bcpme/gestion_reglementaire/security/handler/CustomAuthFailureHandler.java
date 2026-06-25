package com.bcpme.gestion_reglementaire.security.handler;

import com.bcpme.gestion_reglementaire.entity.Utilisateur;
import com.bcpme.gestion_reglementaire.repository.UtilisateurRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    private final UtilisateurRepository utilisateurRepository;
    private final JavaMailSender mailSender;

    public CustomAuthFailureHandler(
            UtilisateurRepository utilisateurRepository,
            JavaMailSender mailSender) {

        this.utilisateurRepository = utilisateurRepository;
        this.mailSender = mailSender;
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

                    SimpleMailMessage mail =
                            new SimpleMailMessage();

                    mail.setTo("nkamolga@gmail.com");
                    mail.setSubject("Compte utilisateur bloqué");

                    mail.setText(
                            "Le compte utilisateur '"
                                    + utilisateur.getUsername()
                                    + "' a été bloqué après 3 tentatives de connexion échouées.\n\n"
                                    + "Veuillez vérifier et débloquer le compte si nécessaire."
                    );

                    mailSender.send(mail);

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