package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.TimetableDTO;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.TimetableService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
class TimetableControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TimetableService timetableService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private TimetableController timetableController;

    private ObjectMapper objectMapper;
    private TimetableDTO testDTO;
    private User testAdminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(timetableController).build();
        objectMapper = new ObjectMapper();

        testAdminUser = new User();
        testAdminUser.setId(1L);
        testAdminUser.setRole(User.Role.ADMIN);
        testAdminUser.setEmail("admin@school.com");

        testDTO = new TimetableDTO();
        testDTO.setId(1L);
        testDTO.setSchoolClassId(1L);
        testDTO.setTeacherId(1L);
        testDTO.setSubject("Mathematics");
        testDTO.setDayOfWeek("MONDAY");
        testDTO.setStartTime("08:00");
        testDTO.setEndTime("09:00");
        testDTO.setRoom("Room 101");
        testDTO.setSession("2023/2024");
        testDTO.setTerm("FIRST");
        testDTO.setActive(true);
    }

    @Test
    void createTimetableEntry_ShouldReturnCreatedEntry() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(timetableService.createEntry(any(TimetableDTO.class))).thenReturn(testDTO);

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.subject").value("Mathematics"));

        verify(timetableService, times(1)).createEntry(any(TimetableDTO.class));
    }

    @Test
    void updateTimetableEntry_ShouldReturnUpdatedEntry() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(timetableService.updateEntry(eq(1L), any(TimetableDTO.class))).thenReturn(testDTO);

        mockMvc.perform(put("/api/timetable/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(timetableService, times(1)).updateEntry(eq(1L), any(TimetableDTO.class));
    }

    @Test
    void getTimetableEntry_ShouldReturnEntry() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireTeacherOrAdmin(any(User.class));
        when(timetableService.getEntry(1L)).thenReturn(testDTO);

        mockMvc.perform(get("/api/timetable/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(timetableService, times(1)).getEntry(1L);
    }

    @Test
    void deleteTimetableEntry_ShouldReturnNoContent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        doNothing().when(timetableService).deleteEntry(1L);

        mockMvc.perform(delete("/api/timetable/1"))
                .andExpect(status().isNoContent());

        verify(timetableService, times(1)).deleteEntry(1L);
    }

    @Test
    void getClassTimetable_ShouldReturnTimetable() throws Exception {
        List<TimetableDTO> timetable = Arrays.asList(testDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireTeacherOrAdmin(any(User.class));
        when(timetableService.getClassTimetable(eq(1L), eq("2023/2024"), eq("FIRST")))
                .thenReturn(timetable);

        mockMvc.perform(get("/api/timetable/class/1")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(timetableService, times(1))
                .getClassTimetable(eq(1L), eq("2023/2024"), eq("FIRST"));
    }

    @Test
    void getTeacherTimetable_ShouldReturnTimetable() throws Exception {
        List<TimetableDTO> timetable = Arrays.asList(testDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(timetableService.getTeacherTimetable(eq(1L), eq("2023/2024"), eq("FIRST")))
                .thenReturn(timetable);

        mockMvc.perform(get("/api/timetable/teacher/1")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(timetableService, times(1))
                .getTeacherTimetable(eq(1L), eq("2023/2024"), eq("FIRST"));
    }

    @Test
    void getSchoolTimetable_ShouldReturnTimetable() throws Exception {
        List<TimetableDTO> timetable = Arrays.asList(testDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireTeacherOrAdmin(any(User.class));
        when(timetableService.getSchoolTimetable("2023/2024", "FIRST")).thenReturn(timetable);

        mockMvc.perform(get("/api/timetable/school")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(timetableService, times(1)).getSchoolTimetable("2023/2024", "FIRST");
    }

    @Test
    void getMyTeacherTimetable_ShouldReturnTimetable() throws Exception {
        User teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setRole(User.Role.TEACHER);
        teacherUser.setEmail("teacher@school.com");

        List<TimetableDTO> timetable = Arrays.asList(testDTO);
        when(securityUtils.getCurrentUser()).thenReturn(teacherUser);
        when(timetableService.getTeacherOwnTimetable(eq("teacher@school.com"), eq("2023/2024"), eq("FIRST")))
                .thenReturn(timetable);

        mockMvc.perform(get("/api/timetable/me")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(timetableService, times(1))
                .getTeacherOwnTimetable(eq("teacher@school.com"), eq("2023/2024"), eq("FIRST"));
    }

    @Test
    void getMyStudentTimetable_ShouldReturnTimetable() throws Exception {
        User studentUser = new User();
        studentUser.setId(3L);
        studentUser.setRole(User.Role.STUDENT);
        studentUser.setUsername("student");

        List<TimetableDTO> timetable = Arrays.asList(testDTO);
        when(securityUtils.getCurrentUser()).thenReturn(studentUser);
        when(timetableService.getStudentOwnTimetable(eq("student"), eq("2023/2024"), eq("FIRST")))
                .thenReturn(timetable);

        mockMvc.perform(get("/api/timetable/student/me")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(timetableService, times(1))
                .getStudentOwnTimetable(eq("student"), eq("2023/2024"), eq("FIRST"));
    }

    @Test
    void getWardTimetable_ShouldReturnTimetable() throws Exception {
        User parentUser = new User();
        parentUser.setId(4L);
        parentUser.setRole(User.Role.PARENT);
        parentUser.setEmail("parent@example.com");

        List<TimetableDTO> timetable = Arrays.asList(testDTO);
        when(securityUtils.getCurrentUser()).thenReturn(parentUser);
        when(timetableService.getParentWardTimetable(eq("parent@example.com"), eq(1L), eq("2023/2024"), eq("FIRST")))
                .thenReturn(timetable);

        mockMvc.perform(get("/api/timetable/parent/ward/1")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(timetableService, times(1))
                .getParentWardTimetable(eq("parent@example.com"), eq(1L), eq("2023/2024"), eq("FIRST"));
    }

    @Test
    void checkAvailability_ShouldReturnAvailability() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(accessControlService).requireAdmin(any(User.class));
        when(timetableService.checkAvailability(
                eq(1L), eq("MONDAY"), eq("08:00"), eq("09:00"), eq("2023/2024"), eq("FIRST")
        )).thenReturn(true);

        mockMvc.perform(get("/api/timetable/check-availability")
                        .param("teacherId", "1")
                        .param("day", "MONDAY")
                        .param("session", "2023/2024")
                        .param("term", "FIRST")
                        .param("startTime", "08:00")
                        .param("endTime", "09:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        verify(timetableService, times(1))
                .checkAvailability(eq(1L), eq("MONDAY"), eq("08:00"), eq("09:00"), eq("2023/2024"), eq("FIRST"));
    }
}