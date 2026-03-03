// src/main/java/com/inkFront/schoolManagement/service/AttendanceService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    // Mark attendance for a student
    Attendance markAttendance(Long studentId, LocalDate date, String session,
                              Result.Term term, Attendance.AttendanceStatus status, String remarks);

    // Mark attendance for multiple students
    List<Attendance> markBulkAttendance(List<Long> studentIds, LocalDate date,
                                        String session, Result.Term term,
                                        Attendance.AttendanceStatus status);

    // Get attendance for a student on a specific date
    Attendance getStudentAttendance(Long studentId, LocalDate date, String session, Result.Term term);

    // Get all attendance records for a student in a term
    List<Attendance> getStudentTermAttendance(Long studentId, String session, Result.Term term);

    // Get attendance summary for a student in a term
    AttendanceSummary getStudentTermSummary(Long studentId, String session, Result.Term term);

    // Get attendance summary for a student in a session
    Map<String, Object> getStudentSessionSummary(Long studentId, String session);

    // Get all attendance records for a class on a specific date
    List<Attendance> getClassAttendance(String className, LocalDate date, String session, Result.Term term);

    // Get attendance statistics for a class in a term
    Map<String, Object> getClassTermStatistics(String className, String session, Result.Term term);

    // Get attendance statistics for the whole school
    Map<String, Object> getSchoolAttendanceStatistics(String session, Result.Term term);

    // Initialize school days for a term
    List<LocalDate> initializeSchoolDays(List<LocalDate> dates, String session, Result.Term term);

    // Calculate attendance summary for all students in a term
    void calculateAllTermSummaries(String session, Result.Term term);
}