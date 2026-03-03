// src/main/java/com/inkFront/schoolManagement/service/SessionResultService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Result;

import java.util.List;
import java.util.Map;

public interface SessionResultService {

    // Calculate session result for a single student
    SessionResult calculateSessionResult(Long studentId, String session);

    // Calculate session results for all students
    List<SessionResult> calculateAllSessionResults(String session);

    // Get session result for a student
    SessionResult getSessionResult(Long studentId, String session);

    // Get all session results for a class
    List<SessionResult> getClassSessionResults(String className, String session);

    // Get all session results for an arm
    List<SessionResult> getArmSessionResults(String className, String arm, String session);

    // Get school-wide session results with rankings
    Map<String, Object> getSchoolSessionRankings(String session);

    // Get class rankings for a session
    Map<String, Object> getClassRankings(String className, String session);

    // Get arm rankings for a session
    Map<String, Object> getArmRankings(String className, String arm, String session);

    // Generate comprehensive session report for a student
    Map<String, Object> generateSessionReport(Long studentId, String session);

    // Get session statistics
    Map<String, Object> getSessionStatistics(String session);

    // Promote students based on session results
    Map<String, Object> promoteStudents(String session);

    // Generate graduation list
    List<Map<String, Object>> getGraduationList(String session);
}