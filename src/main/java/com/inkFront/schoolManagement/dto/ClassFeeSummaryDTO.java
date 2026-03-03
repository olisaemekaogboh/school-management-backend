// src/main/java/com/inkFront/schoolManagement/dto/ClassFeeSummaryDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassFeeSummaryDTO {
    private String studentClass;
    private Integer totalStudents;
    private Integer studentsWithFees;
    private Integer fullyPaid;
    private Integer partiallyPaid;
    private Integer notPaid;
    private Integer overdue;
    private Double totalExpected;
    private Double totalCollected;
    private Double totalOutstanding;
    private Double collectionPercentage;
    private Double averagePerStudent;
}