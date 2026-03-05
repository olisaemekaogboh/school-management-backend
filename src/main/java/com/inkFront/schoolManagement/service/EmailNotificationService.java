// src/main/java/com/inkFront/schoolManagement/service/EmailNotificationService.java
package com.inkFront.schoolManagement.service;

public interface EmailNotificationService {
    void sendEmail(String to, String subject, String body);
}