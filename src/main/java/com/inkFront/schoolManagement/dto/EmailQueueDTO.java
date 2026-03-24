// src/main/java/com/inkFront/schoolManagement/dto/EmailQueueDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.EmailQueue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueDTO {

    private Long id;
    private Long announcementId;
    private String toEmail;
    private String subject;
    private String messageContent;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime nextRetryAt;
    private LocalDateTime sentAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EmailQueueDTO fromEntity(EmailQueue queue) {
        if (queue == null) {
            return null;
        }

        return EmailQueueDTO.builder()
                .id(queue.getId())
                .announcementId(queue.getAnnouncementId())
                .toEmail(queue.getToEmail())
                .subject(queue.getSubject())
                .messageContent(queue.getMessageContent())
                .status(queue.getStatus() != null ? queue.getStatus().name() : null)
                .errorMessage(queue.getErrorMessage())
                .retryCount(queue.getRetryCount())
                .maxRetries(queue.getMaxRetries())
                .nextRetryAt(queue.getNextRetryAt())
                .sentAt(queue.getSentAt())
                .lastAttemptAt(queue.getLastAttemptAt())
                .createdAt(queue.getCreatedAt())
                .updatedAt(queue.getUpdatedAt())
                .build();
    }
}