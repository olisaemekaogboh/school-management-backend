package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResultPinVerificationRequestDTO {

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotBlank(message = "session is required")
    private String session;

    private String term;

    @NotBlank(message = "pin is required")
    private String pin;
}