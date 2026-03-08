package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SchoolClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDTO {
    private Long id;
    private String className;
    private String arm; // ADD THIS FIELD - it's missing!
    private String classCode;
    private SchoolClass.ClassCategory category;
    private String description;
    private Long classTeacherId;
    private String classTeacherName;
    private List<String> subjects;
    private Integer capacity;
    private Integer currentEnrollment;
    private List<StudentResponseDTO> students;

    public static ClassDTO fromEntity(SchoolClass schoolClass) {
        return ClassDTO.builder()
                .id(schoolClass.getId())
                .className(schoolClass.getClassName())
                .arm(schoolClass.getArm()) // ADD THIS
                .classCode(schoolClass.getClassCode())
                .category(schoolClass.getCategory())
                .description(schoolClass.getDescription())
                .classTeacherId(schoolClass.getClassTeacher() != null ? schoolClass.getClassTeacher().getId() : null)
                .classTeacherName(schoolClass.getClassTeacher() != null ?
                        schoolClass.getClassTeacher().getFirstName() + " " + schoolClass.getClassTeacher().getLastName() : null)
                .subjects(schoolClass.getSubjects())
                .capacity(schoolClass.getCapacity())
                .currentEnrollment(schoolClass.getCurrentEnrollment())
                .build();
    }
}