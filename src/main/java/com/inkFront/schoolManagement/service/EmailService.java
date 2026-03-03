// src/main/java/com/inkFront/schoolManagement/service/EmailService.java
package com.inkFront.schoolManagement.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendWelcomeEmail(String to, String name);
    void sendNotification(String to, String subject, String content);
    void sendTeacherInvitation(String to, String registrationLink, String firstName);
}