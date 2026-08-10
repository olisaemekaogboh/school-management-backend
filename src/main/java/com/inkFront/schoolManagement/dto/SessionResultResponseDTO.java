package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
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

    private String classTeacherRemark;
    private String principalRemark;
    private boolean promoted;
    private String promotionRemark;

    private ResultVisibilityStatus visibilityStatus;
    private String visibilityMessage;
    private boolean printable;
    private String printLockMessage;
    private LocalDateTime publishedAt;
    private String publishedByName;

    private Map<String, Double> subjectAnnualTotals = new HashMap<>();
    private Map<String, Double> subjectAverages = new HashMap<>();

    private Map<String, Double> firstTermSubjectScores = new HashMap<>();
    private Map<String, Double> secondTermSubjectScores = new HashMap<>();
    private Map<String, Double> thirdTermSubjectScores = new HashMap<>();

    public static SessionResultResponseDTO fromEntity(SessionResult sr) {
        if (sr == null) {
            return null;
        }

        Student student = sr.getStudent();

        SessionResultResponseDTO dto = new SessionResultResponseDTO();
        dto.setId(sr.getId());
        dto.setStudentId(student != null ? student.getId() : null);
        dto.setStudentName(student != null
                ? (safe(student.getFirstName()) + " " + safe(student.getMiddleName()) + " " + safe(student.getLastName()))
                .replaceAll("\\s+", " ")
                .trim()
                : null);
        dto.setAdmissionNumber(student != null ? student.getAdmissionNumber() : null);
        dto.setStudentClass(student != null ? student.getStudentClass() : null);
        dto.setClassArm(student != null ? student.getClassArm() : null);

        dto.setSession(sr.getSession());

        dto.setFirstTermTotal(sr.getFirstTermTotal() != null ? sr.getFirstTermTotal() : 0.0);
        dto.setSecondTermTotal(sr.getSecondTermTotal() != null ? sr.getSecondTermTotal() : 0.0);
        dto.setThirdTermTotal(sr.getThirdTermTotal() != null ? sr.getThirdTermTotal() : 0.0);

        dto.setFirstTermAverage(sr.getFirstTermAverage() != null ? sr.getFirstTermAverage() : 0.0);
        dto.setSecondTermAverage(sr.getSecondTermAverage() != null ? sr.getSecondTermAverage() : 0.0);
        dto.setThirdTermAverage(sr.getThirdTermAverage() != null ? sr.getThirdTermAverage() : 0.0);

        dto.setFirstTermPosition(sr.getFirstTermPosition());
        dto.setSecondTermPosition(sr.getSecondTermPosition());
        dto.setThirdTermPosition(sr.getThirdTermPosition());

        dto.setAnnualTotal(sr.getAnnualTotal() != null ? sr.getAnnualTotal() : 0.0);
        dto.setAnnualAverage(sr.getAnnualAverage() != null ? sr.getAnnualAverage() : 0.0);
        dto.setAnnualPositionInClass(sr.getAnnualPositionInClass());
        dto.setAnnualPositionInArm(sr.getAnnualPositionInArm());
        dto.setAnnualPositionInSchool(sr.getAnnualPositionInSchool());

        dto.setTotalSchoolDays(sr.getTotalSchoolDays() != null ? sr.getTotalSchoolDays() : 0);
        dto.setTotalDaysPresent(sr.getTotalDaysPresent() != null ? sr.getTotalDaysPresent() : 0);
        dto.setTotalDaysAbsent(sr.getTotalDaysAbsent() != null ? sr.getTotalDaysAbsent() : 0);
        dto.setAttendancePercentage(sr.getAttendancePercentage() != null ? sr.getAttendancePercentage() : 0.0);

        dto.setPromoted(sr.isPromoted());
        dto.setPromotionRemark(sr.getPromotionRemark());

        dto.setVisibilityStatus(sr.getVisibilityStatus());
        dto.setVisibilityMessage(sr.getVisibilityMessage());
        dto.setPrintable(sr.isPrintable());
        dto.setPrintLockMessage(sr.getPrintLockMessage());
        dto.setPublishedAt(sr.getPublishedAt());
        dto.setPublishedByName(sr.getPublishedByName());

        dto.setClassTeacherRemark(null);
        dto.setPrincipalRemark(null);

        dto.setSubjectAnnualTotals(
                sr.getSubjectAnnualTotals() != null ? new HashMap<>(sr.getSubjectAnnualTotals()) : new HashMap<>()
        );
        dto.setSubjectAverages(
                sr.getSubjectAverages() != null ? new HashMap<>(sr.getSubjectAverages()) : new HashMap<>()
        );
        dto.setFirstTermSubjectScores(
                sr.getFirstTermSubjectScores() != null ? new HashMap<>(sr.getFirstTermSubjectScores()) : new HashMap<>()
        );
        dto.setSecondTermSubjectScores(
                sr.getSecondTermSubjectScores() != null ? new HashMap<>(sr.getSecondTermSubjectScores()) : new HashMap<>()
        );
        dto.setThirdTermSubjectScores(
                sr.getThirdTermSubjectScores() != null ? new HashMap<>(sr.getThirdTermSubjectScores()) : new HashMap<>()
        );

        return dto;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}