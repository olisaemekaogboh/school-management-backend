// src/main/java/com/inkFront/schoolManagement/model/SessionResult.java
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

    // Term Totals
    private double firstTermTotal;
    private double secondTermTotal;
    private double thirdTermTotal;

    // Term Averages
    private double firstTermAverage;
    private double secondTermAverage;
    private double thirdTermAverage;

    // Term Positions
    private Integer firstTermPosition;
    private Integer secondTermPosition;
    private Integer thirdTermPosition;

    // Annual Summary
    private double annualTotal;
    private double annualAverage;
    private Integer annualPositionInClass;
    private Integer annualPositionInArm;
    private Integer annualPositionInSchool;

    // Attendance Summary
    private int totalSchoolDays;
    private int totalDaysPresent;
    private int totalDaysAbsent;
    private double attendancePercentage;

    // Promotion Status
    private boolean promoted;
    private String promotionRemark;

    // Subject Performance Summary
    @ElementCollection
    @CollectionTable(name = "session_subject_totals",
            joinColumns = @JoinColumn(name = "session_result_id"))
    @MapKeyColumn(name = "subject")
    @Column(name = "total_score")
    private Map<String, Double> subjectAnnualTotals = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "session_subject_averages",
            joinColumns = @JoinColumn(name = "session_result_id"))
    @MapKeyColumn(name = "subject")
    @Column(name = "average_score")
    private Map<String, Double> subjectAverages = new HashMap<>();

    // Teacher's Comments
    private String classTeacherRemark;
    private String principalRemark;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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

    // ADD THIS METHOD - calculate annual averages
    public void calculateAnnualAverage() {
        // Calculate annual totals
        this.annualTotal = firstTermTotal + secondTermTotal + thirdTermTotal;

        // Calculate annual averages
        double totalAverage = firstTermAverage + secondTermAverage + thirdTermAverage;
        this.annualAverage = totalAverage / 3;

        // Determine promotion status (average >= 40 and attendance >= 75%)
        boolean passedAcademically = annualAverage >= 40;
        boolean metAttendanceRequirement = attendancePercentage >= 75;

        this.promoted = passedAcademically && metAttendanceRequirement;

        if (!passedAcademically) {
            this.promotionRemark = "Failed to meet academic requirements";
        } else if (!metAttendanceRequirement) {
            this.promotionRemark = "Failed to meet attendance requirements";
        } else if (promoted) {
            this.promotionRemark = "Promoted to next class";
        } else {
            this.promotionRemark = "Retaining in current class";
        }
    }
}