package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
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

    private boolean printable;
    private String printLockMessage;

    public static SessionResultSummaryDTO fromSessionResult(SessionResult sessionResult) {
        if (sessionResult == null) {
            return null;
        }

        Student student = sessionResult.getStudent();

        return SessionResultSummaryDTO.builder()
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null
                        ? (safe(student.getFirstName()) + " " + safe(student.getLastName())).trim()
                        : null)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .studentClass(student != null ? student.getStudentClass() : null)
                .classArm(student != null ? student.getClassArm() : null)
                .session(sessionResult.getSession())
                .annualAverage(sessionResult.getAnnualAverage() != null ? sessionResult.getAnnualAverage() : 0.0)
                .attendancePercentage(sessionResult.getAttendancePercentage() != null ? sessionResult.getAttendancePercentage() : 0.0)
                .promoted(sessionResult.isPromoted())
                .position(sessionResult.getAnnualPositionInClass())
                .printable(sessionResult.isPrintable())
                .printLockMessage(sessionResult.getPrintLockMessage())
                .build();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}