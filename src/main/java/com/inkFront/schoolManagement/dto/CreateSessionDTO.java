package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSessionDTO {
    @NotBlank
    private String name; // "2025/2026"
}