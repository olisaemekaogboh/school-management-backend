package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.BookBorrowingDTO;
import com.inkFront.schoolManagement.dto.BookDTO;
import com.inkFront.schoolManagement.dto.BorrowRequestDTO;

import java.util.List;

public interface LibraryService {
    BookDTO createBook(BookDTO dto);
    BookDTO updateBook(Long id, BookDTO dto);
    BookDTO getBook(Long id);
    void deleteBook(Long id);
    List<BookDTO> getAllBooks();
    List<BookDTO> searchBooks(String term);
    List<BookDTO> getBooksByCategory(String category);

    BookBorrowingDTO borrowBook(BorrowRequestDTO dto);
    BookBorrowingDTO returnBook(Long borrowingId);
    BookBorrowingDTO renewBook(Long borrowingId);
    BookBorrowingDTO reportLost(Long borrowingId);

    List<BookBorrowingDTO> getBorrowingsByStudent(Long studentId);
    List<BookBorrowingDTO> getBorrowingsByTeacher(Long teacherId);
    List<BookBorrowingDTO> getOverdueBorrowings();
}