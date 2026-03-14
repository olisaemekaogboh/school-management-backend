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

    private boolean promoted;
    private String promotionRemark;

    private Map<String, Double> subjectAnnualTotals = new HashMap<>();
    private Map<String, Double> subjectAverages = new HashMap<>();

    private String classTeacherRemark;
    private String principalRemark;

    public static SessionResultResponseDTO fromEntity(SessionResult sr) {
        Student student = sr.getStudent();

        return new SessionResultResponseDTO(
                sr.getId(),
                student != null ? student.getId() : null,
                student != null ? (student.getFirstName() + " " + student.getLastName()) : null,
                student != null ? student.getAdmissionNumber() : null,
                student != null ? student.getStudentClass() : null,
                student != null ? student.getClassArm() : null,
                sr.getSession(),
                sr.getFirstTermTotal(),
                sr.getSecondTermTotal(),
                sr.getThirdTermTotal(),
                sr.getFirstTermAverage(),
                sr.getSecondTermAverage(),
                sr.getThirdTermAverage(),
                sr.getFirstTermPosition(),
                sr.getSecondTermPosition(),
                sr.getThirdTermPosition(),
                sr.getAnnualTotal(),
                sr.getAnnualAverage(),
                sr.getAnnualPositionInClass(),
                sr.getAnnualPositionInArm(),
                sr.getAnnualPositionInSchool(),
                sr.getTotalSchoolDays(),
                sr.getTotalDaysPresent(),
                sr.getTotalDaysAbsent(),
                sr.getAttendancePercentage(),
                sr.isPromoted(),
                sr.getPromotionRemark(),
                sr.getSubjectAnnualTotals() != null ? new HashMap<>(sr.getSubjectAnnualTotals()) : new HashMap<>(),
                sr.getSubjectAverages() != null ? new HashMap<>(sr.getSubjectAverages()) : new HashMap<>(),
                sr.getClassTeacherRemark(),
                sr.getPrincipalRemark()
        );
    }
}