package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrintableStatusUpdateDTO {

    @NotNull(message = "Printable status is required")
    private Boolean printable;

    private String printLockMessage;
}