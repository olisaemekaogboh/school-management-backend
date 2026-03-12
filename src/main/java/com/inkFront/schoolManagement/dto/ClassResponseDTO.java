package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SchoolClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponseDTO {

    private Long id;
    private String className;
    private String arm;
    private String classCode;
    private String category;
    private String description;
    private Integer capacity;
    private Integer currentEnrollment;
    private Integer studentCount;

    private Long classTeacherId;
    private String classTeacherName;

    public static ClassResponseDTO fromEntity(SchoolClass schoolClass) {
        return fromEntity(schoolClass, 0);
    }

    public static ClassResponseDTO fromEntity(SchoolClass schoolClass, int studentCount) {
        if (schoolClass == null) {
            return null;
        }

        String teacherName = null;
        Long teacherId = null;

        if (schoolClass.getClassTeacher() != null) {
            teacherId = schoolClass.getClassTeacher().getId();

            String firstName = schoolClass.getClassTeacher().getFirstName() == null
                    ? ""
                    : schoolClass.getClassTeacher().getFirstName();

            String lastName = schoolClass.getClassTeacher().getLastName() == null
                    ? ""
                    : schoolClass.getClassTeacher().getLastName();

            teacherName = (firstName + " " + lastName).trim();
        }

        return ClassResponseDTO.builder()
                .id(schoolClass.getId())
                .className(schoolClass.getClassName())
                .arm(schoolClass.getArm())
                .classCode(schoolClass.getClassCode())
                .category(schoolClass.getCategory() != null ? schoolClass.getCategory().name() : null)
                .description(schoolClass.getDescription())
                .capacity(schoolClass.getCapacity())
                .currentEnrollment(studentCount)
                .studentCount(studentCount)
                .classTeacherId(teacherId)
                .classTeacherName(teacherName)
                .build();
    }
}