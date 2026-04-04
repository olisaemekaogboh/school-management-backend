package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.dto.ResultVisibilityUpdateDTO;
import com.inkFront.schoolManagement.dto.TermAssessmentUpdateDTO;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.TermResult;

import java.util.List;
import java.util.Map;

public interface ResultService {

    Result addOrUpdateResult(ResultRequestDTO request);

    Result addOrUpdateResult(
            Long studentId,
            String subject,
            String session,
            Result.Term term,
            Map<String, Double> scores
    );

    List<Result> getStudentResults(Long studentId, String session, Result.Term term);

    TermResult setTermResultPrintableStatus(
            Long studentId,
            String session,
            Result.Term term,
            boolean printable,
            String printLockMessage
    );

    TermResult updateTermVisibility(
            Long studentId,
            String session,
            Result.Term term,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    );

    TermResult calculateTermResult(Long studentId, String session, Result.Term term);

    SessionResult calculateSessionResult(Long studentId, String session);

    Map<String, Object> getClassRankings(Long classId, String session, Result.Term term);

    Map<String, Object> getSchoolRankings(String session, Result.Term term);

    void calculateAllTermResults(String session, Result.Term term);

    void calculateAllSessionResults(String session);

    Map<String, Object> generateResultSheet(Long studentId, String session, Result.Term term);

    Map<String, Object> generateAnnualResultSheet(Long studentId, String session);

    TermResult updateTermAssessment(
            Long studentId,
            String session,
            Result.Term term,
            TermAssessmentUpdateDTO request
    );

    TermResult signByClassTeacher(
            Long studentId,
            String session,
            Result.Term term,
            String signatureUrl
    );

    TermResult signByAdmin(
            Long studentId,
            String session,
            Result.Term term,
            String signatureUrl
    );
}