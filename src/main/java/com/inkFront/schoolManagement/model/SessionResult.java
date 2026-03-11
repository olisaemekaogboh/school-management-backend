package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "session_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "session"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String session;

    private double firstTermTotal;
    private double secondTermTotal;
    private double thirdTermTotal;

    private double firstTermAverage;
    private double secondTermAverage;
    private double thirdTermAverage;

    private Integer firstTermPosition;
    private Integer secondTermPosition;
    private Integer thirdTermPosition;

    private double annualTotal;
    private double annualAverage;
    private Integer annualPositionInClass;
    private Integer annualPositionInArm;
    private Integer annualPositionInSchool;

    private int totalSchoolDays;
    private int totalDaysPresent;
    private int totalDaysAbsent;
    private double attendancePercentage;

    private boolean promoted;
    private String promotionRemark;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_subject_totals",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject")
    @Column(name = "total_score")
    private Map<String, Double> subjectAnnualTotals = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_subject_averages",
            joinColumns = @JoinColumn(name = "session_result_id")
    )
    @MapKeyColumn(name = "subject")
    @Column(name = "average_score")
    private Map<String, Double> subjectAverages = new HashMap<>();

    private String classTeacherRemark;
    private String principalRemark;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Transient
    private int completedTermsCount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateAnnualAverage();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAnnualAverage();
    }

    public void calculateAnnualAverage() {
        this.annualTotal = firstTermTotal + secondTermTotal + thirdTermTotal;

        int termCount = 0;
        double averageSum = 0;

        if (hasFirstTerm()) {
            averageSum += firstTermAverage;
            termCount++;
        }

        if (hasSecondTerm()) {
            averageSum += secondTermAverage;
            termCount++;
        }

        if (hasThirdTerm()) {
            averageSum += thirdTermAverage;
            termCount++;
        }

        this.completedTermsCount = termCount;
        this.annualAverage = termCount > 0 ? averageSum / termCount : 0;

        boolean sessionCompleted = termCount == 3;
        boolean passedAcademically = annualAverage >= 40;
        boolean metAttendanceRequirement = attendancePercentage >= 75;

        if (!sessionCompleted) {
            this.promoted = false;
            this.promotionRemark = "Session still in progress";
            return;
        }

        this.promoted = passedAcademically && metAttendanceRequirement;

        if (!passedAcademically) {
            this.promotionRemark = "Failed to meet academic requirements";
        } else if (!metAttendanceRequirement) {
            this.promotionRemark = "Failed to meet attendance requirements";
        } else {
            this.promotionRemark = "Promoted to next class";
        }
    }

    private boolean hasFirstTerm() {
        return firstTermPosition != null || firstTermTotal > 0 || firstTermAverage > 0;
    }

    private boolean hasSecondTerm() {
        return secondTermPosition != null || secondTermTotal > 0 || secondTermAverage > 0;
    }

    private boolean hasThirdTerm() {
        return thirdTermPosition != null || thirdTermTotal > 0 || thirdTermAverage > 0;
    }
}