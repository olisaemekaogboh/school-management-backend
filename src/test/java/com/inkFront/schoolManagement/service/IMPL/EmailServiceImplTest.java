package com.inkFront.schoolManagement.service.IMPL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@school.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void sendVerificationEmail_ShouldSendEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("test@example.com", "test-token");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldSendEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendPasswordResetEmail("test@example.com", "reset-token");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendWelcomeEmail_ShouldSendEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendWelcomeEmail("test@example.com", "John Doe");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotification_ShouldSendEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendNotification("test@example.com", "Subject", "Content");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendTeacherInvitation_ShouldSendEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendTeacherInvitation("test@example.com", "http://localhost:3000/register", "John");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_WithException_ShouldThrowRuntimeException() {
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class, () ->
                emailService.sendVerificationEmail("test@example.com", "token")
        );
    }
}