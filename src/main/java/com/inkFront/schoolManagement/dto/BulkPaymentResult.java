// src/main/java/com/inkFront/schoolManagement/dto/BulkPaymentResult.java
package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPaymentResult {
    private Integer totalProcessed;
    private Integer successful;
    private Integer failed;
    private Double totalAmount;
    private List<Long> successfulIds = new ArrayList<>();
    private List<Long> failedIds = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String bulkReference;
}