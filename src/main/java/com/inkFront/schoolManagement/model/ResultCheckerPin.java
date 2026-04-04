package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "result_checker_pins",
        indexes = {
                @Index(name = "idx_result_checker_pin_scope_session", columnList = "pinScope, session"),
                @Index(name = "idx_result_checker_pin_active", columnList = "active"),
                @Index(name = "idx_result_checker_pin_student", columnList = "student_id"),
                @Index(name = "idx_result_checker_pin_class", columnList = "class_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultCheckerPin {

    public enum PinScope {
        TERM,
        SESSION
    }

    public enum TargetType {
        STUDENT,
        CLASS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PinScope pinScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @Column(nullable = false, length = 30)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Result.Term term;

    @Column(nullable = false)
    private Integer maxUsage = 1;

    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime expiresAt;

    @Column(length = 250)
    private String notes;

    @Column(length = 150)
    private String createdByName;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean canStillBeUsed() {
        return active
                && !isExpired()
                && maxUsage != null
                && usedCount != null
                && usedCount < maxUsage;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (maxUsage == null || maxUsage < 1) {
            maxUsage = 1;
        }

        if (usedCount == null || usedCount < 0) {
            usedCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (maxUsage == null || maxUsage < 1) {
            maxUsage = 1;
        }

        if (usedCount == null || usedCount < 0) {
            usedCount = 0;
        }
    }
}