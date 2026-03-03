// src/main/java/com/inkFront/schoolManagement/dto/DefaulterDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaulterDTO {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;
    private String parentName;
    private String parentPhone;
    private Double outstandingBalance;
    private Integer overdueFees;
    private Integer totalFees;
    private List<FeeDTO> fees;
    private String mostUrgentFeeType;
    private LocalDate earliestDueDate;
}