package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.BookBorrowing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookBorrowingRepository extends JpaRepository<BookBorrowing, Long> {

    List<BookBorrowing> findByStudent_Id(Long studentId);

    List<BookBorrowing> findByTeacher_Id(Long teacherId);

    List<BookBorrowing> findByStatus(BookBorrowing.BorrowingStatus status);

    List<BookBorrowing> findByStatusAndDueDateBefore(
            BookBorrowing.BorrowingStatus status,
            LocalDate date
    );
}