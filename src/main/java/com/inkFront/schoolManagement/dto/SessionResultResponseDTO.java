// src/main/java/com/inkFront/schoolManagement/dto/SessionResultResponseDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SessionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResultResponseDTO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;

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
    private int daysPresent;
    private int daysAbsent;
    private double attendancePercentage;

    // Promotion Status
    private boolean promoted;
    private String promotionRemark;

    // Subject Performance
    private Map<String, Double> subjectAnnualAverages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SessionResultResponseDTO fromSessionResult(SessionResult sessionResult) {
        if (sessionResult == null) {
            return null;
        }

        return SessionResultResponseDTO.builder()
                .id(sessionResult.getId())
                .studentId(sessionResult.getStudent().getId())
                .studentName(sessionResult.getStudent().getFirstName() + " " +
                        (sessionResult.getStudent().getMiddleName() != null ?
                                sessionResult.getStudent().getMiddleName() + " " : "") +
                        sessionResult.getStudent().getLastName())
                .admissionNumber(sessionResult.getStudent().getAdmissionNumber())
                .studentClass(sessionResult.getStudent().getStudentClass())
                .classArm(sessionResult.getStudent().getClassArm())
                .session(sessionResult.getSession())
                .firstTermTotal(sessionResult.getFirstTermTotal())
                .secondTermTotal(sessionResult.getSecondTermTotal())
                .thirdTermTotal(sessionResult.getThirdTermTotal())
                .firstTermAverage(sessionResult.getFirstTermAverage())
                .secondTermAverage(sessionResult.getSecondTermAverage())
                .thirdTermAverage(sessionResult.getThirdTermAverage())
                .firstTermPosition(sessionResult.getFirstTermPosition())
                .secondTermPosition(sessionResult.getSecondTermPosition())
                .thirdTermPosition(sessionResult.getThirdTermPosition())
                .annualTotal(sessionResult.getAnnualTotal())
                .annualAverage(sessionResult.getAnnualAverage())
                .annualPositionInClass(sessionResult.getAnnualPositionInClass())
                .annualPositionInArm(sessionResult.getAnnualPositionInArm())
                .annualPositionInSchool(sessionResult.getAnnualPositionInSchool())
                .totalSchoolDays(sessionResult.getTotalSchoolDays())
                .daysPresent(sessionResult.getTotalDaysPresent())
                .daysAbsent(sessionResult.getTotalDaysAbsent())
                .attendancePercentage(sessionResult.getAttendancePercentage())
                .promoted(sessionResult.isPromoted())
                .promotionRemark(sessionResult.getPromotionRemark())
                .subjectAnnualAverages(sessionResult.getSubjectAverages())
                .createdAt(sessionResult.getCreatedAt())
                .updatedAt(sessionResult.getUpdatedAt())
                .build();
    }
}