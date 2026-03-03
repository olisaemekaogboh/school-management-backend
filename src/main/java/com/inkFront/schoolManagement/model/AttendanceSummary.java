// src/main/java/com/inkFront/schoolManagement/model/AttendanceSummary.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_summary", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "term", "session"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result.Term term;

    private int totalSchoolDays;
    private int daysPresent;
    private int daysAbsent;
    private int daysLate;
    private int daysExcused;

    private double attendancePercentage;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculatePercentage();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculatePercentage();
    }

    // Changed from private to public
    public void calculatePercentage() {
        if (totalSchoolDays > 0) {
            this.attendancePercentage = (daysPresent * 100.0) / totalSchoolDays;
        } else {
            this.attendancePercentage = 0;
        }
    }
}