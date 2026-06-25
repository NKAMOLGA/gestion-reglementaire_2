package com.bcpme.gestion_reglementaire.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void envoyerAlerte(String username) {

        envoyerMail(
                "nkamolga@gmail.com",
                "Compte bloqué",
                "Le compte "
                        + username
                        + " a été bloqué après 3 tentatives de connexion échouées."
        );
    }

    public void envoyerMail(
            String destinataire,
            String sujet,
            String contenu) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(contenu);

        mailSender.send(message);
    }
}