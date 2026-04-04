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

    public enum VisibilityStatus {
        HIDDEN,
        STAFF_ONLY,
        PUBLISHED,
        PRINTABLE
    }

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

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false, length = 30)
    private VisibilityStatus visibilityStatus = VisibilityStatus.STAFF_ONLY;

    @Column(name = "visibility_message", length = 300)
    private String visibilityMessage =
            "Result is available to staff only until admin releases it.";

    @Column(nullable = false)
    private boolean printable = false;

    @Column(length = 300)
    private String printLockMessage =
            "Printable result is locked. The admin will unlock it when the result is ready.";

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by_name", length = 150)
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


    @Column(columnDefinition = "TEXT")
    private String characterTraitsJson;


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

    public boolean isVisibleToStudentOrParent() {
        return visibilityStatus == VisibilityStatus.PUBLISHED
                || visibilityStatus == VisibilityStatus.PRINTABLE;
    }

    public void markHidden(String message) {
        this.visibilityStatus = VisibilityStatus.HIDDEN;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Result is currently hidden from students and parents.";
        this.printable = false;
        this.publishedAt = null;
        this.publishedByName = null;
        this.printLockMessage = "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    public void markStaffOnly(String message) {
        this.visibilityStatus = VisibilityStatus.STAFF_ONLY;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Result is available to staff only.";
        this.printable = false;
        this.publishedAt = null;
        this.publishedByName = null;
        this.printLockMessage = "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    public void markPublished(String message, String publisherName) {
        this.visibilityStatus = VisibilityStatus.PUBLISHED;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Result has been published for student and parent viewing.";
        this.printable = false;
        this.publishedAt = LocalDateTime.now();
        this.publishedByName = normalizeText(publisherName);
        this.printLockMessage = "Printable result is locked. View is allowed, printing is not yet enabled.";
    }

    public void markPrintable(String message, String publisherName) {
        this.visibilityStatus = VisibilityStatus.PRINTABLE;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Result has been published and printing is enabled.";
        this.printable = true;
        this.publishedAt = LocalDateTime.now();
        this.publishedByName = normalizeText(publisherName);
        this.printLockMessage = "Printable result is available.";
    }

    public void refreshCompletionStatus() {
        this.completed = this.classTeacherSigned && this.adminSigned;

        if (!this.completed) {
            this.printable = false;

            if (this.visibilityStatus == VisibilityStatus.PRINTABLE) {
                this.visibilityStatus = VisibilityStatus.STAFF_ONLY;
            }

            if (this.printLockMessage == null || this.printLockMessage.isBlank()) {
                this.printLockMessage = "Result is incomplete. Awaiting required signatures.";
            }

            if (this.visibilityMessage == null || this.visibilityMessage.isBlank()) {
                this.visibilityMessage = "Result is incomplete and cannot be released yet.";
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

        this.visibilityStatus = VisibilityStatus.STAFF_ONLY;
        this.visibilityMessage = "Result modified. Awaiting review before release.";
        this.printable = false;
        this.publishedAt = null;
        this.publishedByName = null;
        this.printLockMessage = "Result modified. Requires re-approval.";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateAggregates();
        refreshCompletionStatus();
        normalizeState();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAggregates();
        refreshCompletionStatus();
        normalizeState();
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

    private void normalizeState() {
        if (visibilityStatus == null) {
            visibilityStatus = printable ? VisibilityStatus.PRINTABLE : VisibilityStatus.STAFF_ONLY;
        }

        if (visibilityStatus != VisibilityStatus.PRINTABLE) {
            printable = false;
        }

        if (!hasText(visibilityMessage)) {
            visibilityMessage = switch (visibilityStatus) {
                case HIDDEN -> "Result is currently hidden from students and parents.";
                case STAFF_ONLY -> "Result is available to staff only.";
                case PUBLISHED -> "Result has been published for student and parent viewing.";
                case PRINTABLE -> "Result has been published and printing is enabled.";
            };
        }

        if (!hasText(printLockMessage)) {
            printLockMessage = printable
                    ? "Printable result is available."
                    : "Printable result is locked. The admin will unlock it when the result is ready.";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }
}