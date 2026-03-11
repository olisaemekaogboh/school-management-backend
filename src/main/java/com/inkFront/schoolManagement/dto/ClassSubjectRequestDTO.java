package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassSubjectRequestDTO {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Class arm is required")
    private String classArm;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;
}