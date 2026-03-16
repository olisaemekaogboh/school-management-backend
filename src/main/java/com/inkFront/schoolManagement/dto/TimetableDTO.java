package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableDTO {

    private Long id;

    @NotNull(message = "schoolClassId is required")
    private Long schoolClassId;

    @NotNull(message = "teacherId is required")
    private Long teacherId;

    private String teacherName;
    private String className;
    private String classArm;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "dayOfWeek is required")
    private String dayOfWeek;

    @NotBlank(message = "startTime is required")
    private String startTime;

    @NotBlank(message = "endTime is required")
    private String endTime;

    private String room;

    @NotBlank(message = "session is required")
    private String session;

    @NotBlank(message = "term is required")
    private String term;

    private Boolean active;
}