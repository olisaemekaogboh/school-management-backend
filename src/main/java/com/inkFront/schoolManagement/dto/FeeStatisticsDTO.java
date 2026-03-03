// src/main/java/com/inkFront/schoolManagement/dto/FeeStatisticsDTO.java
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
public class FeeStatisticsDTO {
    private Double totalExpected;
    private Double totalCollected;
    private Double totalOutstanding;
    private Long totalFees;
    private Long totalStudents;
    private Long paidCount;
    private Long partialCount;
    private Long pendingCount;
    private Long overdueCount;
    private Long waivedCount;
    private Double collectionRate;
    private Double outstandingRate;
    private Map<String, Long> statusBreakdown;
    private Map<String, Double> typeBreakdown;
}