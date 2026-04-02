package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.model.*;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.ResultService;
import com.inkFront.schoolManagement.service.SessionResultService;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ResultControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ResultService resultService;

    @Mock
    private SessionResultService sessionResultService;

    @Mock
    private TermResultRepository termResultRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ResultController resultController;

    private ObjectMapper objectMapper;
    private ResultRequestDTO testResultRequest;
    private User testTeacherUser;
    private SchoolClass testClass;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resultController).build();
        objectMapper = new ObjectMapper();

        testResultRequest = new ResultRequestDTO();
        testResultRequest.setStudentId(1L);
        testResultRequest.setSubjectId(1L);
        testResultRequest.setSession("2023/2024");
        testResultRequest.setTerm(Result.Term.FIRST);
        testResultRequest.setResumptionTest(5.0);
        testResultRequest.setAssignments(10.0);
        testResultRequest.setProject(10.0);
        testResultRequest.setMidtermTest(10.0);
        testResultRequest.setSecondTest(5.0);
        testResultRequest.setExamination(45.0);

        testTeacherUser = new User();
        testTeacherUser.setId(1L);
        testTeacherUser.setRole(User.Role.TEACHER);

        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");
    }

    @Test
    void addOrUpdateResult_ShouldReturnCreatedResult() throws Exception {
        Result mockResult = new Result();
        mockResult.setId(1L);

        when(securityUtils.getCurrentUser()).thenReturn(testTeacherUser);
        doNothing().when(accessControlService).requireStudentResultModification(
                any(User.class), anyLong(), anyLong());
        when(resultService.addOrUpdateResult(any(ResultRequestDTO.class)))
                .thenReturn(mockResult);

        mockMvc.perform(post("/api/results/student/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testResultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(resultService, times(1)).addOrUpdateResult(any(ResultRequestDTO.class));
    }

    @Test
    void getStudentResults_ShouldReturnList() throws Exception {
        List<Result> results = Arrays.asList(new Result());
        when(securityUtils.getCurrentUser()).thenReturn(testTeacherUser);
        doNothing().when(accessControlService).requireStudentResultAccess(any(User.class), anyLong());
        when(resultService.getStudentResults(eq(1L), anyString(), any(Result.Term.class)))
                .thenReturn(results);

        mockMvc.perform(get("/api/results/student/1")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk());

        verify(resultService, times(1)).getStudentResults(eq(1L), anyString(), any(Result.Term.class));
    }

    @Test
    void getTermResult_ShouldReturnResultSheet() throws Exception {
        Map<String, Object> resultSheet = new HashMap<>();
        resultSheet.put("studentId", 1L);
        resultSheet.put("average", 85.5);

        when(securityUtils.getCurrentUser()).thenReturn(testTeacherUser);
        doNothing().when(accessControlService).requireStudentResultAccess(any(User.class), anyLong());
        when(resultService.generateResultSheet(eq(1L), anyString(), any(Result.Term.class)))
                .thenReturn(resultSheet);

        mockMvc.perform(get("/api/results/student/1/term")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1L));

        verify(resultService, times(1)).generateResultSheet(eq(1L), anyString(), any(Result.Term.class));
    }

    @Test
    void getClassRankings_ShouldReturnRankings() throws Exception {
        Map<String, Object> rankings = new HashMap<>();
        rankings.put("classId", 1L);

        when(securityUtils.getCurrentUser()).thenReturn(testTeacherUser);
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        doNothing().when(accessControlService).requireClassTeacherOrAdmin(any(User.class), anyLong());
        when(resultService.getClassRankings(eq(1L), anyString(), any(Result.Term.class)))
                .thenReturn(rankings);

        mockMvc.perform(get("/api/results/rankings/class/1")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(1L));

        verify(resultService, times(1)).getClassRankings(eq(1L), anyString(), any(Result.Term.class));
    }

    @Test
    void getSchoolRankings_ShouldReturnRankings() throws Exception {
        Map<String, Object> rankings = new HashMap<>();
        rankings.put("totalStudents", 100);

        when(securityUtils.getCurrentUser()).thenReturn(testTeacherUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(resultService.getSchoolRankings(anyString(), any(Result.Term.class)))
                .thenReturn(rankings);

        mockMvc.perform(get("/api/results/rankings/school")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(100));

        verify(resultService, times(1)).getSchoolRankings(anyString(), any(Result.Term.class));
    }

    @Test
    void calculateAllTermResults_ShouldCalculate() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testTeacherUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        doNothing().when(resultService).calculateAllTermResults(anyString(), any(Result.Term.class));

        mockMvc.perform(post("/api/results/calculate/term")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk());

        verify(resultService, times(1)).calculateAllTermResults(anyString(), any(Result.Term.class));
    }
}