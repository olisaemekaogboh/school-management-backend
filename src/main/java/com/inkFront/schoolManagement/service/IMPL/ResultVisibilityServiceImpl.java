package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ResultVisibilityUpdateDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.service.ResultVisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ResultVisibilityServiceImpl implements ResultVisibilityService {

    private final StudentRepository studentRepository;
    private final TermResultRepository termResultRepository;
    private final SessionResultRepository sessionResultRepository;

    @Override
    public Map<String, Object> updateTermVisibilityForStudent(
            Long studentId,
            String session,
            Result.Term term,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        TermResult termResult = termResultRepository.findAll().stream()
                .filter(r -> r.getStudent() != null)
                .filter(r -> r.getStudent().getId().equals(studentId))
                .filter(r -> session.equals(r.getSession()))
                .filter(r -> term == r.getTerm())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Term result not found"));

        applyTermVisibility(termResult, request, publishedByName);
        termResultRepository.save(termResult);

        return buildTermResponse(termResult);
    }

    @Override
    public Map<String, Object> updateTermVisibilityForClass(
            Long classId,
            String session,
            Result.Term term,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        Set<Long> studentIds = studentRepository.findAll().stream()
                .filter(s -> s.getSchoolClass() != null)
                .filter(s -> s.getSchoolClass().getId().equals(classId))
                .map(Student::getId)
                .collect(Collectors.toSet());

        List<TermResult> results = termResultRepository.findAll().stream()
                .filter(r -> r.getStudent() != null)
                .filter(r -> studentIds.contains(r.getStudent().getId()))
                .filter(r -> session.equals(r.getSession()))
                .filter(r -> term == r.getTerm())
                .toList();

        results.forEach(r -> applyTermVisibility(r, request, publishedByName));
        termResultRepository.saveAll(results);

        return Map.of(
                "message", "Class term visibility updated successfully",
                "updatedCount", results.size(),
                "classId", classId,
                "session", session,
                "term", term
        );
    }

    @Override
    public Map<String, Object> updateSessionVisibilityForStudent(
            Long studentId,
            String session,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        SessionResult result = sessionResultRepository.findAll().stream()
                .filter(r -> r.getStudent() != null)
                .filter(r -> r.getStudent().getId().equals(studentId))
                .filter(r -> session.equals(r.getSession()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Session result not found"));

        applySessionVisibility(result, request, publishedByName);
        sessionResultRepository.save(result);

        return buildSessionResponse(result);
    }

    @Override
    public Map<String, Object> updateSessionVisibilityForClass(
            Long classId,
            String session,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        Set<Long> studentIds = studentRepository.findAll().stream()
                .filter(s -> s.getSchoolClass() != null)
                .filter(s -> s.getSchoolClass().getId().equals(classId))
                .map(Student::getId)
                .collect(Collectors.toSet());

        List<SessionResult> results = sessionResultRepository.findAll().stream()
                .filter(r -> r.getStudent() != null)
                .filter(r -> studentIds.contains(r.getStudent().getId()))
                .filter(r -> session.equals(r.getSession()))
                .toList();

        results.forEach(r -> applySessionVisibility(r, request, publishedByName));
        sessionResultRepository.saveAll(results);

        return Map.of(
                "message", "Class session visibility updated successfully",
                "updatedCount", results.size(),
                "classId", classId,
                "session", session
        );
    }

    private void applyTermVisibility(
            TermResult result,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        ResultVisibilityStatus status = parseStatus(String.valueOf(request.getVisibilityStatus()));

        switch (status) {
            case HIDDEN -> result.markHidden(request.getVisibilityMessage());
            case STAFF_ONLY -> result.markStaffOnly(request.getVisibilityMessage());
            case PUBLISHED -> result.markPublished(request.getVisibilityMessage(), publishedByName);
            case PRINTABLE -> {
                if (!result.isCompleted()) {
                    throw new IllegalStateException("Result must be fully signed before it can be made printable");
                }
                result.markPrintable(request.getVisibilityMessage(), publishedByName);
            }
        }
    }

    private void applySessionVisibility(
            SessionResult result,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        ResultVisibilityStatus status = parseStatus(String.valueOf(request.getVisibilityStatus()));

        switch (status) {
            case HIDDEN -> result.markHidden(request.getVisibilityMessage());
            case STAFF_ONLY -> result.markStaffOnly(request.getVisibilityMessage());
            case PUBLISHED -> result.markPublished(request.getVisibilityMessage(), publishedByName);
            case PRINTABLE -> result.markPrintable(request.getVisibilityMessage(), publishedByName);
        }
    }

    private ResultVisibilityStatus parseStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("visibilityStatus is required");
        }

        try {
            return ResultVisibilityStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid visibilityStatus. Allowed values: HIDDEN, STAFF_ONLY, PUBLISHED, PRINTABLE"
            );
        }
    }

    private Map<String, Object> buildTermResponse(TermResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", result.getId());
        response.put("studentId", result.getStudent() != null ? result.getStudent().getId() : null);
        response.put("session", result.getSession());
        response.put("term", result.getTerm());
        response.put("visibilityStatus", result.getVisibilityStatus() != null ? result.getVisibilityStatus().name() : null);
        response.put("visibilityMessage", result.getVisibilityMessage());
        response.put("printable", result.isPrintable());
        response.put("printLockMessage", result.getPrintLockMessage());
        response.put("publishedAt", result.getPublishedAt());
        response.put("publishedByName", result.getPublishedByName());
        return response;
    }

    private Map<String, Object> buildSessionResponse(SessionResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", result.getId());
        response.put("studentId", result.getStudent() != null ? result.getStudent().getId() : null);
        response.put("session", result.getSession());
        response.put("visibilityStatus", result.getVisibilityStatus() != null ? result.getVisibilityStatus().name() : null);
        response.put("visibilityMessage", result.getVisibilityMessage());
        response.put("printable", result.isPrintable());
        response.put("printLockMessage", result.getPrintLockMessage());
        response.put("publishedAt", result.getPublishedAt());
        response.put("publishedByName", result.getPublishedByName());
        return response;
    }
}