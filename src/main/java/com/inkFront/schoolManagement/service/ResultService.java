// src/main/java/com/inkFront/schoolManagement/service/ResultService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.model.SessionResult;

import java.util.List;
import java.util.Map;

public interface ResultService {

    // NEW: DTO-based method
    Result addOrUpdateResult(ResultRequestDTO request);

    // Original method (keep for backward compatibility)
    Result addOrUpdateResult(Long studentId, String subject, String session,
                             Result.Term term, Map<String, Double> scores);

    List<Result> getStudentResults(Long studentId, String session, Result.Term term);

    TermResult calculateTermResult(Long studentId, String session, Result.Term term);

    SessionResult calculateSessionResult(Long studentId, String session);

    Map<String, Object> getClassRankings(String className, String session, Result.Term term);

    Map<String, Object> getArmRankings(String className, String arm, String session, Result.Term term);

    Map<String, Object> getSchoolRankings(String session, Result.Term term);

    void calculateAllTermResults(String session, Result.Term term);

    void calculateAllSessionResults(String session);

    Map<String, Object> generateResultSheet(Long studentId, String session, Result.Term term);

    Map<String, Object> generateAnnualResultSheet(Long studentId, String session);
}