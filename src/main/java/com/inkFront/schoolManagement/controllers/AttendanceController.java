package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccessControlService accessControlService;
    private final SecurityUtils securityUtils;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    private ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        log.error(message, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", message,
                        "error", e.getMessage()
                ));
    }

    @PostMapping("/student/{studentId}")
    public ResponseEntity<?> markAttendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam Attendance.AttendanceStatus status,
            @RequestParam(required = false) String remarks) {
        try {
            User user = currentUser();
            accessControlService.requireAttendanceMarking(user, studentId);

            Attendance attendance = attendanceService.markAttendance(
                    studentId, date, session, term, status, remarks
            );

            return new ResponseEntity<>(attendance, HttpStatus.CREATED);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to mark attendance", e);
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> markBulkAttendance(
            @RequestBody List<Long> studentIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam Attendance.AttendanceStatus status) {
        try {
            User user = currentUser();
            accessControlService.requireTeacherOrAdmin(user);

            if (studentIds == null || studentIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Student list cannot be empty"));
            }

            for (Long studentId : studentIds) {
                accessControlService.requireAttendanceMarking(user, studentId);
            }

            List<Attendance> attendances = attendanceService.markBulkAttendance(
                    studentIds, date, session, term, status
            );

            return new ResponseEntity<>(attendances, HttpStatus.CREATED);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to mark bulk attendance", e);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAttendanceAccess(user, studentId);

            Attendance attendance = attendanceService.getStudentAttendance(studentId, date, session, term);
            return attendance != null ? ResponseEntity.ok(attendance) : ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch student attendance", e);
        }
    }

    @GetMapping("/student/{studentId}/term")
    public ResponseEntity<?> getStudentTermAttendance(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAttendanceAccess(user, studentId);

            return ResponseEntity.ok(
                    attendanceService.getStudentTermAttendance(studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch student term attendance", e);
        }
    }

    @GetMapping("/student/{studentId}/summary")
    public ResponseEntity<?> getStudentTermSummary(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAttendanceAccess(user, studentId);

            return ResponseEntity.ok(
                    attendanceService.getStudentTermSummary(studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch attendance summary", e);
        }
    }

    @GetMapping("/student/{studentId}/session")
    public ResponseEntity<?> getStudentSessionSummary(
            @PathVariable Long studentId,
            @RequestParam String session) {
        try {
            User user = currentUser();
            accessControlService.requireAttendanceAccess(user, studentId);

            return ResponseEntity.ok(
                    attendanceService.getStudentSessionSummary(studentId, session)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch session attendance summary", e);
        }
    }

    @GetMapping("/me/term")
    public ResponseEntity<?> getMyAttendance(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            return ResponseEntity.ok(
                    attendanceService.getStudentTermAttendance(user.getStudent().getId(), session, term)
            );
        } catch (Exception e) {
            return serverError("Unable to fetch your attendance", e);
        }
    }

    @GetMapping("/me/summary")
    public ResponseEntity<?> getMyAttendanceSummary(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            return ResponseEntity.ok(
                    attendanceService.getStudentTermSummary(user.getStudent().getId(), session, term)
            );
        } catch (Exception e) {
            return serverError("Unable to fetch your attendance summary", e);
        }
    }

    @GetMapping("/class/{className}/arm/{arm}")
    public ResponseEntity<?> getClassAttendance(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireClassTeacherOrAdmin(user, className, arm);

            return ResponseEntity.ok(
                    attendanceService.getClassAttendance(className, arm, date, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class attendance", e);
        }
    }

    @GetMapping("/statistics/class/{className}/arm/{arm}")
    public ResponseEntity<?> getClassTermStatistics(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireClassTeacherOrAdmin(user, className, arm);

            Map<String, Object> statistics = attendanceService.getClassTermStatistics(className, arm, session, term);
            return ResponseEntity.ok(statistics);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class attendance statistics", e);
        }
    }

    @GetMapping("/statistics/school")
    public ResponseEntity<?> getSchoolAttendanceStatistics(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            return ResponseEntity.ok(
                    attendanceService.getSchoolAttendanceStatistics(session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch school attendance statistics", e);
        }
    }

    @PostMapping("/initialize-days")
    public ResponseEntity<?> initializeSchoolDays(
            @RequestBody List<LocalDate> dates,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            return ResponseEntity.ok(
                    attendanceService.initializeSchoolDays(dates, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to initialize school days", e);
        }
    }

    @PostMapping("/calculate-all")
    public ResponseEntity<?> calculateAllTermSummaries(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            attendanceService.calculateAllTermSummaries(session, term);
            return ResponseEntity.ok(Map.of("message", "Attendance summaries calculated successfully"));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate all attendance summaries", e);
        }
    }

    @PostMapping("/test-single")
    public ResponseEntity<?> testSingleAttendance(
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @RequestParam Attendance.AttendanceStatus status) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setDate(date);
            attendance.setSession(session);
            attendance.setTerm(term);
            attendance.setStatus(status);

            attendanceRepository.save(attendance);

            return ResponseEntity.ok(Map.of("message", "Success"));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to save test attendance", e);
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debugAttendance(
            @RequestParam String className,
            @RequestParam(required = false) String arm,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            Map<String, Object> response = new HashMap<>();

            List<Student> students = (arm != null && !arm.isBlank())
                    ? studentRepository.findByStudentClassAndClassArm(className, arm)
                    : studentRepository.findByStudentClass(className);

            response.put("className", className);
            response.put("arm", arm);
            response.put("studentsFound", students.size());
            response.put("studentIds", students.stream().map(Student::getId).toList());

            List<Map<String, Object>> attendanceStatus = new ArrayList<>();
            for (Student student : students) {
                Optional<Attendance> existing =
                        attendanceRepository.findByStudentAndDateAndSessionAndTerm(student, date, session, term);

                Map<String, Object> statusMap = new HashMap<>();
                statusMap.put("studentId", student.getId());
                statusMap.put("studentName", student.getFirstName() + " " + student.getLastName());
                statusMap.put("class", student.getStudentClass());
                statusMap.put("arm", student.getClassArm());
                statusMap.put("existingAttendance", existing.isPresent());
                attendanceStatus.add(statusMap);
            }

            response.put("attendanceStatus", attendanceStatus);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to debug attendance", e);
        }
    }
}