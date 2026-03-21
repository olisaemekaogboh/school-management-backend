package com.inkFront.schoolManagement.dto;

import lombok.Data;

@Data
public class BookBorrowingDTO {

    private Long id;

    private Long bookId;
    private String bookTitle;

    private Long studentId;
    private String studentAdmissionNumber;
    private String studentName;

    private Long teacherId;
    private String teacherEmployeeId;
    private String teacherName;

    private String borrowDate;
    private String dueDate;
    private String returnDate;

    private String status; // BORROWED, RETURNED, LOST, OVERDUE, RENEWED
    private String remarks;
}