package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.SessionRequestDTO;
import com.inkFront.schoolManagement.dto.SessionResponseDTO;

import java.util.List;

public interface SessionService {

    SessionResponseDTO createSession(SessionRequestDTO request);

    SessionResponseDTO updateSession(Long id, SessionRequestDTO request);

    void deleteSession(Long id);

    List<SessionResponseDTO> getAllSessions();

    SessionResponseDTO getSessionById(Long id);

    SessionResponseDTO getActiveSession();

    SessionResponseDTO activateSession(Long id);
}