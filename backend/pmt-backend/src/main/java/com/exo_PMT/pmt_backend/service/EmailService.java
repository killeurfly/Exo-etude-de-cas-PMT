package com.exo_PMT.pmt_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi des notifications par e-mail via le serveur SMTP configuré ({@code spring.mail.*}).
 * <p>
 * L'envoi n'est actif que si un expéditeur est configuré ({@code MAIL_FROM} ou {@code MAIL_USERNAME}) :
 * sans configuration (tests, CI), les notifications sont seulement journalisées.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${pmt.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** Indique si un expéditeur est configuré, donc si les e-mails sont réellement envoyés. */
    public boolean isEnabled() {
        return from != null && !from.isBlank();
    }

    /**
     * Prévient une personne qu'une tâche vient de lui être assignée.
     * Un échec d'envoi ne doit pas annuler l'assignation : il est seulement signalé dans les logs.
     *
     * @param to             adresse e-mail de la personne assignée
     * @param taskName       nom de la tâche
     * @param projectName    nom du projet
     * @param assignedByName nom de la personne qui a fait l'assignation
     */
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
