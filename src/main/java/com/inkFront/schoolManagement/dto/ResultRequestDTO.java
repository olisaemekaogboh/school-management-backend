// src/main/java/com/inkFront/schoolManagement/dto/ResultRequestDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultRequestDTO {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotBlank(message = "Session is required")
    @Pattern(
            regexp = "^\\d{4}/\\d{4}$",
            message = "Session must be in format YYYY/YYYY (e.g., 2025/2026)"
    )
    private String session;

    @NotNull(message = "Term is required")
    private Result.Term term;

    @Min(value = 0, message = "Resumption test must be at least 0")
    @Max(value = 5, message = "Resumption test cannot exceed 5 marks")
    private Double resumptionTest;

    @Min(value = 0, message = "Assignments must be at least 0")
    @Max(value = 10, message = "Assignments cannot exceed 10 marks")
    private Double assignments;

    @Min(value = 0, message = "Project must be at least 0")
    @Max(value = 10, message = "Project cannot exceed 10 marks")
    private Double project;

    @Min(value = 0, message = "Midterm test must be at least 0")
    @Max(value = 10, message = "Midterm test cannot exceed 10 marks")
    private Double midtermTest;

    @Min(value = 0, message = "Second test must be at least 0")
    @Max(value = 5, message = "Second test cannot exceed 5 marks")
    private Double secondTest;

    @Min(value = 0, message = "Examination must be at least 0")
    @Max(value = 60, message = "Examination cannot exceed 60 marks")
    private Double examination;

    private String remarks;

    public static ResultRequestDTO fromScoresMap(
            Long studentId,
            Long subjectId,
            String session,
            Result.Term term,
            Map<String, Double> scores
    ) {
        return ResultRequestDTO.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .session(session)
                .term(term)
                .resumptionTest(scores.getOrDefault("resumptionTest", 0.0))
                .assignments(scores.getOrDefault("assignments", 0.0))
                .project(scores.getOrDefault("project", 0.0))
                .midtermTest(scores.getOrDefault("midtermTest", 0.0))
                .secondTest(scores.getOrDefault("secondTest", 0.0))
                .examination(scores.getOrDefault("examination", 0.0))
                .build();
    }

    public boolean isValid() {
        double caTotal = getContinuousAssessmentTotal();
        double examScore = examination != null ? examination : 0.0;
        return caTotal <= 40 && examScore <= 60 && (caTotal + examScore) <= 100;
    }

    public double getContinuousAssessmentTotal() {
        return (resumptionTest != null ? resumptionTest : 0.0) +
                (assignments != null ? assignments : 0.0) +
                (project != null ? project : 0.0) +
                (midtermTest != null ? midtermTest : 0.0) +
                (secondTest != null ? secondTest : 0.0);
    }

    public double getTotalScore() {
        return getContinuousAssessmentTotal() + (examination != null ? examination : 0.0);
    }

    public String getGrade() {
        double total = getTotalScore();
        if (total >= 70) return "A";
        if (total >= 60) return "B";
        if (total >= 50) return "C";
        if (total >= 45) return "D";
        if (total >= 40) return "E";
        return "F";
    }

    public String getRemark() {
        double total = getTotalScore();
        if (total >= 70) return "Excellent";
        if (total >= 60) return "Very Good";
        if (total >= 50) return "Good";
        if (total >= 45) return "Pass";
        if (total >= 40) return "Fair";
        return "Fail";
    }
}