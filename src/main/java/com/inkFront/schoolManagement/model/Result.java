package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "term", "session", "subject"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // NEW: Add relationship to TermResult
    @ManyToOne
    @JoinColumn(name = "term_result_id")
    private TermResult termResult;

    @Column(nullable = false)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(nullable = false)
    private String subject;

    // Continuous Assessment Components (40 marks total)
    private double resumptionTest; // 5 marks
    private double assignments;    // 10 marks
    private double project;        // 10 marks
    private double midtermTest;    // 10 marks
    private double secondTest;     // 5 marks

    // Examination
    private double examination;    // 60 marks

    // Calculated fields
    private double continuousAssessment;
    private double total;
    private String grade;
    private String remarks;

    // Ranking fields
    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateScores();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateScores();
    }

    private void calculateScores() {
        // Calculate Continuous Assessment (max 40)
        this.continuousAssessment = resumptionTest + assignments + project + midtermTest + secondTest;

        // Calculate Total (max 100)
        this.total = continuousAssessment + examination;

        // Determine Grade
        if (total >= 70) {
            this.grade = "A";
            this.remarks = "Excellent";
        } else if (total >= 60) {
            this.grade = "B";
            this.remarks = "Very Good";
        } else if (total >= 50) {
            this.grade = "C";
            this.remarks = "Good";
        } else if (total >= 45) {
            this.grade = "D";
            this.remarks = "Pass";
        } else if (total >= 40) {
            this.grade = "E";
            this.remarks = "Fair";
        } else {
            this.grade = "F";
            this.remarks = "Fail";
        }
    }

    public enum Term {
        FIRST, SECOND, THIRD
    }
}