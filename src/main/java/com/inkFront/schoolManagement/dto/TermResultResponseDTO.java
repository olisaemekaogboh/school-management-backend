// src/main/java/com/inkFront/schoolManagement/dto/TermResultResponseDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.TermResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermResultResponseDTO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;

    private String session;
    private String term;

    private double totalScore;
    private double average;
    private Integer positionInClass;
    private Integer positionInArm;
    private Integer positionInSchool;

    // Attendance fields
    private int totalSchoolDays;
    private int daysPresent;
    private int daysAbsent;
    private double attendancePercentage;

    private String classTeacherComment;
    private String principalComment;

    private List<ResultResponseDTO> subjectResults;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TermResultResponseDTO fromTermResult(TermResult termResult) {
        if (termResult == null) {
            return null;
        }

        List<ResultResponseDTO> subjectResults = termResult.getSubjectResults() != null ?
                termResult.getSubjectResults().stream()
                        .map(ResultResponseDTO::fromResult)
                        .collect(Collectors.toList()) : null;

        String studentFullName = termResult.getStudent().getFirstName() + " " +
                (termResult.getStudent().getMiddleName() != null ? termResult.getStudent().getMiddleName() + " " : "") +
                termResult.getStudent().getLastName();

        return TermResultResponseDTO.builder()
                .id(termResult.getId())
                .studentId(termResult.getStudent().getId())
                .studentName(studentFullName)
                .admissionNumber(termResult.getStudent().getAdmissionNumber())
                .studentClass(termResult.getStudent().getStudentClass())
                .classArm(termResult.getStudent().getClassArm())
                .session(termResult.getSession())
                .term(termResult.getTerm() != null ? termResult.getTerm().toString() : null)
                .totalScore(termResult.getTotalScore())
                .average(termResult.getAverage())
                .positionInClass(termResult.getPositionInClass())
                .positionInArm(termResult.getPositionInArm())
                .positionInSchool(termResult.getPositionInSchool())
                .totalSchoolDays(termResult.getTotalSchoolDays())
                .daysPresent(termResult.getDaysPresent())
                .daysAbsent(termResult.getDaysAbsent())
                .attendancePercentage(termResult.getAttendancePercentage())
                .classTeacherComment(termResult.getClassTeacherComment())
                .principalComment(termResult.getPrincipalComment())
                .subjectResults(subjectResults)
                .createdAt(termResult.getCreatedAt())
                .updatedAt(termResult.getUpdatedAt())
                .build();
    }
}