package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassSubjectRequestDTO {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotNull(message = "Subject id is required")
    private Long subjectId;
}