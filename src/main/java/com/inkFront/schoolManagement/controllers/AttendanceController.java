// src/main/java/com/inkFront/schoolManagement/controllers/AttendanceController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);
    private final AttendanceService attendanceService;
    private final StudentRepository studentRepository; // You might need to inject this
    private final AttendanceRepository attendanceRepository; // You might need to inject this

    // Mark attendance for a single student
    @PostMapping("/student/{studentId}")
    public ResponseEntity<Attendance> markAttendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam Attendance.AttendanceStatus status,
            @RequestParam(required = false) String remarks) {

        log.info("Marking attendance for student: {}", studentId);
        Attendance attendance = attendanceService.markAttendance(
                studentId, date, session, term, status, remarks);
        return new ResponseEntity<>(attendance, HttpStatus.CREATED);
    }

    // Mark bulk attendance for multiple students
    // In AttendanceController.java, update the markBulkAttendance method

    @PostMapping("/bulk")
    public ResponseEntity<?> markBulkAttendance(
            @RequestBody List<Long> studentIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam Attendance.AttendanceStatus status) {

        try {
            log.info("Marking bulk attendance for {} students", studentIds.size());
            log.info("Date: {}, Session: {}, Term: {}, Status: {}", date, session, term, status);

            List<Attendance> attendances = attendanceService.markBulkAttendance(
                    studentIds, date, session, term, status);

            return new ResponseEntity<>(attendances, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Error marking bulk attendance: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get attendance for a student on a specific date
    @GetMapping("/student/{studentId}")
    public ResponseEntity<Attendance> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting attendance for student: {} on date: {}", studentId, date);
        Attendance attendance = attendanceService.getStudentAttendance(
                studentId, date, session, term);
        return attendance != null ? ResponseEntity.ok(attendance) : ResponseEntity.notFound().build();
    }

    // Get all attendance records for a student in a term
    @GetMapping("/student/{studentId}/term")
    public ResponseEntity<List<Attendance>> getStudentTermAttendance(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting term attendance for student: {}", studentId);
        List<Attendance> attendances = attendanceService.getStudentTermAttendance(
                studentId, session, term);
        return ResponseEntity.ok(attendances);
    }

    // Get attendance summary for a student in a term
    @GetMapping("/student/{studentId}/summary")
    public ResponseEntity<AttendanceSummary> getStudentTermSummary(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting attendance summary for student: {}", studentId);
        AttendanceSummary summary = attendanceService.getStudentTermSummary(
                studentId, session, term);
        return ResponseEntity.ok(summary);
    }

    // Get attendance summary for a student in a session
    @GetMapping("/student/{studentId}/session")
    public ResponseEntity<Map<String, Object>> getStudentSessionSummary(
            @PathVariable Long studentId,
            @RequestParam String session) {

        log.info("Getting session attendance summary for student: {}", studentId);
        Map<String, Object> summary = attendanceService.getStudentSessionSummary(
                studentId, session);
        return ResponseEntity.ok(summary);
    }

    // Get class attendance for a specific date
    @GetMapping("/class/{className}")
    public ResponseEntity<List<Attendance>> getClassAttendance(
            @PathVariable String className,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting class attendance for: {} on date: {}", className, date);
        List<Attendance> attendances = attendanceService.getClassAttendance(
                className, date, session, term);
        return ResponseEntity.ok(attendances);
    }

    // Get attendance statistics for a class in a term
    @GetMapping("/statistics/class/{className}")
    public ResponseEntity<Map<String, Object>> getClassTermStatistics(
            @PathVariable String className,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting class statistics for: {}", className);
        Map<String, Object> statistics = attendanceService.getClassTermStatistics(
                className, session, term);
        return ResponseEntity.ok(statistics);
    }

    // Get school-wide attendance statistics
    @GetMapping("/statistics/school")
    public ResponseEntity<Map<String, Object>> getSchoolAttendanceStatistics(
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting school attendance statistics");
        Map<String, Object> statistics = attendanceService.getSchoolAttendanceStatistics(
                session, term);
        return ResponseEntity.ok(statistics);
    }

    // Initialize school days for a term
    @PostMapping("/initialize-days")
    public ResponseEntity<List<LocalDate>> initializeSchoolDays(
            @RequestBody List<LocalDate> dates,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Initializing {} school days", dates.size());
        List<LocalDate> initializedDates = attendanceService.initializeSchoolDays(
                dates, session, term);
        return ResponseEntity.ok(initializedDates);
    }

    // Calculate all term summaries
    @PostMapping("/calculate-all")
    public ResponseEntity<String> calculateAllTermSummaries(
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Calculating all term summaries");
        attendanceService.calculateAllTermSummaries(session, term);
        return ResponseEntity.ok("Attendance summaries calculated successfully");
    }

    // Test single attendance (debug endpoint)
    @PostMapping("/test-single")
    public ResponseEntity<String> testSingleAttendance(
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam Attendance.AttendanceStatus status) {

        log.info("Testing single attendance for student: {}", studentId);

        try {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setDate(date);
            attendance.setSession(session);
            attendance.setTerm(term);
            attendance.setStatus(status);

            attendanceRepository.save(attendance);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error in test attendance: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // Debug endpoint
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugAttendance(
            @RequestParam String className,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Student> students = studentRepository.findByStudentClass(className);
            response.put("studentsFound", students.size());
            response.put("studentIds", students.stream().map(Student::getId).collect(Collectors.toList()));

            List<Map<String, Object>> attendanceStatus = new ArrayList<>();
            for (Student student : students) {
                Optional<Attendance> existing = attendanceRepository
                        .findByStudentAndDateAndSessionAndTerm(student, date, session, term);

                Map<String, Object> status = new HashMap<>();
                status.put("studentId", student.getId());
                status.put("studentName", student.getFirstName() + " " + student.getLastName());
                status.put("existingAttendance", existing.isPresent());

                attendanceStatus.add(status);
            }
            response.put("attendanceStatus", attendanceStatus);
            response.put("success", true);

        } catch (Exception e) {
            log.error("Debug error: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}