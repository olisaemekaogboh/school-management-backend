// src/main/java/com/inkFront/schoolManagement/dto/AcademicSessionRequest.java
package com.inkFront.schoolManagement.dto;

import lombok.Data;

@Data
public class AcademicSessionRequest {
    private String name;       // "2025/2026"
    private Integer startYear;  // 2025
    private Integer endYear;    // 2026
    private Boolean active;     // optional
}