package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.model.*;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AttendanceService;
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

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AttendanceController attendanceController;

    private ObjectMapper objectMapper;
    private User testUser;
    private Student testStudent;
    private SchoolClass testClass;
    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("teacher");
        testUser.setRole(User.Role.TEACHER);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");

        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");
        testClass.setClassCode("G10A");

        testStudent.setSchoolClass(testClass);

        testAttendance = new Attendance();
        testAttendance.setId(1L);
        testAttendance.setStudent(testStudent);
        testAttendance.setDate(LocalDate.now());
        testAttendance.setSession("2023/2024");
        testAttendance.setTerm(Result.Term.FIRST);
        testAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);
    }

    @Test
    void getSchoolDailyStatistics_ShouldReturnStatistics() throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalStudents", 30);
        statistics.put("present", 25);
        statistics.put("absent", 5);

        when(attendanceService.getSchoolAttendanceStatisticsForDate(
                any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(statistics);

        mockMvc.perform(get("/api/attendance/school/daily-statistics")
                        .param("date", "2024-01-15")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(30));

        verify(attendanceService, times(1))
                .getSchoolAttendanceStatisticsForDate(any(LocalDate.class), anyString(), any(Result.Term.class));
    }

    @Test
    void markAttendance_ShouldCreateAttendance() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        doNothing().when(accessControlService).requireAttendanceMarking(any(User.class), anyLong());

        when(attendanceService.markAttendance(
                eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class),
                eq(Attendance.AttendanceStatus.PRESENT), anyString()))
                .thenReturn(testAttendance);

        mockMvc.perform(post("/api/attendance/student/1")
                        .param("date", "2024-01-15")
                        .param("session", "2023/2024")
                        .param("term", "FIRST")
                        .param("status", "PRESENT")
                        .param("remarks", "On time"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(attendanceService, times(1))
                .markAttendance(eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class),
                        eq(Attendance.AttendanceStatus.PRESENT), anyString());
    }

    @Test
    void getStudentAttendance_ShouldReturnAttendance() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        doNothing().when(accessControlService).requireAttendanceAccess(any(User.class), anyLong());

        when(attendanceService.getStudentAttendance(
                eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(testAttendance);

        mockMvc.perform(get("/api/attendance/student/1")
                        .param("date", "2024-01-15")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(attendanceService, times(1))
                .getStudentAttendance(eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class));
    }

    @Test
    void getStudentTermSummary_ShouldReturnSummary() throws Exception {
        AttendanceSummary summary = new AttendanceSummary();
        summary.setStudent(testStudent);
        summary.setSession("2023/2024");
        summary.setTerm(Result.Term.FIRST);
        summary.setTotalSchoolDays(20);
        summary.setDaysPresent(18);
        summary.setAttendancePercentage(90.0);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        doNothing().when(accessControlService).requireAttendanceAccess(any(User.class), anyLong());

        when(attendanceService.getStudentTermSummary(eq(1L), anyString(), any(Result.Term.class)))
                .thenReturn(summary);

        mockMvc.perform(get("/api/attendance/student/1/summary")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1L));

        verify(attendanceService, times(1))
                .getStudentTermSummary(eq(1L), anyString(), any(Result.Term.class));
    }

    @Test
    void getClassAttendance_ShouldReturnClassAttendance() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        doNothing().when(accessControlService).requireClassTeacherOrAdmin(any(User.class), anyLong());

        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceService.getClassAttendance(eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(attendances);

        mockMvc.perform(get("/api/attendance/class/1")
                        .param("date", "2024-01-15")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(1L));

        verify(attendanceService, times(1))
                .getClassAttendance(eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class));
    }
}