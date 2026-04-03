package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "term_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TermResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result.Term term;

    @Column(nullable = false)
    private boolean printable = false;

    @Column(length = 300)
    private String printLockMessage =
            "Printable result is locked. The admin will unlock it when the result is ready.";

    @OneToMany(
            mappedBy = "termResult",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private List<Result> subjectResults = new ArrayList<>();

    private double totalScore;
    private double average;
    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;

    @Column(length = 1000)
    private String classTeacherComment;

    @Column(length = 1000)
    private String principalComment;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String characterTraitsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String psychomotorTraitsJson;

    private LocalDate nextTermBegins;

    private int totalDaysPresent;
    private int totalDaysAbsent;
    private int totalSchoolDays;
    private int daysPresent;
    private int daysAbsent;
    private double attendancePercentage;

    @Column(nullable = false)
    private boolean classTeacherSigned = false;

    @Column(nullable = false)
    private boolean adminSigned = false;

    @Column(nullable = false)
    private boolean completed = false;

    private LocalDateTime classTeacherSignedAt;
    private LocalDateTime adminSignedAt;

    @Column(length = 500)
    private String classTeacherSignatureUrl;

    @Column(length = 500)
    private String adminSignatureUrl;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void addResult(Result result) {
        if (result == null) {
            return;
        }

        if (subjectResults == null) {
            subjectResults = new ArrayList<>();
        }

        if (!subjectResults.contains(result)) {
            subjectResults.add(result);
        }

        result.setTermResult(this);
    }

    public void removeResult(Result result) {
        if (result == null || subjectResults == null) {
            return;
        }

        subjectResults.remove(result);
        result.setTermResult(null);
    }

    public void refreshCompletionStatus() {
        this.completed = this.classTeacherSigned && this.adminSigned;

        if (!this.completed) {
            this.printable = false;
            if (this.printLockMessage == null || this.printLockMessage.isBlank()) {
                this.printLockMessage = "Result is incomplete. Awaiting required signatures.";
            }
        }
    }

    public void resetApprovalState() {
        this.classTeacherSigned = false;
        this.adminSigned = false;
        this.completed = false;

        this.classTeacherSignedAt = null;
        this.adminSignedAt = null;

        this.classTeacherSignatureUrl = null;
        this.adminSignatureUrl = null;

        this.printable = false;
        this.printLockMessage = "Result modified. Requires re-approval.";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateAggregates();
        refreshCompletionStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAggregates();
        refreshCompletionStatus();
    }

    public void calculateAggregates() {
        if (subjectResults != null && !subjectResults.isEmpty()) {
            this.totalScore = subjectResults.stream()
                    .mapToDouble(Result::getTotal)
                    .sum();

            this.average = subjectResults.stream()
                    .mapToDouble(Result::getTotal)
                    .average()
                    .orElse(0.0);
        } else {
            this.totalScore = 0.0;
            this.average = 0.0;
        }
    }
}