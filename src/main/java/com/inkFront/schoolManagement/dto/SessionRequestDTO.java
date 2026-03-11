// src/main/java/com/inkFront/schoolManagement/dto/SessionRequestDTO.java
package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SessionRequestDTO {

    @NotBlank(message = "Session name is required")
    private String sessionName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private boolean active;
}