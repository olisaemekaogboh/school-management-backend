package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.TimetableDTO;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class TimetableController {

    private final TimetableService timetableService;
    private final SecurityUtils securityUtils;
    private final AccessControlService accessControlService;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    private String currentUsernameOrEmail() {
        User user = currentUser();
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return user.getUsername();
    }

    private ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", message));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TimetableDTO dto) {
        try {
            accessControlService.requireAdmin(currentUser());
            return new ResponseEntity<>(timetableService.createEntry(dto), HttpStatus.CREATED);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TimetableDTO dto) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(timetableService.updateEntry(id, dto));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            accessControlService.requireTeacherOrAdmin(currentUser());
            return ResponseEntity.ok(timetableService.getEntry(id));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            accessControlService.requireAdmin(currentUser());
            timetableService.deleteEntry(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<?> classTimetable(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam String term
    ) {
        try {
            accessControlService.requireTeacherOrAdmin(currentUser());
            return ResponseEntity.ok(timetableService.getClassTimetable(classId, session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> teacherTimetable(
            @PathVariable Long teacherId,
            @RequestParam String session,
            @RequestParam String term
    ) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(timetableService.getTeacherTimetable(teacherId, session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/school")
    public ResponseEntity<?> school(
            @RequestParam String session,
            @RequestParam String term
    ) {
        try {
            accessControlService.requireTeacherOrAdmin(currentUser());
            return ResponseEntity.ok(timetableService.getSchoolTimetable(session, term));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> myTeacherTimetable(
            @RequestParam String session,
            @RequestParam String term
    ) {
        try {
            User user = currentUser();
            if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
                throw new AccessDeniedException("Only teacher or admin can access this timetable");
            }

            return ResponseEntity.ok(
                    timetableService.getTeacherOwnTimetable(currentUsernameOrEmail(), session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/student/me")
    public ResponseEntity<?> myStudentTimetable(
            @RequestParam String session,
            @RequestParam String term
    ) {
        try {
            User user = currentUser();
            if (user.getRole() != User.Role.STUDENT && user.getRole() != User.Role.ADMIN) {
                throw new AccessDeniedException("Only student or admin can access this timetable");
            }

            return ResponseEntity.ok(
                    timetableService.getStudentOwnTimetable(currentUsernameOrEmail(), session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/parent/ward/{studentId}")
    public ResponseEntity<?> wardTimetable(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam String term
    ) {
        try {
            User user = currentUser();
            if (user.getRole() != User.Role.PARENT && user.getRole() != User.Role.ADMIN) {
                throw new AccessDeniedException("Only parent or admin can access ward timetable");
            }

            return ResponseEntity.ok(
                    timetableService.getParentWardTimetable(currentUsernameOrEmail(), studentId, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @GetMapping("/check-availability")
    public ResponseEntity<?> check(
            @RequestParam Long teacherId,
            @RequestParam String day,
            @RequestParam String session,
            @RequestParam String term,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(
                    timetableService.checkAvailability(teacherId, day, startTime, endTime, session, term)
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }
}