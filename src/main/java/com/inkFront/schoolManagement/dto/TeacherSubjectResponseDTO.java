package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.TeacherSubject;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSubjectResponseDTO {

    private Long id; // assignment id
    private Long teacherId;
    private String teacherName;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private Long classId;
    private String className;
    private String classArm;

    // OLD SIGNATURE - keep this so existing method references still compile
    public static TeacherSubjectResponseDTO fromEntity(TeacherSubject entity) {
        String teacherName = entity.getTeacher().getFirstName() + " " + entity.getTeacher().getLastName();

        return TeacherSubjectResponseDTO.builder()
                .id(entity.getId())
                .teacherId(entity.getTeacher().getId())
                .teacherName(teacherName)
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .subjectCode(entity.getSubject().getCode())
                .classId(null)
                .className(entity.getClassName())
                .classArm(entity.getClassArm())
                .build();
    }

    // NEW SIGNATURE - use this where you already resolved the SchoolClass
    public static TeacherSubjectResponseDTO fromEntity(TeacherSubject entity, SchoolClass schoolClass) {
        String teacherName = entity.getTeacher().getFirstName() + " " + entity.getTeacher().getLastName();

        return TeacherSubjectResponseDTO.builder()
                .id(entity.getId())
                .teacherId(entity.getTeacher().getId())
                .teacherName(teacherName)
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .subjectCode(entity.getSubject().getCode())
                .classId(schoolClass != null ? schoolClass.getId() : null)
                .className(entity.getClassName())
                .classArm(entity.getClassArm())
                .build();
    }
}