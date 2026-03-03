// src/main/java/com/inkFront/schoolManagement/dto/SessionResultSummaryDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SessionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResultSummaryDTO {

    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;
    private String session;
    private double annualAverage;
    private double attendancePercentage;
    private boolean promoted;
    private Integer position;

    public static SessionResultSummaryDTO fromSessionResult(SessionResult sessionResult) {
        if (sessionResult == null) {
            return null;
        }

        return SessionResultSummaryDTO.builder()
                .studentId(sessionResult.getStudent().getId())
                .studentName(sessionResult.getStudent().getFirstName() + " " +
                        sessionResult.getStudent().getLastName())
                .admissionNumber(sessionResult.getStudent().getAdmissionNumber())
                .studentClass(sessionResult.getStudent().getStudentClass())
                .classArm(sessionResult.getStudent().getClassArm())
                .session(sessionResult.getSession())
                .annualAverage(sessionResult.getAnnualAverage())
                .attendancePercentage(sessionResult.getAttendancePercentage())
                .promoted(sessionResult.isPromoted())
                .position(sessionResult.getAnnualPositionInClass())
                .build();
    }
}