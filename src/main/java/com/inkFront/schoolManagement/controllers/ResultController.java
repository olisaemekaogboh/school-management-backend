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
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);

    private final ResultService resultService;
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
            accessControlService.requireStudentResultModification(user, studentId);

            Result result = resultService.addOrUpdateResult(
                    studentId,
                    resultRequest.getSubject(),
                    resultRequest.getSession(),
                    resultRequest.getTerm(),
                    Map.of(
                            "resumptionTest", resultRequest.getResumptionTest(),
                            "assignments", resultRequest.getAssignments(),
                            "project", resultRequest.getProject(),
                            "midtermTest", resultRequest.getMidtermTest(),
                            "secondTest", resultRequest.getSecondTest(),
                            "examination", resultRequest.getExamination()
                    )
            );

            return ResponseEntity.ok(ResultResponseDTO.fromResult(result));

        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
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
            return ResponseEntity.ok(results);

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

            Map<String, Object> result = resultService.generateAnnualResultSheet(studentId, session);
            return ResponseEntity.ok(result);

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
    public ResponseEntity<?> getMyAnnualResult(
            @RequestParam String session) {

        try {
            User user = currentUser();

            if (user.getStudent() == null) {
                return forbidden("This account is not linked to a student");
            }

            Long studentId = user.getStudent().getId();
            return ResponseEntity.ok(resultService.generateAnnualResultSheet(studentId, session));

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
            @RequestParam String arm,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        try {
            User user = currentUser();
            accessControlService.requireClassTeacherOrAdmin(user, className, arm);

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
    public ResponseEntity<?> calculateAllSessionResults(
            @RequestParam String session) {

        try {
            User user = currentUser();
            accessControlService.requireAdmin(user);

            resultService.calculateAllSessionResults(session);
            return ResponseEntity.ok(Map.of("message", "All annual results calculated successfully"));

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
                    .findByStudent_StudentClassAndStudent_ClassArmAndSessionAndTermOrderByAverageDesc(
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