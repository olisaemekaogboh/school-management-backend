package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimetableDTO {

    private Long id;

    @NotNull(message = "schoolClassId is required")
    private Long schoolClassId;

    @NotNull(message = "teacherId is required")
    private Long teacherId;

    // ✅ matches Timetable.subject
    @NotBlank(message = "subject is required")
    private String subject;

    // MONDAY..SUNDAY
    @NotBlank(message = "dayOfWeek is required")
    private String dayOfWeek;

    // "09:00"
    @NotBlank(message = "startTime is required")
    private String startTime;

    // "10:00"
    @NotBlank(message = "endTime is required")
    private String endTime;

    private String room;

    // "2025/2026"
    @NotBlank(message = "session is required")
    private String session;

    // FIRST/SECOND/THIRD
    @NotBlank(message = "term is required")
    private String term;

    private Boolean active;
}