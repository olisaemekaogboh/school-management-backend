package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.service.SubjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubjectService subjectService;

    @InjectMocks
    private SubjectController subjectController;

    private ObjectMapper objectMapper;
    private SubjectRequestDTO testRequest;
    private SubjectResponseDTO testResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subjectController).build();
        objectMapper = new ObjectMapper();

        testRequest = new SubjectRequestDTO();
        testRequest.setName("Mathematics");
        testRequest.setCode("MATH101");
        testRequest.setActive(true);

        testResponse = new SubjectResponseDTO();
        testResponse.setId(1L);
        testResponse.setName("Mathematics");
        testResponse.setCode("MATH101");
        testResponse.setActive(true);
    }

    @Test
    void createSubject_ShouldReturnCreatedSubject() throws Exception {
        when(subjectService.createSubject(any(SubjectRequestDTO.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Mathematics"));

        verify(subjectService, times(1)).createSubject(any(SubjectRequestDTO.class));
    }

    @Test
    void updateSubject_ShouldReturnUpdatedSubject() throws Exception {
        when(subjectService.updateSubject(eq(1L), any(SubjectRequestDTO.class))).thenReturn(testResponse);

        mockMvc.perform(put("/api/subjects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(subjectService, times(1)).updateSubject(eq(1L), any(SubjectRequestDTO.class));
    }

    @Test
    void deleteSubject_ShouldReturnNoContent() throws Exception {
        doNothing().when(subjectService).deleteSubject(1L);

        mockMvc.perform(delete("/api/subjects/1"))
                .andExpect(status().isNoContent());

        verify(subjectService, times(1)).deleteSubject(1L);
    }

    @Test
    void getAllSubjects_ShouldReturnList() throws Exception {
        List<SubjectResponseDTO> subjects = Arrays.asList(testResponse);
        when(subjectService.getAllSubjects()).thenReturn(subjects);

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(subjectService, times(1)).getAllSubjects();
    }

    @Test
    void getSubjectById_ShouldReturnSubject() throws Exception {
        when(subjectService.getSubjectById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/subjects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(subjectService, times(1)).getSubjectById(1L);
    }
}