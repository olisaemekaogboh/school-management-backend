// src/main/java/com/inkFront/schoolManagement/dto/GraduationListDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationListDTO {

    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private double finalAverage;
    private double attendancePercentage;
    private Integer position;
    private Map<String, Double> subjectAverages;
}