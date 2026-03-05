// src/main/java/com/inkFront/schoolManagement/model/AcademicSession.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "academic_sessions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Example: "2025/2026"
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer startYear;

    @Column(nullable = false)
    private Integer endYear;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}