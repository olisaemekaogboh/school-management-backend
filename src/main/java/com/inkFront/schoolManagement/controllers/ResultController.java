package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.dto.ResultResponseDTO;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.ResultService;
import com.inkFront.schoolManagement.service.SessionResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "https://localhost:3000")
@RequiredArgsConstructor
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);

    private final ResultService resultService;
    private final SessionResultService sessionResultService;
    private final TermResultRepository termResultRepository;
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
    public ResponseEntity<?> addOrUpdateResult(
            @PathVariable Long studentId,
            @Valid @RequestBody ResultRequestDTO resultRequest) {
        try {
            User user = currentUser();

            log.info("POST /api/results/student/{} => userId={}, role={}, requestStudentId={}, subjectId={}, session={}, term={}",
                    studentId,
                    user != null ? user.getId() : null,
                    user != null ? user.getRole() : null,
                    resultRequest.getStudentId(),
                    resultRequest.getSubjectId(),
                    resultRequest.getSession(),
                    resultRequest.getTerm());

            accessControlService.requireStudentResultModification(
                    user,
                    studentId,
                    resultRequest.getSubjectId()
            );

            resultRequest.setStudentId(studentId);

            Result result = resultService.addOrUpdateResult(resultRequest);

            log.info("Result saved successfully => studentId={}, subjectId={}, resultId={}",
                    studentId, resultRequest.getSubjectId(), result.getId());

            return ResponseEntity.ok(ResultResponseDTO.fromResult(result));
        } catch (AccessDeniedException e) {
            log.warn("Result save denied => studentId={}, subjectId={}, reason={}",
                    studentId, resultRequest.getSubjectId(), e.getMessage());
            return forbidden(e.getMessage());
        } catch (Exception e) {
            log.error("Result save failed => studentId={}, subjectId={}", studentId, resultRequest.getSubjectId(), e);
            return serverError("Unable to add or update result", e);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentResults(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireStudentResultAccess(user, studentId);

            List<Result> results = resultService.getStudentResults(studentId, session, term);
            List<ResultResponseDTO> response = results.stream()
                    .map(ResultResponseDTO::fromResult)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch student results", e);
        }
    }

    @GetMapping("/student/{studentId}/term")
    public ResponseEntity<?> getTermResult(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireStudentResultAccess(user, studentId);

            Map<String, Object> resultSheet = resultService.generateResultSheet(studentId, session, term);
            return ResponseEntity.ok(resultSheet);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch term result", e);
        }
    }

    @GetMapping("/student/{studentId}/annual")
    public ResponseEntity<?> getAnnualResult(
            @PathVariable Long studentId,
            @RequestParam String session) {
        try {
            User user = currentUser();
            accessControlService.requireStudentResultAccess(user, studentId);

            // Use session result service for consistent annual totals / averages / attendance
            return ResponseEntity.ok(sessionResultService.calculateSessionResult(studentId, session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch annual result", e);
        }
    }

    @GetMapping("/me/term")
    public ResponseEntity<?> getMyTermResult(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            Long studentId = user.getStudent().getId();
            return ResponseEntity.ok(resultService.generateResultSheet(studentId, session, term));
        } catch (Exception e) {
            return serverError("Unable to fetch your term result", e);
        }
    }

    @GetMapping("/me/annual")
    public ResponseEntity<?> getMyAnnualResult(@RequestParam String session) {
        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            Long studentId = user.getStudent().getId();

            // Use session result service for consistent annual totals / averages / attendance
            return ResponseEntity.ok(sessionResultService.calculateSessionResult(studentId, session));
        } catch (Exception e) {
            return serverError("Unable to fetch your annual result", e);
        }
    }

    @GetMapping("/rankings/class/{className}/arm/{arm}")
    public ResponseEntity<?> getArmRankings(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireClassTeacherOrAdmin(user, className, arm);

            Map<String, Object> rankings = resultService.getArmRankings(className, arm, session, term);
            return ResponseEntity.ok(rankings);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch arm rankings", e);
        }
    }

    @GetMapping("/rankings/class/{className}")
    public ResponseEntity<?> getClassRankings(
            @PathVariable String className,
            @RequestParam(required = false) String arm,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();

            if (arm == null || arm.isBlank()) {
                accessControlService.requireAdmin(user);
            } else {
                accessControlService.requireClassTeacherOrAdmin(user, className, arm);
            }

            Map<String, Object> rankings = resultService.getClassRankings(className, arm, session, term);
            return ResponseEntity.ok(rankings);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class rankings", e);
        }
    }

    @GetMapping("/rankings/school")
    public ResponseEntity<?> getSchoolRankings(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            Map<String, Object> rankings = resultService.getSchoolRankings(session, term);
            return ResponseEntity.ok(rankings);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch school rankings", e);
        }
    }

    @PostMapping("/calculate/term")
    public ResponseEntity<?> calculateAllTermResults(
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            resultService.calculateAllTermResults(session, term);
            return ResponseEntity.ok(Map.of("message", "All term results calculated successfully"));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate all term results", e);
        }
    }

    @PostMapping("/calculate/annual")
    public ResponseEntity<?> calculateAllSessionResults(@RequestParam String session) {
        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            return ResponseEntity.ok(sessionResultService.calculateAllSessionResults(session));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to calculate annual results", e);
        }
    }

    @GetMapping("/statistics/class/{className}/arm/{arm}")
    public ResponseEntity<?> getClassStatistics(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session,
            @RequestParam Result.Term term) {
        try {
            User user = currentUser();
            accessControlService.requireClassTeacherOrAdmin(user, className, arm);

            List<TermResult> classResults = termResultRepository
                    .findByStudent_SchoolClass_ClassNameAndStudent_SchoolClass_ArmAndSessionAndTermOrderByAverageDesc(
                            className, arm, session, term
                    );

            if (classResults.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No results found for this class arm"));
            }

            double classAverage = classResults.stream()
                    .mapToDouble(TermResult::getAverage)
                    .average()
                    .orElse(0);

            double highestScore = classResults.stream()
                    .mapToDouble(TermResult::getAverage)
                    .max()
                    .orElse(0);

            double lowestScore = classResults.stream()
                    .mapToDouble(TermResult::getAverage)
                    .min()
                    .orElse(0);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", classResults.size());
            stats.put("classAverage", classAverage);
            stats.put("highestScore", highestScore);
            stats.put("lowestScore", lowestScore);
            stats.put("className", className);
            stats.put("arm", arm);
            stats.put("session", session);
            stats.put("term", term);

            return ResponseEntity.ok(stats);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to fetch class statistics", e);
        }
    }
}