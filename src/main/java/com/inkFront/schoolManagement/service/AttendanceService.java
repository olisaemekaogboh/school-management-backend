package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    Attendance markAttendance(Long studentId, LocalDate date, String session,
                              Result.Term term, Attendance.AttendanceStatus status, String remarks);

    List<Attendance> markBulkAttendance(List<Long> studentIds, LocalDate date,
                                        String session, Result.Term term,
                                        Attendance.AttendanceStatus status);

    Attendance getStudentAttendance(Long studentId, LocalDate date, String session, Result.Term term);

    List<Attendance> getStudentTermAttendance(Long studentId, String session, Result.Term term);

    AttendanceSummary getStudentTermSummary(Long studentId, String session, Result.Term term);

    Map<String, Object> getStudentSessionSummary(Long studentId, String session);

    List<Attendance> getClassAttendance(Long classId, LocalDate date, String session, Result.Term term);

    Map<String, Object> getClassTermStatistics(Long classId, String session, Result.Term term);

    Map<String, Object> getSchoolAttendanceStatistics(String session, Result.Term term);

    List<LocalDate> initializeSchoolDays(List<LocalDate> dates, String session, Result.Term term);

    void calculateAllTermSummaries(String session, Result.Term term);
}