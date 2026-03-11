// src/main/java/com/inkFront/schoolManagement/dto/ResultResponseDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
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

    private String subject;
    private String session;
    private String term;

    // Assessment components
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

    // Position fields (optional, for term results)
    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public static ResultResponseDTO fromResult(Result result) {
        if (result == null) {
            return null;
        }

        String studentFullName = result.getStudent().getFirstName() + " " +
                (result.getStudent().getMiddleName() != null ? result.getStudent().getMiddleName() + " " : "") +
                result.getStudent().getLastName();

        return ResultResponseDTO.builder()
                .id(result.getId())
                .studentId(result.getStudent().getId())
                .studentName(studentFullName)
                .admissionNumber(result.getStudent().getAdmissionNumber())
                .studentClass(result.getStudent().getStudentClass())
                .classArm(result.getStudent().getClassArm())
                .subject(result.getSubject() != null ? result.getSubject().getName() : null)
                .session(result.getSession())
                .term(result.getTerm() != null ? result.getTerm().toString() : null)
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
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}