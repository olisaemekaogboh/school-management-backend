// src/main/java/com/inkFront/schoolManagement/dto/TermResultSummaryDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.TermResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermResultSummaryDTO {

    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;
    private String session;
    private String term;
    private double totalScore;
    private double average;
    private Integer position;
    private double attendancePercentage;

    public static TermResultSummaryDTO fromTermResult(TermResult termResult) {
        if (termResult == null) {
            return null;
        }

        return TermResultSummaryDTO.builder()
                .studentId(termResult.getStudent().getId())
                .studentName(termResult.getStudent().getFirstName() + " " +
                        termResult.getStudent().getLastName())
                .admissionNumber(termResult.getStudent().getAdmissionNumber())
                .studentClass(termResult.getStudent().getStudentClass())
                .classArm(termResult.getStudent().getClassArm())
                .session(termResult.getSession())
                .term(termResult.getTerm() != null ? termResult.getTerm().toString() : null)
                .totalScore(termResult.getTotalScore())
                .average(termResult.getAverage())
                .position(termResult.getPositionInClass())
                .attendancePercentage(termResult.getAttendancePercentage())
                .build();
    }
}