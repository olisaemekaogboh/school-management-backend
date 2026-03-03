// src/main/java/com/inkFront/schoolManagement/dto/FeeDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Fee;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDTO {

    private Long id;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private String classArm;

    @NotBlank(message = "Session is required")
    private String session;

    @NotNull(message = "Term is required")
    private Fee.Term term;

    @NotNull(message = "Fee type is required")
    private Fee.FeeType feeType;

    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private Double paidAmount;
    private Double balance;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;

    private LocalDate paidDate;
    private Fee.PaymentStatus status;
    private String paymentReference;
    private String paymentMethod;

    // Add this field - it was missing
    private String notes;

    // Reminder tracking fields
    private Integer reminderCount;
    private LocalDate lastReminderSent;

    public static FeeDTO fromFee(Fee fee) {
        return FeeDTO.builder()
                .id(fee.getId())
                .studentId(fee.getStudent().getId())
                .studentName(fee.getStudent().getFirstName() + " " + fee.getStudent().getLastName())
                .admissionNumber(fee.getStudent().getAdmissionNumber())
                .studentClass(fee.getStudent().getStudentClass())
                .classArm(fee.getStudent().getClassArm())
                .session(fee.getSession())
                .term(fee.getTerm())
                .feeType(fee.getFeeType())
                .description(fee.getDescription())
                .amount(fee.getAmount())
                .paidAmount(fee.getPaidAmount())
                .balance(fee.getBalance())
                .dueDate(fee.getDueDate())
                .paidDate(fee.getPaidDate())
                .status(fee.getStatus())
                .paymentReference(fee.getPaymentReference())
                .paymentMethod(fee.getPaymentMethod())
                .notes(fee.getNotes())  // Add this line
                .reminderCount(fee.getReminderCount())
                .lastReminderSent(fee.getLastReminderSent())
                .build();
    }
}