package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.TeacherSubject;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSubjectResponseDTO {

    private Long id;
    private Long teacherId;
    private String teacherName;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private String className;
    private String classArm;

    public static TeacherSubjectResponseDTO fromEntity(TeacherSubject entity) {
        String teacherName = entity.getTeacher().getFirstName() + " " + entity.getTeacher().getLastName();

        return TeacherSubjectResponseDTO.builder()
                .id(entity.getId())
                .teacherId(entity.getTeacher().getId())
                .teacherName(teacherName)
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .subjectCode(entity.getSubject().getCode())
                .className(entity.getClassName())
                .classArm(entity.getClassArm())
                .build();
    }
}