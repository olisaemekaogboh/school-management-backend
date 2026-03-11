// src/main/java/com/inkFront/schoolManagement/service/IMPL/SessionServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SessionRequestDTO;
import com.inkFront.schoolManagement.dto.SessionResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.AcademicSession;
import com.inkFront.schoolManagement.repository.SessionRepository;
import com.inkFront.schoolManagement.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    @Override
    public SessionResponseDTO createSession(SessionRequestDTO request) {
        validateRequest(request);

        if (sessionRepository.existsBySessionName(request.getSessionName().trim())) {
            throw new RuntimeException("Session already exists: " + request.getSessionName());
        }

        AcademicSession session = new AcademicSession();
        session.setSessionName(request.getSessionName().trim());
        session.setStartDate(request.getStartDate());
        session.setEndDate(request.getEndDate());

        if (request.isActive()) {
            deactivateCurrentActiveSession();
            session.setActive(true);
        } else {
            session.setActive(false);
        }

        AcademicSession saved = sessionRepository.save(session);
        return SessionResponseDTO.fromEntity(saved);
    }

    @Override
    public SessionResponseDTO updateSession(Long id, SessionRequestDTO request) {
        validateRequest(request);

        AcademicSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        sessionRepository.findBySessionName(request.getSessionName().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Another session already uses this name");
                    }
                });

        session.setSessionName(request.getSessionName().trim());
        session.setStartDate(request.getStartDate());
        session.setEndDate(request.getEndDate());

        if (request.isActive()) {
            deactivateCurrentActiveSession();
            session.setActive(true);
        }

        AcademicSession updated = sessionRepository.save(session);
        return SessionResponseDTO.fromEntity(updated);
    }

    @Override
    public void deleteSession(Long id) {
        AcademicSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        sessionRepository.delete(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponseDTO> getAllSessions() {
        return sessionRepository.findAll()
                .stream()
                .map(SessionResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponseDTO getSessionById(Long id) {
        AcademicSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        return SessionResponseDTO.fromEntity(session);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponseDTO getActiveSession() {
        return sessionRepository.findByActiveTrue()
                .map(SessionResponseDTO::fromEntity)
                .orElse(null);
    }

    @Override
    public SessionResponseDTO activateSession(Long id) {
        AcademicSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        deactivateCurrentActiveSession();
        session.setActive(true);

        AcademicSession saved = sessionRepository.save(session);
        return SessionResponseDTO.fromEntity(saved);
    }

    private void deactivateCurrentActiveSession() {
        sessionRepository.findByActiveTrue().ifPresent(activeSession -> {
            activeSession.setActive(false);
            sessionRepository.save(activeSession);
        });
    }

    private void validateRequest(SessionRequestDTO request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (request.getStartDate().isAfter(request.getEndDate()) ||
                request.getStartDate().isEqual(request.getEndDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        if (request.getSessionName() == null || request.getSessionName().isBlank()) {
            throw new RuntimeException("Session name is required");
        }
    }
}