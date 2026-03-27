package com.inkFront.schoolManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public void calculateAnnualAverage() {
        double first = firstTermAverage != null ? firstTermAverage : 0.0;
        double second = secondTermAverage != null ? secondTermAverage : 0.0;
        double third = thirdTermAverage != null ? thirdTermAverage : 0.0;

        this.annualAverage = (first + second + third) / 3.0;
    }
}