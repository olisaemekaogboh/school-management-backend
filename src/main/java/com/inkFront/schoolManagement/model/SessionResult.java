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

    public void calculateAnnualAverage() {
        double first = firstTermAverage != null ? firstTermAverage : 0.0;
        double second = secondTermAverage != null ? secondTermAverage : 0.0;
        double third = thirdTermAverage != null ? thirdTermAverage : 0.0;

        this.annualAverage = (first + second + third) / 3.0;
    }

    @PrePersist
    protected void onCreate() {
        if (visibilityStatus == null) {
            visibilityStatus = ResultVisibilityStatus.HIDDEN;
        }

        if (visibilityMessage == null || visibilityMessage.isBlank()) {
            visibilityMessage = "Result is not yet published for student or parent access.";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (visibilityStatus == null) {
            visibilityStatus = ResultVisibilityStatus.HIDDEN;
        }

        if (visibilityMessage == null || visibilityMessage.isBlank()) {
            visibilityMessage = "Result is not yet published for student or parent access.";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}