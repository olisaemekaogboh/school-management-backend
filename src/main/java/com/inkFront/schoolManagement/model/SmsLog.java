package com.inkFront.schoolManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_logs", indexes = {
        @Index(name = "idx_sms_logs_student_id", columnList = "student_id"),
        @Index(name = "idx_sms_logs_parent_phone", columnList = "parent_phone"),
        @Index(name = "idx_sms_logs_status", columnList = "status"),
        @Index(name = "idx_sms_logs_sent_at", columnList = "sent_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    @JsonIgnore
    private Announcement announcement;

    private Long studentId;
    private String studentName;
    private String studentClass;
    private String parentName;
    private String parentPhone;


    @Column(columnDefinition = "TEXT")
    private String messageContent;

    private String messageType;

    @Column(length = 50)
    private String messageId;

    @Column(length = 20)
    private String status;

    private Integer deliveryStatus;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private Integer cost;

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