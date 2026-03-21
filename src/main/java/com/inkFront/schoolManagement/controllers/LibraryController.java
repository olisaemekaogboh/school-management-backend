package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.BookBorrowingDTO;
import com.inkFront.schoolManagement.dto.BookDTO;
import com.inkFront.schoolManagement.dto.BorrowRequestDTO;
import com.inkFront.schoolManagement.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class LibraryController {

    private final LibraryService libraryService;

    @PostMapping("/books")
    public BookDTO createBook(@Valid @RequestBody BookDTO dto) {
        return libraryService.createBook(dto);
    }

    @PutMapping("/books/{id}")
    public BookDTO updateBook(@PathVariable Long id, @Valid @RequestBody BookDTO dto) {
        return libraryService.updateBook(id, dto);
    }

    @GetMapping("/books/{id}")
    public BookDTO getBook(@PathVariable Long id) {
        return libraryService.getBook(id);
    }

    @DeleteMapping("/books/{id}")
    public void deleteBook(@PathVariable Long id) {
        libraryService.deleteBook(id);
    }

    @GetMapping("/books")
    public List<BookDTO> getAllBooks() {
        return libraryService.getAllBooks();
    }

    @GetMapping("/books/search")
    public List<BookDTO> searchBooks(@RequestParam String term) {
        return libraryService.searchBooks(term);
    }

    @GetMapping("/books/category/{category}")
    public List<BookDTO> byCategory(@PathVariable String category) {
        return libraryService.getBooksByCategory(category);
    }

    @PostMapping("/borrow")
    public BookBorrowingDTO borrow(@Valid @RequestBody BorrowRequestDTO dto) {
        return libraryService.borrowBook(dto);
    }

    @PostMapping("/return/{id}")
    public BookBorrowingDTO returnBook(@PathVariable Long id) {
        return libraryService.returnBook(id);
    }

    @PostMapping("/renew/{id}")
    public BookBorrowingDTO renew(@PathVariable Long id) {
        return libraryService.renewBook(id);
    }

    @PostMapping("/lost/{id}")
    public BookBorrowingDTO lost(@PathVariable Long id) {
        return libraryService.reportLost(id);
    }

    @GetMapping("/borrowings")
    public List<BookBorrowingDTO> getAllBorrowings() {
        return libraryService.getAllBorrowings();
    }

    @GetMapping("/borrowings/student/{studentId}")
    public List<BookBorrowingDTO> borrowingsByStudent(@PathVariable Long studentId) {
        return libraryService.getBorrowingsByStudent(studentId);
    }

    @GetMapping("/borrowings/teacher/{teacherId}")
    public List<BookBorrowingDTO> borrowingsByTeacher(@PathVariable Long teacherId) {
        return libraryService.getBorrowingsByTeacher(teacherId);
    }

    @GetMapping("/borrowings/overdue")
    public List<BookBorrowingDTO> overdue() {
        return libraryService.getOverdueBorrowings();
    }

    @GetMapping("/statistics")
    public Map<String, Object> statistics() {
        return libraryService.getLibraryStatistics();
    }
}