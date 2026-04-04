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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultVisibilityStatus visibilityStatus = ResultVisibilityStatus.HIDDEN;

    @Column(length = 300)
    private String visibilityMessage =
            "Result is not yet published for student or parent access.";

    private LocalDateTime publishedAt;

    @Column(length = 150)
    private String publishedByName;

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

    public ResultVisibilityStatus getResultVisibilityStatus() {
        return visibilityStatus;
    }

    public void setResultVisibilityStatus(ResultVisibilityStatus visibilityStatus) {
        this.visibilityStatus = visibilityStatus;
    }

    public boolean isVisibleToStudentOrParent() {
        return visibilityStatus == ResultVisibilityStatus.PUBLISHED
                || visibilityStatus == ResultVisibilityStatus.PRINTABLE;
    }

    public void markHidden(String message) {
        this.visibilityStatus = ResultVisibilityStatus.HIDDEN;
        this.visibilityMessage = isBlank(message)
                ? "Result is not yet published for student or parent access."
                : message.trim();
        this.printable = false;

        if (this.printLockMessage == null || this.printLockMessage.isBlank()) {
            this.printLockMessage =
                    "Printable result is locked. The admin will unlock it when the result is ready.";
        }
    }

    public void markStaffOnly(String message) {
        this.visibilityStatus = ResultVisibilityStatus.STAFF_ONLY;
        this.visibilityMessage = isBlank(message)
                ? "Result is available to staff only and hidden from student or parent access."
                : message.trim();
        this.printable = false;

        if (this.printLockMessage == null || this.printLockMessage.isBlank()) {
            this.printLockMessage =
                    "Printable result is locked. The admin will unlock it when the result is ready.";
        }
    }

    public void markPublished(String message, String publishedByName) {
        this.visibilityStatus = ResultVisibilityStatus.PUBLISHED;
        this.visibilityMessage = isBlank(message)
                ? "Result has been published for viewing."
                : message.trim();
        this.printable = false;
        this.publishedAt = LocalDateTime.now();
        this.publishedByName = publishedByName;

        if (this.printLockMessage == null || this.printLockMessage.isBlank()) {
            this.printLockMessage =
                    "Printable result is locked. The admin will unlock it when the result is ready.";
        }
    }

    public void markPrintable(String message, String publishedByName) {
        this.visibilityStatus = ResultVisibilityStatus.PRINTABLE;
        this.visibilityMessage = isBlank(message)
                ? "Result has been published for viewing and printing."
                : message.trim();
        this.printable = true;
        this.printLockMessage = "Printable result is available.";
        this.publishedAt = LocalDateTime.now();
        this.publishedByName = publishedByName;
    }

    public void resetPublicationState(String reason) {
        this.visibilityStatus = ResultVisibilityStatus.HIDDEN;
        this.visibilityMessage = isBlank(reason)
                ? "Result was updated and has been hidden until it is republished."
                : reason.trim();
        this.printable = false;
        this.printLockMessage = "Printable result is locked until admin approves";
        this.publishedAt = null;
        this.publishedByName = null;
    }

    public void refreshCompletionStatus() {
        this.completed = this.classTeacherSigned && this.adminSigned;

        if (!this.completed) {
            this.printable = false;

            if (this.visibilityStatus == ResultVisibilityStatus.PRINTABLE) {
                this.visibilityStatus = ResultVisibilityStatus.PUBLISHED;
            }

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

        resetPublicationState("Result modified. Requires admin republication.");
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateAggregates();
        refreshCompletionStatus();

        if (visibilityStatus == null) {
            visibilityStatus = ResultVisibilityStatus.HIDDEN;
        }

        if (visibilityMessage == null || visibilityMessage.isBlank()) {
            visibilityMessage = "Result is not yet published for student or parent access.";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAggregates();
        refreshCompletionStatus();

        if (visibilityStatus == null) {
            visibilityStatus = ResultVisibilityStatus.HIDDEN;
        }

        if (visibilityMessage == null || visibilityMessage.isBlank()) {
            visibilityMessage = "Result is not yet published for student or parent access.";
        }
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}