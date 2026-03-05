package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookDTO {
    private Long id;

    @NotBlank
    private String title;

    private String author;
    private String isbn;
    private String category;

    @Min(0)
    private Integer totalCopies;

    @Min(0)
    private Integer availableCopies;
}