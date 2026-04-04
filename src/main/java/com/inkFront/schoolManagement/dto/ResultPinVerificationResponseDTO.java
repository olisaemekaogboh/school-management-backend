package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultPinVerificationResponseDTO {

    private boolean valid;
    private String message;
    private Long pinId;
    private Integer remainingUsage;
}