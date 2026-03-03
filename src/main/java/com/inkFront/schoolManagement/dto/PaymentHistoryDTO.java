// src/main/java/com/inkFront/schoolManagement/dto/PaymentHistoryDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Fee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDTO {
    private Long id;
    private Long feeId;
    private String feeType;
    private String description;
    private Double amount;
    private Double paidAmount;
    private Double balance;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String paymentReference;
    private String term;
    private String session;
    private String studentName;
    private String admissionNumber;
}