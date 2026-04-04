package com.inkFront.schoolManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "session_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SessionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String session;

    private Double firstTermTotal = 0.0;
    private Double secondTermTotal = 0.0;
    private Double thirdTermTotal = 0.0;

    private Double firstTermAverage = 0.0;
    private Double secondTermAverage = 0.0;
    private Double thirdTermAverage = 0.0;

    private Double annualTotal = 0.0;
    private Double annualAverage = 0.0;

    private Integer firstTermPosition;
    private Integer secondTermPosition;
    private Integer thirdTermPosition;

    private Integer annualPositionInClass;
    private Integer annualPositionInArm;
    private Integer annualPositionInSchool;

    private Integer totalSchoolDays = 0;
    private Integer totalDaysPresent = 0;
    private Integer totalDaysAbsent = 0;
    private Double attendancePercentage = 0.0;

    private boolean promoted = false;

    @Column(length = 500)
    private String promotionRemark;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false, length = 30)
    private ResultVisibilityStatus resultVisibilityStatus = ResultVisibilityStatus.HIDDEN;

    @Column(nullable = false)
    private boolean printable = false;

    @Column(length = 300)
    private String printLockMessage =
            "Printable result is locked. The admin will unlock it when the result is ready.";

    @Column(name = "visibility_message", length = 300)
    private String visibilityMessage =
            "Session result is currently hidden from students and parents.";

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by_name", length = 150)
    private String publishedByName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_result_subject_averages",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject_name")
    @Column(name = "average_score")
    private Map<String, Double> subjectAverages = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_result_subject_annual_totals",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject_name")
    @Column(name = "annual_total")
    private Map<String, Double> subjectAnnualTotals = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_result_subject_first_term_scores",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject_name")
    @Column(name = "score")
    private Map<String, Double> firstTermSubjectScores = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_result_subject_second_term_scores",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject_name")
    @Column(name = "score")
    private Map<String, Double> secondTermSubjectScores = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_result_subject_third_term_scores",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject_name")
    @Column(name = "score")
    private Map<String, Double> thirdTermSubjectScores = new HashMap<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean isVisibleToStudentOrParent() {
        return resultVisibilityStatus == ResultVisibilityStatus.PUBLISHED
                || resultVisibilityStatus == ResultVisibilityStatus.PRINTABLE;
    }

    public boolean isHiddenFromStudentAndParent() {
        return resultVisibilityStatus == ResultVisibilityStatus.HIDDEN
                || resultVisibilityStatus == ResultVisibilityStatus.STAFF_ONLY;
    }

    public void markHidden(String message) {
        this.resultVisibilityStatus = ResultVisibilityStatus.HIDDEN;
        this.printable = false;
        this.publishedAt = null;
        this.publishedByName = null;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Session result is currently hidden from students and parents.";
        this.printLockMessage = "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    public void markStaffOnly(String message) {
        this.resultVisibilityStatus = ResultVisibilityStatus.STAFF_ONLY;
        this.printable = false;
        this.publishedAt = null;
        this.publishedByName = null;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Session result is available to staff only.";
        this.printLockMessage = "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    public void markPublished(String message, String publisherName) {
        this.resultVisibilityStatus = ResultVisibilityStatus.PUBLISHED;
        this.printable = false;
        this.publishedAt = LocalDateTime.now();
        this.publishedByName = normalizeText(publisherName);
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Session result has been published for student and parent viewing.";
        this.printLockMessage = "Printable result is locked. View is allowed, printing is not yet enabled.";
    }

    public void markPrintable(String message, String publisherName) {
        this.resultVisibilityStatus = ResultVisibilityStatus.PRINTABLE;
        this.printable = true;
        this.publishedAt = LocalDateTime.now();
        this.publishedByName = normalizeText(publisherName);
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Session result has been published and printing is enabled.";
        this.printLockMessage = "Printable result is available.";
    }

    public void resetPublicationState(String message) {
        this.resultVisibilityStatus = ResultVisibilityStatus.STAFF_ONLY;
        this.printable = false;
        this.publishedAt = null;
        this.publishedByName = null;
        this.visibilityMessage = hasText(message)
                ? message.trim()
                : "Session result updated. Awaiting admin release.";
        this.printLockMessage = "Printable result is locked. The admin will unlock it when the result is ready.";
    }

    public void calculateAnnualAverage() {
        double first = firstTermAverage != null ? firstTermAverage : 0.0;
        double second = secondTermAverage != null ? secondTermAverage : 0.0;
        double third = thirdTermAverage != null ? thirdTermAverage : 0.0;
        this.annualAverage = (first + second + third) / 3.0;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        syncPublicationFlags();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        syncPublicationFlags();
    }

    private void syncPublicationFlags() {
        if (resultVisibilityStatus == null) {
            resultVisibilityStatus = printable
                    ? ResultVisibilityStatus.PRINTABLE
                    : ResultVisibilityStatus.HIDDEN;
        }

        if (resultVisibilityStatus != ResultVisibilityStatus.PRINTABLE) {
            printable = false;
        }

        if (!hasText(visibilityMessage)) {
            visibilityMessage = switch (resultVisibilityStatus) {
                case HIDDEN -> "Session result is currently hidden from students and parents.";
                case STAFF_ONLY -> "Session result is available to staff only.";
                case PUBLISHED -> "Session result has been published for student and parent viewing.";
                case PRINTABLE -> "Session result has been published and printing is enabled.";
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