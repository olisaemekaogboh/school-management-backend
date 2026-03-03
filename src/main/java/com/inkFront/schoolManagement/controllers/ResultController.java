// src/main/java/com/inkFront/schoolManagement/controllers/ResultController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.dto.ResultResponseDTO;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.service.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);  // ADD THIS LINE

    private final ResultService resultService;
    private final TermResultRepository termResultRepository;

    @PostMapping("/student/{studentId}")
    public ResponseEntity<ResultResponseDTO> addOrUpdateResult(
            @PathVariable Long studentId,
            @Valid @RequestBody ResultRequestDTO resultRequest) {

        log.info("Adding/updating result for student: {}, subject: {}", studentId, resultRequest.getSubject());

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
    }

    // Get all results for a student in a term
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Result>> getStudentResults(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting results for student: {}, session: {}, term: {}", studentId, session, term);

        List<Result> results = resultService.getStudentResults(studentId, session, term);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/student/{studentId}/term")
    public ResponseEntity<?> getTermResult(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        try {
            log.info("Getting term result for student: {}, session: {}, term: {}", studentId, session, term);

            Map<String, Object> resultSheet = resultService.generateResultSheet(studentId, session, term);
            return ResponseEntity.ok(resultSheet);

        } catch (Exception e) {
            log.error("Error getting term result: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "No result found for this student in the specified term");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Calculate and get annual result for a student
    @GetMapping("/student/{studentId}/annual")
    public ResponseEntity<Map<String, Object>> getAnnualResult(
            @PathVariable Long studentId,
            @RequestParam String session) {

        log.info("Getting annual result for student: {}, session: {}", studentId, session);

        Map<String, Object> resultSheet = resultService.generateAnnualResultSheet(studentId, session);
        return ResponseEntity.ok(resultSheet);
    }

    // Get class rankings
    @GetMapping("/rankings/class/{className}")
    public ResponseEntity<Map<String, Object>> getClassRankings(
            @PathVariable String className,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting class rankings for: {}, session: {}, term: {}", className, session, term);

        Map<String, Object> rankings = resultService.getClassRankings(className, session, term);
        return ResponseEntity.ok(rankings);
    }

    // Get arm rankings
    @GetMapping("/rankings/class/{className}/arm/{arm}")
    public ResponseEntity<Map<String, Object>> getArmRankings(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting arm rankings for: {} {}, session: {}, term: {}", className, arm, session, term);

        Map<String, Object> rankings = resultService.getArmRankings(className, arm, session, term);
        return ResponseEntity.ok(rankings);
    }

    // Get school rankings
    @GetMapping("/rankings/school")
    public ResponseEntity<Map<String, Object>> getSchoolRankings(
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting school rankings for session: {}, term: {}", session, term);

        Map<String, Object> rankings = resultService.getSchoolRankings(session, term);
        return ResponseEntity.ok(rankings);
    }

    // Calculate all term results (bulk operation)
    @PostMapping("/calculate/term")
    public ResponseEntity<String> calculateAllTermResults(
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Calculating all term results for session: {}, term: {}", session, term);

        resultService.calculateAllTermResults(session, term);
        return ResponseEntity.ok("All term results calculated successfully");
    }

    // Calculate all session results (bulk operation)
    @PostMapping("/calculate/annual")
    public ResponseEntity<String> calculateAllSessionResults(
            @RequestParam String session) {

        log.info("Calculating all annual results for session: {}", session);

        resultService.calculateAllSessionResults(session);
        return ResponseEntity.ok("All annual results calculated successfully");
    }

    @GetMapping("/statistics/class/{className}")
    public ResponseEntity<Map<String, Object>> getClassStatistics(
            @PathVariable String className,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        log.info("Getting class statistics for: {}, session: {}, term: {}", className, session, term);

        List<TermResult> classResults = termResultRepository
                .findByClassAndSessionAndTermOrderByAverageDesc(className, session, term);

        if (classResults.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No results found for this class"));
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
        stats.put("session", session);
        stats.put("term", term);

        return ResponseEntity.ok(stats);
    }
}