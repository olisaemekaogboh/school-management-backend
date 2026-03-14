package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;

import java.util.List;
import java.util.Map;

public interface SessionResultService {

    SessionResultResponseDTO calculateSessionResult(Long studentId, String session);

    List<SessionResultResponseDTO> calculateAllSessionResults(String session);



    SessionResultResponseDTO getSessionResult(Long studentId, String session);

    List<SessionResultResponseDTO> getClassSessionResults(String className, String session);

    List<SessionResultResponseDTO> getArmSessionResults(String className, String arm, String session);

    Map<String, Object> getSchoolSessionRankings(String session);

    Map<String, Object> getClassRankings(String className, String session);

    Map<String, Object> getArmRankings(String className, String arm, String session);

    Map<String, Object> generateSessionReport(Long studentId, String session);

    Map<String, Object> getSessionStatistics(String session);

    Map<String, Object> promoteStudents(String session);

    List<Map<String, Object>> getGraduationList(String session);
    List<SessionResultResponseDTO> calculateClassArmSessionResults(String className, String arm, String session);
}