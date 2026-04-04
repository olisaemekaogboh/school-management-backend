package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResultPinVerificationRequestDTO {

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotBlank(message = "session is required")
    private String session;

    private Result.Term term;

    @NotBlank(message = "pin is required")
    private String pin;
}