// src/main/java/com/inkFront/schoolManagement/service/IMPL/EmailServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender; // This is injected via @RequiredArgsConstructor - NO DUPLICATE!

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Async
    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify Your Email - Faith Foundation School";
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        String content = String.format(
                "Hello,\n\n" +
                        "Thank you for registering with Faith Foundation International School.\n\n" +
                        "Please verify your email address by clicking the link below:\n\n" +
                        "%s\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "If you did not create an account, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Faith Foundation International School Administration",
                verificationLink
        );

        sendEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset Your Password - Faith Foundation School";
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String content = String.format(
                "Hello,\n\n" +
                        "You requested to reset your password for your Faith Foundation School account.\n\n" +
                        "Click the link below to proceed:\n\n" +
                        "%s\n\n" +
                        "This link will expire in 1 hour.\n\n" +
                        "If you didn't request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Faith Foundation International School",
                resetLink
        );

        sendEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to Faith Foundation School";
        String content = String.format(
                "Dear %s,\n\n" +
                        "Welcome to Faith Foundation International School! Your account has been successfully created.\n\n" +
                        "You can now log in to access:\n" +
                        "- Student records and profiles\n" +
                        "- Fee payments and history\n" +
                        "- Academic results and report cards\n" +
                        "- Attendance records\n" +
                        "- School announcements\n\n" +
                        "Login at: %s/login\n\n" +
                        "If you have any questions, please contact the school administration.\n\n" +
                        "Best regards,\n" +
                        "The Administration Team\n" +
                        "Faith Foundation International School",
                name,
                frontendUrl
        );

        sendEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendNotification(String to, String subject, String content) {
        sendEmail(to, subject, content);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);

            // 🔥 CRITICAL: DO NOT SWALLOW
            throw new RuntimeException("Email sending failed for " + to, e);
        }
    }
    @Override
    public void sendTeacherInvitation(String to, String registrationLink, String firstName) {
        String subject = "Complete Your Teacher Registration - Faith Foundation School";
        String content = String.format(
                "Hello %s,\n\n" +
                        "You have been invited to join Faith Foundation School as a teacher.\n\n" +
                        "Please click the link below to complete your registration and set your password:\n\n" +
                        "%s\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "If you did not expect this invitation, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Faith Foundation School Administration",
                firstName,
                registrationLink
        );

        sendEmail(to, subject, content);
    }
}