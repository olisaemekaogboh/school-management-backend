package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.SessionResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session-results")
@CrossOrigin(origins = "https://localhost:3000")
@RequiredArgsConstructor
public class SessionResultController {

    private final SessionResultService sessionResultService;
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", message,
                        "error", e.getMessage()
                ));
    }

    @PostMapping("/calculate/student/{studentId}")
    public ResponseEntity<?> calculateSessionResult(
            @PathVariable Long studentId,
            @RequestParam String session) {
        try {
            accessControlService.requireAdmin(currentUser());
            SessionResultResponseDTO result = sessionResultService.calculateSessionResult(studentId, session);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate session result", e);
        }
    }

    @PostMapping("/calculate/all")
    public ResponseEntity<?> calculateAllSessionResults(@RequestParam String session) {
        try {
            accessControlService.requireAdmin(currentUser());
            List<SessionResultResponseDTO> results = sessionResultService.calculateAllSessionResults(session);
            return ResponseEntity.ok(results);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate all session results", e);
        }
    }

    @PostMapping("/calculate/class/{className}/arm/{arm}")
    public ResponseEntity<?> calculateClassArmSessionResults(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session) {
        try {
            accessControlService.requireResultClassAccess(currentUser(), className, arm);

            List<SessionResultResponseDTO> results =
                    sessionResultService.calculateClassArmSessionResults(className, arm, session);

            return ResponseEntity.ok(results);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate class arm session results", e);
        }
    }

    @GetMapping("/rankings/school")
    public ResponseEntity<?> getSchoolRankings(@RequestParam String session) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(sessionResultService.getSchoolSessionRankings(session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch school rankings", e);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getSessionStatistics(@RequestParam String session) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(sessionResultService.getSessionStatistics(session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch session statistics", e);
        }
    }

    @PostMapping("/promote")
    public ResponseEntity<?> promoteStudents(@RequestParam String session) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(sessionResultService.promoteStudents(session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to promote students", e);
        }
    }

    @GetMapping("/graduation-list")
    public ResponseEntity<?> getGraduationList(@RequestParam String session) {
        try {
            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(sessionResultService.getGraduationList(session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch graduation list", e);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getSessionResult(
            @PathVariable Long studentId,
            @RequestParam String session) {
        try {
            accessControlService.requireStudentResultAccess(currentUser(), studentId);
            return ResponseEntity.ok(sessionResultService.getSessionResult(studentId, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch session result", e);
        }
    }

    @GetMapping("/report/{studentId}")
    public ResponseEntity<?> generateSessionReport(
            @PathVariable Long studentId,
            @RequestParam String session) {
        try {
            accessControlService.requireStudentResultAccess(currentUser(), studentId);
            return ResponseEntity.ok(sessionResultService.generateSessionReport(studentId, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to generate session report", e);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMySessionResult(@RequestParam String session) {
        try {
            User user = currentUser();
            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }
            return ResponseEntity.ok(sessionResultService.getSessionResult(user.getStudent().getId(), session));
        } catch (Exception e) {
            return serverError("Unable to fetch your session result", e);
        }
    }

    @GetMapping("/class/{className}")
    public ResponseEntity<?> getClassSessionResults(
            @PathVariable String className,
            @RequestParam String session,
            @RequestParam(required = false) String arm) {
        try {
            if (arm != null && !arm.isBlank()) {
                accessControlService.requireResultClassAccess(currentUser(), className, arm);
                return ResponseEntity.ok(sessionResultService.getArmSessionResults(className, arm, session));
            }

            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(sessionResultService.getClassSessionResults(className, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class session results", e);
        }
    }

    @GetMapping("/class/{className}/arm/{arm}")
    public ResponseEntity<?> getArmSessionResults(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session) {
        try {
            accessControlService.requireResultClassAccess(currentUser(), className, arm);
            return ResponseEntity.ok(sessionResultService.getArmSessionResults(className, arm, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch arm session results", e);
        }
    }

    @GetMapping("/rankings/class/{className}")
    public ResponseEntity<?> getClassRankings(
            @PathVariable String className,
            @RequestParam String session,
            @RequestParam(required = false) String arm) {
        try {
            if (arm != null && !arm.isBlank()) {
                accessControlService.requireResultClassAccess(currentUser(), className, arm);
                return ResponseEntity.ok(sessionResultService.getArmRankings(className, arm, session));
            }

            accessControlService.requireAdmin(currentUser());
            return ResponseEntity.ok(sessionResultService.getClassRankings(className, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class rankings", e);
        }
    }

    @GetMapping("/rankings/class/{className}/arm/{arm}")
    public ResponseEntity<?> getArmRankings(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session) {
        try {
            accessControlService.requireResultClassAccess(currentUser(), className, arm);
            return ResponseEntity.ok(sessionResultService.getArmRankings(className, arm, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch arm rankings", e);
        }
    }
}