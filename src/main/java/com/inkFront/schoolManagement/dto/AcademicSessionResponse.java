// src/main/java/com/inkFront/schoolManagement/dto/AcademicSessionResponse.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AcademicSessionResponse {
    private Long id;
    private String name;
    private Integer startYear;
    private Integer endYear;
    private Boolean active;
    private LocalDateTime createdAt;
}