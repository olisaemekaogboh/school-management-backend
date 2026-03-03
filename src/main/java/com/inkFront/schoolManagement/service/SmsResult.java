// src/main/java/com/inkFront/schoolManagement/service/SmsResult.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.SmsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsResult {
    private String messageId;
    private String phoneNumber;
    private String status; // SENT, DELIVERED, FAILED, PENDING
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private int cost;
    private Long studentId;
    private String studentName;
    private String parentName;
    private Boolean requiresFollowUp;

    // Statistics
    private int successCount;
    private int failedCount;
    private int pendingCount;
    private int deliveredCount;
    private List<String> failedNumbers;
    private List<String> successNumbers;
    private List<SmsLog> logs;
    private String message;
}