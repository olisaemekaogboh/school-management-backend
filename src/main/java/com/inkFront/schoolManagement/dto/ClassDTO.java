package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SchoolClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDTO {

    private Long id;
    private String className;
    private String arm;
    private String classCode;
    private SchoolClass.ClassCategory category;
    private String description;
    private Long classTeacherId;
    private String classTeacherName;
    private List<String> subjects;
    private Integer capacity;
    private Integer currentEnrollment;
    private Integer studentCount;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private List<StudentResponseDTO> students;

    public static ClassDTO fromEntity(SchoolClass schoolClass) {
        return fromEntity(schoolClass, new ArrayList<>(), new ArrayList<>());
    }

    public static ClassDTO fromEntity(SchoolClass schoolClass, List<String> subjects) {
        return fromEntity(schoolClass, subjects, new ArrayList<>());
    }

    public static ClassDTO fromEntity(
            SchoolClass schoolClass,
            List<String> subjects,
            List<StudentResponseDTO> students
    ) {
        int count = students != null ? students.size() : 0;

        return ClassDTO.builder()
                .id(schoolClass.getId())
                .className(schoolClass.getClassName())
                .arm(schoolClass.getArm())
                .classCode(schoolClass.getClassCode())
                .category(schoolClass.getCategory())
                .description(schoolClass.getDescription())
                .classTeacherId(
                        schoolClass.getClassTeacher() != null
                                ? schoolClass.getClassTeacher().getId()
                                : null
                )
                .classTeacherName(
                        schoolClass.getClassTeacher() != null
                                ? ((schoolClass.getClassTeacher().getFirstName() == null ? "" : schoolClass.getClassTeacher().getFirstName())
                                + " " +
                                (schoolClass.getClassTeacher().getLastName() == null ? "" : schoolClass.getClassTeacher().getLastName())).trim()
                                : null
                )
                .subjects(subjects != null ? subjects : new ArrayList<>())
                .capacity(schoolClass.getCapacity())
                .currentEnrollment(count)
                .studentCount(count)
                .createdAt(schoolClass.getCreatedAt())
                .updatedAt(schoolClass.getUpdatedAt())
                .students(students != null ? students : new ArrayList<>())
                .build();
    }
}