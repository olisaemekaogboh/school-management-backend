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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationServiceImpl emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "from", "no-reply@school.com");
    }

    @Test
    void sendEmail_ShouldSendEmailSuccessfully() {
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> {
            emailNotificationService.sendEmail(to, subject, body);
        });

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_WithException_ShouldThrowRuntimeException() {
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailNotificationService.sendEmail(to, subject, body);
        });

        assertTrue(exception.getMessage().contains("Failed to send email to " + to));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_WithNullValues_ShouldHandleGracefully() {
        assertDoesNotThrow(() -> {
            emailNotificationService.sendEmail(null, "subject", "body");
        });

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
