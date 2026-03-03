// src/main/java/com/inkFront/schoolManagement/controllers/SessionResultController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.service.SessionResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session-results")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SessionResultController {

    private final SessionResultService sessionResultService;

    // Calculate session result for a student
    @PostMapping("/calculate/student/{studentId}")
    public ResponseEntity<SessionResult> calculateSessionResult(
            @PathVariable Long studentId,
            @RequestParam String session) {

        SessionResult result = sessionResultService.calculateSessionResult(studentId, session);
        return ResponseEntity.ok(result);
    }

    // Calculate session results for all students
    @PostMapping("/calculate/all")
    public ResponseEntity<List<SessionResult>> calculateAllSessionResults(
            @RequestParam String session) {

        List<SessionResult> results = sessionResultService.calculateAllSessionResults(session);
        return ResponseEntity.ok(results);
    }

    // Get session result for a student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<SessionResult> getSessionResult(
            @PathVariable Long studentId,
            @RequestParam String session) {

        SessionResult result = sessionResultService.getSessionResult(studentId, session);
        return ResponseEntity.ok(result);
    }

    // Get class session results
    @GetMapping("/class/{className}")
    public ResponseEntity<List<SessionResult>> getClassSessionResults(
            @PathVariable String className,
            @RequestParam String session) {

        List<SessionResult> results = sessionResultService.getClassSessionResults(className, session);
        return ResponseEntity.ok(results);
    }

    // Get arm session results
    @GetMapping("/class/{className}/arm/{arm}")
    public ResponseEntity<List<SessionResult>> getArmSessionResults(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session) {

        List<SessionResult> results = sessionResultService.getArmSessionResults(className, arm, session);
        return ResponseEntity.ok(results);
    }

    // Get school rankings
    @GetMapping("/rankings/school")
    public ResponseEntity<Map<String, Object>> getSchoolRankings(
            @RequestParam String session) {

        Map<String, Object> rankings = sessionResultService.getSchoolSessionRankings(session);
        return ResponseEntity.ok(rankings);
    }

    // Get class rankings
    @GetMapping("/rankings/class/{className}")
    public ResponseEntity<Map<String, Object>> getClassRankings(
            @PathVariable String className,
            @RequestParam String session) {

        Map<String, Object> rankings = sessionResultService.getClassRankings(className, session);
        return ResponseEntity.ok(rankings);
    }

    // Get arm rankings
    @GetMapping("/rankings/class/{className}/arm/{arm}")
    public ResponseEntity<Map<String, Object>> getArmRankings(
            @PathVariable String className,
            @PathVariable String arm,
            @RequestParam String session) {

        Map<String, Object> rankings = sessionResultService.getArmRankings(className, arm, session);
        return ResponseEntity.ok(rankings);
    }

    // Generate comprehensive session report
    @GetMapping("/report/{studentId}")
    public ResponseEntity<Map<String, Object>> generateSessionReport(
            @PathVariable Long studentId,
            @RequestParam String session) {

        Map<String, Object> report = sessionResultService.generateSessionReport(studentId, session);
        return ResponseEntity.ok(report);
    }

    // Get session statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSessionStatistics(
            @RequestParam String session) {

        Map<String, Object> stats = sessionResultService.getSessionStatistics(session);
        return ResponseEntity.ok(stats);
    }

    // Promote students based on session results
    @PostMapping("/promote")
    public ResponseEntity<Map<String, Object>> promoteStudents(
            @RequestParam String session) {

        Map<String, Object> result = sessionResultService.promoteStudents(session);
        return ResponseEntity.ok(result);
    }

    // Get graduation list
    @GetMapping("/graduation-list")
    public ResponseEntity<List<Map<String, Object>>> getGraduationList(
            @RequestParam String session) {

        List<Map<String, Object>> graduates = sessionResultService.getGraduationList(session);
        return ResponseEntity.ok(graduates);
    }
}