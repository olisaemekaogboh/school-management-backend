// src/main/java/com/inkFront/schoolManagement/dto/SmsLogDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SmsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLogDTO {
    private Long id;
    private Long announcementId;
    private Long studentId;
    private String studentName;
    private String studentClass;
    private String parentName;
    private String parentPhone;
    private String messageContent;
    private String messageType;
    private String status;
    private Integer deliveryStatus;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private String errorMessage;
    private Integer retryCount;
    private Boolean requiresFollowUp;

    public static SmsLogDTO fromSmsLog(SmsLog log) {
        if (log == null) return null;

        return SmsLogDTO.builder()
                .id(log.getId())
                .announcementId(log.getAnnouncement() != null ? log.getAnnouncement().getId() : null)
                .studentId(log.getStudentId())
                .studentName(log.getStudentName())
                .studentClass(log.getStudentClass())
                .parentName(log.getParentName())
                .parentPhone(log.getParentPhone())
                .messageContent(log.getMessageContent())
                .messageType(log.getMessageType())
                .status(log.getStatus())
                .deliveryStatus(log.getDeliveryStatus())
                .sentAt(log.getSentAt())
                .deliveredAt(log.getDeliveredAt())
                .errorMessage(log.getErrorMessage())
                .retryCount(log.getRetryCount())
                .requiresFollowUp(log.getRequiresFollowUp())
                .build();
    }
}