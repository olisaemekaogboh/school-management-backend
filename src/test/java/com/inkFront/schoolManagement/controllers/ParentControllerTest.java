package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Fee;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AttendanceService;
import com.inkFront.schoolManagement.service.FeeService;
import com.inkFront.schoolManagement.service.ParentService;
import com.inkFront.schoolManagement.service.ResultService;
import com.inkFront.schoolManagement.service.SessionResultService;
import com.inkFront.schoolManagement.service.StudentService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ParentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ParentService parentService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ResultService resultService;

    @Mock
    private SessionResultService sessionResultService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private FeeService feeService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private ParentController parentController;

    private ObjectMapper objectMapper;
    private ParentDTO testParentDTO;
    private User testAdminUser;
    private User testParentUser;
    private Student testStudent;
    private Parent testParent;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(parentController).build();
        objectMapper = new ObjectMapper();

        testParent = new Parent();
        testParent.setId(1L);
        testParent.setFirstName("John");
        testParent.setLastName("Doe");
        testParent.setEmail("john.doe@example.com");
        testParent.setPhoneNumber("+1234567890");

        testParentDTO = new ParentDTO();
        testParentDTO.setId(1L);
        testParentDTO.setFirstName("John");
        testParentDTO.setLastName("Doe");
        testParentDTO.setEmail("john.doe@example.com");
        testParentDTO.setPhoneNumber("+1234567890");
        testParentDTO.setRelationship(Parent.Relationship.FATHER);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");
        testStudent.setParent(testParent);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(10L);
        schoolClass.setClassName("Grade 10");
        schoolClass.setArm("A");
        testStudent.setSchoolClass(schoolClass);
        testStudent.setStatus(Student.StudentStatus.ACTIVE);

        testAdminUser = new User();
        testAdminUser.setId(100L);
        testAdminUser.setRole(User.Role.ADMIN);

        testParentUser = new User();
        testParentUser.setId(200L);
        testParentUser.setRole(User.Role.PARENT);
        testParentUser.setParent(testParent);
    }

    @Test
    void createParent_ShouldReturnCreatedParent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.createParent(any(ParentDTO.class))).thenReturn(testParentDTO);

        mockMvc.perform(post("/api/parents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testParentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(parentService, times(1)).createParent(any(ParentDTO.class));
    }

    @Test
    void getAllParents_ShouldReturnList() throws Exception {
        List<ParentDTO> parents = Arrays.asList(testParentDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.getAllParents()).thenReturn(parents);

        mockMvc.perform(get("/api/parents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(parentService, times(1)).getAllParents();
    }

    @Test
    void getParentsPaginated_ShouldReturnPage() {
        Page<ParentDTO> parentPage = new PageImpl<>(
                Arrays.asList(testParentDTO),
                PageRequest.of(0, 10),
                1
        );

        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.getAllParentsPaginated(any())).thenReturn(parentPage);

        ResponseEntity<Page<ParentDTO>> response =
                parentController.getParentsPaginated(0, 10, "id", "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());

        verify(parentService, times(1)).getAllParentsPaginated(any());
    }

    @Test
    void getParentById_AsAdmin_ShouldReturnParent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.getParentById(1L)).thenReturn(Optional.of(testParentDTO));

        mockMvc.perform(get("/api/parents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).getParentById(1L);
    }

    @Test
    void getParentById_AsParent_ShouldReturnOwnRecord() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(parentService.getParentById(1L)).thenReturn(Optional.of(testParentDTO));

        mockMvc.perform(get("/api/parents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).getParentById(1L);
    }

    @Test
    void getParentById_AsParent_AccessingOtherParent_ShouldReturnForbidden() {
        User otherParentUser = new User();
        otherParentUser.setId(3L);
        otherParentUser.setRole(User.Role.PARENT);
        Parent otherParent = new Parent();
        otherParent.setId(2L);
        otherParentUser.setParent(otherParent);

        when(securityUtils.getCurrentUser()).thenReturn(otherParentUser);

        ServletException ex = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/api/parents/1")).andReturn()
        );

        assertInstanceOf(AccessDeniedException.class, ex.getCause());
        verify(parentService, never()).getParentById(anyLong());
    }

    @Test
    void getMyProfile_ShouldReturnParentProfile() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);

        mockMvc.perform(get("/api/parents/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(parentService, never()).getParentById(anyLong());
    }

    @Test
    void getMyProfile_WhenNoParentLinked_ShouldReturnForbidden() throws Exception {
        User userWithoutParent = new User();
        userWithoutParent.setId(3L);
        userWithoutParent.setRole(User.Role.PARENT);
        userWithoutParent.setParent(null);

        when(securityUtils.getCurrentUser()).thenReturn(userWithoutParent);

        mockMvc.perform(get("/api/parents/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyWards_ShouldReturnWards() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(testStudent));

        mockMvc.perform(get("/api/parents/me/wards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].firstName").value("Jane"))
                .andExpect(jsonPath("$[0].studentClass").value("Grade 10"))
                .andExpect(jsonPath("$[0].classArm").value("A"));

        verify(studentRepository, times(1)).findAll();
        verify(studentService, times(1)).getStudentById(1L);
    }

    @Test
    void getParentByEmail_ShouldReturnParent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.getParentByEmail("john.doe@example.com")).thenReturn(Optional.of(testParentDTO));

        mockMvc.perform(get("/api/parents/by-email")
                        .param("email", "john.doe@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).getParentByEmail("john.doe@example.com");
    }

    @Test
    void verifyParentEmail_ShouldReturnSuccess() throws Exception {
        when(parentService.verifyParentEmail("john.doe@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/parents/verify")
                        .param("email", "john.doe@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Parent found with email: john.doe@example.com"));

        verify(parentService, times(1)).verifyParentEmail("john.doe@example.com");
    }

    @Test
    void verifyParentEmail_WhenNotFound_ShouldReturnNotFound() throws Exception {
        when(parentService.verifyParentEmail("unknown@example.com")).thenReturn(false);

        mockMvc.perform(get("/api/parents/verify")
                        .param("email", "unknown@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(parentService, times(1)).verifyParentEmail("unknown@example.com");
    }

    @Test
    void updateParent_AsAdmin_ShouldUpdate() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.updateParent(eq(1L), any(ParentDTO.class))).thenReturn(testParentDTO);

        mockMvc.perform(put("/api/parents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testParentDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).updateParent(eq(1L), any(ParentDTO.class));
    }

    @Test
    void updateParent_AsParent_ShouldUpdateOwnRecord() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(parentService.updateParent(eq(1L), any(ParentDTO.class))).thenReturn(testParentDTO);

        mockMvc.perform(put("/api/parents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testParentDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).updateParent(eq(1L), any(ParentDTO.class));
    }

    @Test
    void deleteParent_ShouldReturnSuccess() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        doNothing().when(parentService).deleteParent(1L);

        mockMvc.perform(delete("/api/parents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Parent deleted successfully"));

        verify(parentService, times(1)).deleteParent(1L);
    }

    @Test
    void searchParents_ShouldReturnResults() throws Exception {
        List<ParentDTO> parents = Arrays.asList(testParentDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.searchParents("John")).thenReturn(parents);

        mockMvc.perform(get("/api/parents/search")
                        .param("q", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(parentService, times(1)).searchParents("John");
    }

    @Test
    void addWardToParent_ShouldAddWard() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.addWardToParent(1L, 1L)).thenReturn(testParentDTO);

        mockMvc.perform(post("/api/parents/1/wards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).addWardToParent(1L, 1L);
    }

    @Test
    void removeWardFromParent_ShouldRemoveWard() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.removeWardFromParent(1L, 1L)).thenReturn(testParentDTO);

        mockMvc.perform(delete("/api/parents/1/wards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(parentService, times(1)).removeWardFromParent(1L, 1L);
    }

    @Test
    void getParentsWithNoWards_ShouldReturnList() throws Exception {
        List<ParentDTO> parents = Arrays.asList(testParentDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.getParentsWithNoWards()).thenReturn(parents);

        mockMvc.perform(get("/api/parents/no-wards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(parentService, times(1)).getParentsWithNoWards();
    }

    @Test
    void createMultipleParents_ShouldReturnCreatedParents() throws Exception {
        List<ParentDTO> parents = Arrays.asList(testParentDTO, testParentDTO);
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.createMultipleParents(anyList())).thenReturn(parents);

        mockMvc.perform(post("/api/parents/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parents)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        verify(parentService, times(1)).createMultipleParents(anyList());
    }

    @Test
    void getParentStats_ShouldReturnStatistics() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testAdminUser);
        when(parentService.getTotalParentCount()).thenReturn(10L);
        when(parentService.getParentsWithNoWards()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/parents/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParents").value(10));

        verify(parentService, times(1)).getTotalParentCount();
        verify(parentService, times(1)).getParentsWithNoWards();
    }

    @Test
    void getWardTermResult_ShouldReturnResult() throws Exception {
        Map<String, Object> resultSheet = new HashMap<>();
        resultSheet.put("studentId", 1L);
        resultSheet.put("average", 85.5);

        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(resultService.generateResultSheet(eq(1L), anyString(), any(Result.Term.class)))
                .thenReturn(resultSheet);

        mockMvc.perform(get("/api/parents/me/wards/1/results/term")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1L));

        verify(resultService, times(1)).generateResultSheet(eq(1L), anyString(), any(Result.Term.class));
    }

    @Test
    void getWardSessionResult_ShouldReturnSessionResult() throws Exception {
        SessionResultResponseDTO sessionResult = new SessionResultResponseDTO();
        sessionResult.setStudentId(1L);
        sessionResult.setAnnualAverage(82.5);

        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(sessionResultService.calculateSessionResult(eq(1L), anyString()))
                .thenReturn(sessionResult);

        mockMvc.perform(get("/api/parents/me/wards/1/results/session")
                        .param("session", "2023/2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1L));

        verify(sessionResultService, times(1)).calculateSessionResult(eq(1L), anyString());
    }

    @Test
    void getWardAttendance_ShouldReturnAttendance() throws Exception {
        AttendanceSummary summary = new AttendanceSummary();
        summary.setStudent(testStudent);
        summary.setAttendancePercentage(95.0);

        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceService.getStudentTermSummary(eq(1L), anyString(), any(Result.Term.class)))
                .thenReturn(summary);

        mockMvc.perform(get("/api/parents/me/wards/1/attendance")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendancePercentage").value(95.0));

        verify(attendanceService, times(1)).getStudentTermSummary(eq(1L), anyString(), any(Result.Term.class));
    }

    @Test
    void getWardFees_ShouldReturnFees() throws Exception {
        Fee fee = new Fee();
        fee.setId(1L);
        fee.setAmount(50000.0);
        fee.setStatus(Fee.PaymentStatus.PENDING);

        when(securityUtils.getCurrentUser()).thenReturn(testParentUser);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeService.getStudentFees(eq(1L), anyString(), any(Fee.Term.class)))
                .thenReturn(Arrays.asList(fee));

        mockMvc.perform(get("/api/parents/me/wards/1/fees")
                        .param("session", "2023/2024")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(feeService, times(1)).getStudentFees(eq(1L), anyString(), any(Fee.Term.class));
    }

    @Test
    void handleOptions_ShouldReturnOk() throws Exception {
        mockMvc.perform(options("/api/parents"))
                .andExpect(status().isOk());
    }
}