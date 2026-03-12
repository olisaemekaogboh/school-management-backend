package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "classes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"class_name", "arm"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false)
    private String className; // e.g. JSS 1

    @Column(nullable = false)
    private String arm; // e.g. A, B, C

    private String classCode; // e.g. JSS1-A

    @Enumerated(EnumType.STRING)
    private ClassCategory category;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;

    private Integer capacity;
    private Integer currentEnrollment;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (classCode == null || classCode.isBlank()) {
            classCode = generateClassCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (classCode == null || classCode.isBlank()) {
            classCode = generateClassCode();
        }
    }

    private String generateClassCode() {
        if (className == null) return null;

        String base = className.replace(" ", "").toUpperCase();
        if (arm == null || arm.isBlank()) {
            return base;
        }
        return base + "-" + arm.trim().toUpperCase();
    }

    public enum ClassCategory {
        NURSERY, PRIMARY, JUNIOR_SECONDARY, SENIOR_SECONDARY
    }
}