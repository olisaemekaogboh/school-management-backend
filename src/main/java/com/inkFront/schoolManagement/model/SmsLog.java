// src/main/java/com/inkFront/schoolManagement/model/SmsLog.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_logs", indexes = {
        @Index(name = "idx_student_id", columnList = "studentId"),
        @Index(name = "idx_parent_phone", columnList = "parentPhone"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_sent_at", columnList = "sentAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "announcement_id")
    private Announcement announcement;

    private Long studentId;
    private String studentName;
    private String studentClass;
    private String parentName;
    private String parentPhone;

    private String messageContent;
    private String messageType; // FEE_REMINDER, RESUMPTION, RESULT, etc.

    @Column(length = 50)
    private String messageId; // Provider's message ID

    @Column(length = 20)
    private String status; // SENT, DELIVERED, FAILED, PENDING

    private Integer deliveryStatus; // 0=Pending, 1=Sent, 2=Delivered, 3=Failed
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private Integer cost; // in kobo/cents

    @Column(length = 500)
    private String errorMessage;

    private Integer retryCount;
    private Boolean requiresFollowUp;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
        if (requiresFollowUp == null) requiresFollowUp = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}