package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "term", "session", "subject_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_result_id")
    private TermResult termResult;

    @Column(nullable = false)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    private double resumptionTest;
    private double assignments;
    private double project;
    private double midtermTest;
    private double secondTest;

    private double examination;

    private double continuousAssessment;
    private double total;
    private String grade;
    private String remarks;

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
        this.continuousAssessment =
                resumptionTest + assignments + project + midtermTest + secondTest;

        this.total = continuousAssessment + examination;

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