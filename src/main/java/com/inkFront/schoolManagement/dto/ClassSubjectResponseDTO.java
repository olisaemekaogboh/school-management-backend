package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ClassSubject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubjectResponseDTO {

    private Long id;
    private Long classId;
    private String className;
    private String classArm;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    public static ClassSubjectResponseDTO fromEntity(ClassSubject classSubject) {
        if (classSubject == null) {
            return null;
        }

        return ClassSubjectResponseDTO.builder()
                .id(classSubject.getId())
                .classId(
                        classSubject.getSchoolClass() != null
                                ? classSubject.getSchoolClass().getId()
                                : null
                )
                .className(
                        classSubject.getSchoolClass() != null
                                ? classSubject.getSchoolClass().getClassName()
                                : null
                )
                .classArm(
                        classSubject.getSchoolClass() != null
                                ? classSubject.getSchoolClass().getArm()
                                : null
                )
                .subjectId(
                        classSubject.getSubject() != null
                                ? classSubject.getSubject().getId()
                                : null
                )
                .subjectName(
                        classSubject.getSubject() != null
                                ? classSubject.getSubject().getName()
                                : null
                )
                .subjectCode(
                        classSubject.getSubject() != null
                                ? classSubject.getSubject().getCode()
                                : null
                )
                .build();
    }
}