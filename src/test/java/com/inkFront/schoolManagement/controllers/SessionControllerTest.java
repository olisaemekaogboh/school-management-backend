package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkFront.schoolManagement.dto.SessionRequestDTO;
import com.inkFront.schoolManagement.dto.SessionResponseDTO;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import com.inkFront.schoolManagement.model.Term;
import com.inkFront.schoolManagement.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController sessionController;

    private ObjectMapper objectMapper;
    private SessionRequestDTO testRequest;
    private SessionResponseDTO testResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testRequest = new SessionRequestDTO();
        testRequest.setSessionName("2023/2024");
        testRequest.setStartDate(LocalDate.of(2023, 9, 1));
        testRequest.setEndDate(LocalDate.of(2024, 7, 31));
        testRequest.setCurrentTerm(Term.FIRST);
        testRequest.setActive(true);

        testResponse = new SessionResponseDTO();
        testResponse.setId(1L);
        testResponse.setSessionName("2023/2024");
        testResponse.setStartDate(LocalDate.of(2023, 9, 1));
        testResponse.setEndDate(LocalDate.of(2024, 7, 31));
        testResponse.setCurrentTerm(Term.FIRST);
        testResponse.setActive(true);
    }

    @Test
    void createSession_ShouldReturnCreatedSession() throws Exception {
        when(sessionService.createSession(any(SessionRequestDTO.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.session").value("2023/2024"));

        verify(sessionService, times(1)).createSession(any(SessionRequestDTO.class));
    }

    @Test
    void updateSession_ShouldReturnUpdatedSession() throws Exception {
        when(sessionService.updateSession(eq(1L), any(SessionRequestDTO.class))).thenReturn(testResponse);

        mockMvc.perform(put("/api/sessions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(sessionService, times(1)).updateSession(eq(1L), any(SessionRequestDTO.class));
    }

    @Test
    void deleteSession_ShouldReturnNoContent() throws Exception {
        doNothing().when(sessionService).deleteSession(1L);

        mockMvc.perform(delete("/api/sessions/1"))
                .andExpect(status().isNoContent());

        verify(sessionService, times(1)).deleteSession(1L);
    }

    @Test
    void getAllSessions_ShouldReturnList() throws Exception {
        List<SessionResponseDTO> sessions = Arrays.asList(testResponse);
        when(sessionService.getAllSessions()).thenReturn(sessions);

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(sessionService, times(1)).getAllSessions();
    }

    @Test
    void getSessionById_ShouldReturnSession() throws Exception {
        when(sessionService.getSessionById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(sessionService, times(1)).getSessionById(1L);
    }

    @Test
    void getActiveSession_WhenExists_ShouldReturnSession() throws Exception {
        when(sessionService.getActiveSession()).thenReturn(testResponse);

        mockMvc.perform(get("/api/sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(sessionService, times(1)).getActiveSession();
    }

    @Test
    void getActiveSession_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(sessionService.getActiveSession()).thenReturn(null);

        mockMvc.perform(get("/api/sessions/active"))
                .andExpect(status().isNotFound());

        verify(sessionService, times(1)).getActiveSession();
    }

    @Test
    void activateSession_ShouldReturnActivatedSession() throws Exception {
        when(sessionService.activateSession(1L)).thenReturn(testResponse);

        mockMvc.perform(put("/api/sessions/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(sessionService, times(1)).activateSession(1L);
    }

    @Test
    void createSession_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        SessionRequestDTO invalidRequest = new SessionRequestDTO();

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).createSession(any());
    }
}