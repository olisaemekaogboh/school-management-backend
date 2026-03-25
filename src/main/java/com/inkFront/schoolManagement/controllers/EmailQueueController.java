// src/main/java/com/inkFront/schoolManagement/controllers/EmailQueueController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.EmailQueueDTO;
import com.inkFront.schoolManagement.model.EmailQueueStatus;
import com.inkFront.schoolManagement.service.EmailQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-queue")
@CrossOrigin(origins = "https://localhost:3000")
@RequiredArgsConstructor
public class EmailQueueController {

    private final EmailQueueService emailQueueService;

    @GetMapping
    public ResponseEntity<List<EmailQueueDTO>> getAllQueuedEmails() {
        List<EmailQueueDTO> data = emailQueueService.getAllQueuedEmails()
                .stream()
                .map(EmailQueueDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(data);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmailQueueDTO>> getByStatus(@PathVariable String status) {
        EmailQueueStatus queueStatus = EmailQueueStatus.valueOf(status.toUpperCase());

        List<EmailQueueDTO> data = emailQueueService.getQueuedEmailsByStatus(queueStatus)
                .stream()
                .map(EmailQueueDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(data);
    }

    @GetMapping("/announcement/{announcementId}")
    public ResponseEntity<List<EmailQueueDTO>> getByAnnouncement(@PathVariable Long announcementId) {
        List<EmailQueueDTO> data = emailQueueService.getQueuedEmailsByAnnouncement(announcementId)
                .stream()
                .map(EmailQueueDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(data);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getQueueStats() {
        return ResponseEntity.ok(emailQueueService.getQueueStats());
    }

    @PostMapping("/{queueId}/retry")
    public ResponseEntity<Map<String, String>> retryEmail(@PathVariable Long queueId) {
        emailQueueService.retryEmail(queueId);
        return ResponseEntity.ok(Map.of("message", "Email queued for retry"));
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processQueueNow() {
        emailQueueService.processQueue();
        return ResponseEntity.ok(Map.of("message", "Email queue processing triggered"));
    }
}