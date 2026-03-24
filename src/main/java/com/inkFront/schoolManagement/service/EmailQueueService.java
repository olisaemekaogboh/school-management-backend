// src/main/java/com/inkFront/schoolManagement/service/EmailQueueService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.EmailQueue;
import com.inkFront.schoolManagement.model.EmailQueueStatus;

import java.util.List;
import java.util.Map;

public interface EmailQueueService {
    void queueEmail(Long announcementId, String to, String subject, String body);
    void processQueue();

    List<EmailQueue> getAllQueuedEmails();
    List<EmailQueue> getQueuedEmailsByStatus(EmailQueueStatus status);
    List<EmailQueue> getQueuedEmailsByAnnouncement(Long announcementId);
    Map<String, Long> getQueueStats();

    void retryEmail(Long queueId);
}