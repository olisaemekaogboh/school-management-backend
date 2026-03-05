package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
            String title, String author, String isbn
    );

    List<Book> findByCategoryIgnoreCase(String category);
}