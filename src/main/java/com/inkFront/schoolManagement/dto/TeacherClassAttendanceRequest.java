package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TeacherClassAttendanceRequest {
    private List<Long> studentIds;
    private LocalDate date;
    private String session;
    private Result.Term term;
    private Attendance.AttendanceStatus status;
}