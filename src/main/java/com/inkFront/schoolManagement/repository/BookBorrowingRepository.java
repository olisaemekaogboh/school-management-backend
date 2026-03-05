package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.BookBorrowing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookBorrowingRepository extends JpaRepository<BookBorrowing, Long> {
    List<BookBorrowing> findByStudentId(Long studentId);
    List<BookBorrowing> findByTeacherId(Long teacherId);

    // Borrowings by status (ENUM)
    List<BookBorrowing> findByStatus(BookBorrowing.BorrowingStatus status);

}