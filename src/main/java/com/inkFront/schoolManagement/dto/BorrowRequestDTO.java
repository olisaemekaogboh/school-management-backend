package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequestDTO {
    @NotNull
    private Long bookId;

    // Exactly one of these should be provided by frontend
    private Long studentId;
    private Long teacherId;

    // ISO string dates recommended (or your own format)
    private String dueDate;
}