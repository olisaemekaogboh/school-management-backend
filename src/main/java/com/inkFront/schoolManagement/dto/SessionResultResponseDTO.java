package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Map<String, Double> subjectAnnualTotals = new HashMap<>();
    private Map<String, Double> subjectAverages = new HashMap<>();

    private Map<String, Double> firstTermSubjectScores = new HashMap<>();
    private Map<String, Double> secondTermSubjectScores = new HashMap<>();
    private Map<String, Double> thirdTermSubjectScores = new HashMap<>();

    public static SessionResultResponseDTO fromEntity(SessionResult sr) {
        Student student = sr.getStudent();

        SessionResultResponseDTO dto = new SessionResultResponseDTO();
        dto.setId(sr.getId());
        dto.setStudentId(student != null ? student.getId() : null);
        dto.setStudentName(student != null ? (student.getFirstName() + " " + student.getLastName()).trim() : null);
        dto.setAdmissionNumber(student != null ? student.getAdmissionNumber() : null);
        dto.setStudentClass(student != null ? student.getStudentClass() : null);
        dto.setClassArm(student != null ? student.getClassArm() : null);

        dto.setSession(sr.getSession());

        dto.setFirstTermTotal(sr.getFirstTermTotal());
        dto.setSecondTermTotal(sr.getSecondTermTotal());
        dto.setThirdTermTotal(sr.getThirdTermTotal());

        dto.setFirstTermAverage(sr.getFirstTermAverage());
        dto.setSecondTermAverage(sr.getSecondTermAverage());
        dto.setThirdTermAverage(sr.getThirdTermAverage());

        dto.setFirstTermPosition(sr.getFirstTermPosition());
        dto.setSecondTermPosition(sr.getSecondTermPosition());
        dto.setThirdTermPosition(sr.getThirdTermPosition());

        dto.setAnnualTotal(sr.getAnnualTotal());
        dto.setAnnualAverage(sr.getAnnualAverage());
        dto.setAnnualPositionInClass(sr.getAnnualPositionInClass());
        dto.setAnnualPositionInArm(sr.getAnnualPositionInArm());
        dto.setAnnualPositionInSchool(sr.getAnnualPositionInSchool());

        dto.setTotalSchoolDays(sr.getTotalSchoolDays());
        dto.setTotalDaysPresent(sr.getTotalDaysPresent());
        dto.setTotalDaysAbsent(sr.getTotalDaysAbsent());
        dto.setAttendancePercentage(sr.getAttendancePercentage());

        dto.setPromoted(sr.isPromoted());
        dto.setPromotionRemark(sr.getPromotionRemark());

        // keep these fields for frontend compatibility even if SessionResult does not have them yet
        dto.setClassTeacherRemark(null);
        dto.setPrincipalRemark(null);

        dto.setSubjectAnnualTotals(
                sr.getSubjectAnnualTotals() != null
                        ? new HashMap<>(sr.getSubjectAnnualTotals())
                        : new HashMap<>()
        );

        dto.setSubjectAverages(
                sr.getSubjectAverages() != null
                        ? new HashMap<>(sr.getSubjectAverages())
                        : new HashMap<>()
        );

        dto.setFirstTermSubjectScores(
                sr.getFirstTermSubjectScores() != null
                        ? new HashMap<>(sr.getFirstTermSubjectScores())
                        : new HashMap<>()
        );

        dto.setSecondTermSubjectScores(
                sr.getSecondTermSubjectScores() != null
                        ? new HashMap<>(sr.getSecondTermSubjectScores())
                        : new HashMap<>()
        );

        dto.setThirdTermSubjectScores(
                sr.getThirdTermSubjectScores() != null
                        ? new HashMap<>(sr.getThirdTermSubjectScores())
                        : new HashMap<>()
        );

        return dto;
    }
}