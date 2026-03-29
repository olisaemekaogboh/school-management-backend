package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultResponseDTO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;
    private String classCode;

    private String subject;
    private String session;
    private String term;

    private double resumptionTest;
    private double assignments;
    private double project;
    private double midtermTest;
    private double secondTest;
    private double continuousAssessment;
    private double examination;
    private double total;

    private String grade;
    private String remarks;

    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResultResponseDTO fromResult(Result result) {
        if (result == null) {
            return null;
        }

        Student student = result.getStudent();

        String studentFullName = student == null
                ? null
                : (
                (student.getFirstName() != null ? student.getFirstName() : "") + " " +
                        (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                        (student.getLastName() != null ? student.getLastName() : "")
        ).replaceAll("\\s+", " ").trim();

        return ResultResponseDTO.builder()
                .id(result.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(studentFullName)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .studentClass(student != null ? student.getStudentClass() : null)
                .classArm(student != null ? student.getClassArm() : null)
                .classCode(student != null && student.getSchoolClass() != null
                        ? student.getSchoolClass().getClassCode()
                        : null)
                .subject(result.getSubject() != null ? result.getSubject().getName() : null)
                .session(result.getSession())
                .term(result.getTerm() != null ? result.getTerm().name() : null)
                .resumptionTest(result.getResumptionTest())
                .assignments(result.getAssignments())
                .project(result.getProject())
                .midtermTest(result.getMidtermTest())
                .secondTest(result.getSecondTest())
                .continuousAssessment(result.getContinuousAssessment())
                .examination(result.getExamination())
                .total(result.getTotal())
                .grade(result.getGrade())
                .remarks(result.getRemarks())
                .positionInClass(result.getPositionInClass())
                .positionInArm(result.getPositionInArm())
                .positionInSchool(result.getPositionInSchool())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}