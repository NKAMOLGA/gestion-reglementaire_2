package com.bcpme.gestion_reglementaire.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String alertRecipient;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.alert-recipient:nkamolga@gmail.com}") String alertRecipient) {
        this.mailSender = mailSender;
        this.alertRecipient = alertRecipient;
    }

    public String getAlertRecipient() {
        return alertRecipient;
    }

    public void envoyerAlerte(String username) {
        envoyerMail(
                alertRecipient,
                "Compte bloqué",
                "Le compte "
                        + username
                        + " a été bloqué après 3 tentatives de connexion échouées."
        );
    }

    public void envoyerAlerteReglementaire(String sujet, String contenu) {
        envoyerMail(alertRecipient, sujet, contenu);
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