// src/main/java/com/inkFront/schoolManagement/dto/NotifySummaryDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NotifySummaryDTO {
    private Long announcementId;
    private String title;

    private int smsSuccess;
    private int smsFailed;

    private int emailSuccess;
    private int emailFailed;

    private int portalCount;

    private List<String> failedSmsNumbers;
    private List<String> failedEmails;

    private String message; // user friendly message
}