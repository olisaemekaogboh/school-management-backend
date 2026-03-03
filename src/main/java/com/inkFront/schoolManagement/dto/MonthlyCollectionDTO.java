// src/main/java/com/inkFront/schoolManagement/dto/MonthlyCollectionDTO.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCollectionDTO {
    private Integer year;
    private Integer month;
    private String monthName;
    private Double amount;
    private Long transactionCount;
    private Double averagePerTransaction;
}