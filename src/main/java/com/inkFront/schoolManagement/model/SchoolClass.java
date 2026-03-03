// src/main/java/com/inkFront/schoolManagement/model/SchoolClass.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String className; // e.g., "JSS 1", "SSS 2"

    private String classCode; // e.g., "JSS1", "SSS2"

    @Enumerated(EnumType.STRING)
    private ClassCategory category; // NURSERY, PRIMARY, JUNIOR_SECONDARY, SENIOR_SECONDARY

    private String description;

    @ManyToOne
    @JoinColumn(name = "class_teacher_id")
    private Teacher classTeacher;

    @OneToMany(mappedBy = "studentClass")
    private List<Student> students = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "class_subjects", joinColumns = @JoinColumn(name = "class_id"))
    @Column(name = "subject")
    private List<String> subjects = new ArrayList<>();

    private Integer capacity; // Maximum number of students

    private Integer currentEnrollment;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (classCode == null) {
            classCode = generateClassCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateClassCode() {
        if (className == null) return null;
        return className.replace(" ", "").toUpperCase();
    }

    public enum ClassCategory {
        NURSERY, PRIMARY, JUNIOR_SECONDARY, SENIOR_SECONDARY
    }
}