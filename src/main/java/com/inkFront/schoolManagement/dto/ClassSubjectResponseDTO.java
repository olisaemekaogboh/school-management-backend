package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.ClassSubject;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSubjectResponseDTO {

    private Long id;
    private String className;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    public static ClassSubjectResponseDTO fromEntity(ClassSubject entity) {
        return ClassSubjectResponseDTO.builder()
                .id(entity.getId())
                .className(entity.getClassName())
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .subjectCode(entity.getSubject().getCode())
                .build();
    }
}