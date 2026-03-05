package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.EmailLog;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogDTO {

    private Long id;
    private Long announcementId;
    private String toEmail;
    private String subject;
    private String messageContent;
    private String status;
    private String errorMessage;
    private LocalDateTime sentAt;

    public static EmailLogDTO fromEmailLog(EmailLog log) {
        if (log == null) return null;

        return EmailLogDTO.builder()
                .id(log.getId())
                .announcementId(log.getAnnouncementId())
                .toEmail(log.getToEmail())
                .subject(log.getSubject())
                .messageContent(log.getMessageContent())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .sentAt(log.getSentAt())
                .build();
    }
}