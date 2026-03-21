package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String isbn;
    private String publisher;
    private String publicationDate;
    private String edition;
    private String category;
    private String shelfLocation;
    private String description;
    private String status;

    @Min(value = 0, message = "Total copies cannot be negative")
    private Integer totalCopies;

    @Min(value = 0, message = "Available copies cannot be negative")
    private Integer availableCopies;
}