package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequestDTO {

    @NotNull
    private Long bookId;

    // Optional legacy support
    private Long studentId;
    private Long teacherId;

    // Preferred identifiers
    private String studentAdmissionNumber;
    private String teacherEmployeeId;

    // ISO date string: yyyy-MM-dd
    private String dueDate;

    private String remarks;
}