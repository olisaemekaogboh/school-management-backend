// src/main/java/com/inkFront/schoolManagement/model/TermResult.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// In TermResult.java - Fix the cascade configuration
@Entity
@Table(name = "term_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TermResult {

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


    @OneToMany(mappedBy = "termResult",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<Result> subjectResults = new ArrayList<>();

    // Helper methods to maintain both sides of the relationship
    public void addResult(Result result) {
        subjectResults.add(result);
        result.setTermResult(this);
    }

    public void removeResult(Result result) {
        subjectResults.remove(result);
        result.setTermResult(null);
    }




    private double totalScore;
    private double average;
    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;
    private String classTeacherComment;
    private String principalComment;
    private int totalDaysPresent;
    private int totalDaysAbsent;
    private int totalSchoolDays;
    private int daysPresent;
    private int daysAbsent;
    private double attendancePercentage;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAggregates();
    }

    public void calculateAggregates() {
        if (subjectResults != null && !subjectResults.isEmpty()) {
            this.totalScore = subjectResults.stream()
                    .mapToDouble(Result::getTotal)
                    .sum();
            this.average = subjectResults.stream()
                    .mapToDouble(Result::getTotal)
                    .average()
                    .orElse(0);
        } else {
            this.totalScore = 0;
            this.average = 0;
        }
    }
}