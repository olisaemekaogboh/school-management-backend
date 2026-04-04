package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResultCheckerPinCreateDTO {

    @NotBlank(message = "pinScope is required")
    private String pinScope; // TERM or SESSION

    @NotBlank(message = "targetType is required")
    private String targetType; // STUDENT or CLASS

    private Long studentId;

    private Long classId;

    @NotBlank(message = "session is required")
    private String session;

    private Result.Term term;

    @NotNull(message = "count is required")
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 500, message = "count cannot exceed 500")
    private Integer count = 1;

    @NotNull(message = "maxUsage is required")
    @Min(value = 1, message = "maxUsage must be at least 1")
    @Max(value = 100, message = "maxUsage cannot exceed 100")
    private Integer maxUsage = 1;

    private LocalDateTime expiresAt;

    private String notes;
}