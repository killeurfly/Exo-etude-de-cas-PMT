package com.exo_PMT.pmt_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${pmt.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean isEnabled() {
        return from != null && !from.isBlank();
    }

    // Un échec d'envoi ne doit pas annuler l'assignation : on le signale seulement dans les logs.
    public void sendTaskAssigned(String to, String taskName, String projectName, String assignedByName) {
        if (!isEnabled()) {
            log.info("E-mail non envoyé à {} (MAIL_FROM / MAIL_USERNAME non configuré) : tâche « {} » assignée.", to, taskName);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[PMT] Nouvelle tâche assignée : " + taskName);
        message.setText("""
                Bonjour,

                %s vous a assigné la tâche « %s » dans le projet « %s ».

                Connectez-vous à PMT pour la consulter.
                """.formatted(assignedByName, taskName, projectName));

        try {
            mailSender.send(message);
            log.info("E-mail d'assignation envoyé à {} pour la tâche « {} ».", to, taskName);
        } catch (MailException e) {
            log.warn("Échec de l'envoi de l'e-mail à {} : {}", to, e.getMessage());
        }
    }
}
