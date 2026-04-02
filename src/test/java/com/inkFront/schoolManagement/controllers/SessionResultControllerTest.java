package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.SessionResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SessionResultControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SessionResultService sessionResultService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SessionResultController sessionResultController;

    private ObjectMapper objectMapper;
    private User testAdminUser;
    private SessionResultResponseDTO testResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sessionResultController).build();
        objectMapper = new ObjectMapper();

        testAdminUser = new User();
        testAdminUser.setId(1L);
        testAdminUser.setRole(User.Role.ADMIN);

        testResponse = new SessionResultResponseDTO();
        testResponse.setStudentId(1L);
        testResponse.setStudentName("John Doe");
        testResponse.setSession("2023/2024");

        // ✅ correct fields
        testResponse.setAnnualAverage(85.5);
    }

    @Test
    void calculateSessionResult_ShouldReturnResult() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(sessionResultService.calculateSessionResult(eq(1L), eq("2023/2024")))
                .thenReturn(testResponse);

        mockMvc.perform(post("/api/session-results/calculate/student/1")
                        .param("session", "2023/2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1L))
                .andExpect(jsonPath("$.annualAverage").value(85.5));

        verify(sessionResultService, times(1))
                .calculateSessionResult(eq(1L), eq("2023/2024"));
    }
}