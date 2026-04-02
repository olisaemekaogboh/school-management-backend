package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SessionRequestDTO;
import com.inkFront.schoolManagement.dto.SessionResponseDTO;
import com.inkFront.schoolManagement.model.AcademicSession;
import com.inkFront.schoolManagement.model.Term;
import com.inkFront.schoolManagement.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private AcademicSession testSession;
    private SessionRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        testSession = new AcademicSession();
        testSession.setId(1L);
        testSession.setSessionName("2023/2024");
        testSession.setStartDate(LocalDate.of(2023, 9, 1));
        testSession.setEndDate(LocalDate.of(2024, 7, 31));
        testSession.setCurrentTerm(Term.FIRST);
        testSession.setActive(true);

        testRequestDTO = new SessionRequestDTO();
        testRequestDTO.setSessionName("2023/2024");
        testRequestDTO.setStartDate(LocalDate.of(2023, 9, 1));
        testRequestDTO.setEndDate(LocalDate.of(2024, 7, 31));
        testRequestDTO.setCurrentTerm(Term.FIRST);
        testRequestDTO.setActive(true);
    }

    @Test
    void createSession_ShouldCreateSession() {
        when(sessionRepository.existsBySessionName("2023/2024")).thenReturn(false);
        when(sessionRepository.save(any(AcademicSession.class))).thenReturn(testSession);

        SessionResponseDTO result = sessionService.createSession(testRequestDTO);

        assertNotNull(result);
        assertEquals("2023/2024", result.getSessionName());
        verify(sessionRepository, times(1)).save(any(AcademicSession.class));
    }

    @Test
    void getActiveSession_WhenExists_ShouldReturnSession() {
        when(sessionRepository.findByActiveTrue()).thenReturn(Optional.of(testSession));

        SessionResponseDTO result = sessionService.getActiveSession();

        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    void getActiveSession_WhenNotExists_ShouldReturnNull() {
        when(sessionRepository.findByActiveTrue()).thenReturn(Optional.empty());

        SessionResponseDTO result = sessionService.getActiveSession();

        assertNull(result);
    }
}