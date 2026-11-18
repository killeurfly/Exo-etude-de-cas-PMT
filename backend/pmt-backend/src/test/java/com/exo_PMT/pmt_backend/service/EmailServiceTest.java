package com.exo_PMT.pmt_backend.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendsNothingWhenMailIsNotConfigured() {
        EmailService emailService = new EmailService(mailSender, "");

        emailService.sendTaskAssigned("bob@example.test", "Tâche", "PMT", "Alice");

        assertFalse(emailService.isEnabled());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsAssignmentEmailToAssignee() {
        EmailService emailService = new EmailService(mailSender, "pmt@example.test");

        emailService.sendTaskAssigned("bob@example.test", "Écrire les tests", "PMT", "Alice");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertTrue(emailService.isEnabled());
        assertEquals("pmt@example.test", message.getFrom());
        assertArrayEquals(new String[] {"bob@example.test"}, message.getTo());
        assertEquals("[PMT] Nouvelle tâche assignée : Écrire les tests", message.getSubject());
        assertTrue(message.getText().contains("Alice vous a assigné la tâche « Écrire les tests » dans le projet « PMT »."));
    }

    @Test
    void sendingFailureDoesNotPropagate() {
        EmailService emailService = new EmailService(mailSender, "pmt@example.test");
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendTaskAssigned("bob@example.test", "Tâche", "PMT", "Alice"));
    }
}
