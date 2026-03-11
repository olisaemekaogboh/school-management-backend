package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherSubjectRequestDTO {

    @NotNull(message = "Teacher id is required")
    private Long teacherId;

    @NotNull(message = "Subject id is required")
    private Long subjectId;

    @NotBlank(message = "Class name is required")
    private String className;

    private String classArm;
}