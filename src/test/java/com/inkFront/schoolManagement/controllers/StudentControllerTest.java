package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkFront.schoolManagement.dto.StudentRequestDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudentService studentService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private StudentController studentController;

    private ObjectMapper objectMapper;
    private Student student;
    private StudentRequestDTO requestDTO;
    private User studentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(10L);
        schoolClass.setClassName("JSS1");
        schoolClass.setArm("A");

        student = new Student();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setAdmissionNumber("ADM001");
        student.setSchoolClass(schoolClass);
        student.setDateOfBirth(LocalDate.of(2012, 1, 1));
        student.setStatus(Student.StudentStatus.ACTIVE);
        student.setParentName("Jane Doe");
        student.setParentPhone("08012345678");
        student.setParentEmail("parent@example.com");
        student.setAddress("12 School Road");

        requestDTO = new StudentRequestDTO();
        requestDTO.setFirstName("John");
        requestDTO.setLastName("Doe");
        requestDTO.setMiddleName("Michael");
        requestDTO.setClassId(10L);
        requestDTO.setGender(Student.Gender.MALE);
        requestDTO.setDateOfBirth(LocalDate.of(2012, 1, 1));
        requestDTO.setParentName("Jane Doe");
        requestDTO.setParentPhone("08012345678");
        requestDTO.setParentEmail("parent@example.com");
        requestDTO.setAddress("12 School Road");
        requestDTO.setLocalGovtArea("Onitsha North");
        requestDTO.setStateOfOrigin("Anambra");
        requestDTO.setNationality("Nigerian");
        requestDTO.setReligion("Christianity");
        requestDTO.setEmergencyContactName("Jane Doe");
        requestDTO.setEmergencyContactPhone("08012345678");
        requestDTO.setEmergencyContactRelationship("Mother");
        requestDTO.setStatus(Student.StudentStatus.ACTIVE);
        requestDTO.setPreviousSchool("Nursery Test School");
        requestDTO.setExcludeFromPromotion(false);

        studentUser = new User();
        studentUser.setId(100L);
        studentUser.setRole(User.Role.STUDENT);
        studentUser.setStudent(student);
    }

    @Test
    void registerStudentJson_ShouldReturnCreatedStudent() {
        when(studentService.registerStudent(any(Student.class))).thenReturn(student);

        ResponseEntity<StudentResponseDTO> response = studentController.registerStudentJson(requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("ADM001", response.getBody().getAdmissionNumber());

        verify(studentService, times(1)).registerStudent(any(Student.class));
    }

    @Test
    void registerStudentWithFile_ShouldReturnCreatedStudent() throws Exception {
        when(studentService.registerStudent(any(Student.class))).thenReturn(student);

        MockMultipartFile studentJson = new MockMultipartFile(
                "student",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(requestDTO)
        );

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "pic.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/api/students")
                        .file(studentJson)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(studentService, times(1)).registerStudent(any(Student.class));
    }

    @Test
    void registerStudentWithFilePart_WhenMissingStudentPart_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "pic.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/api/students/with-file")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStudentJson_ShouldReturnUpdatedStudent() {
        when(studentService.updateStudent(eq(1L), any(Student.class))).thenReturn(student);

        ResponseEntity<StudentResponseDTO> response = studentController.updateStudentJson(1L, requestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());

        verify(studentService, times(1)).updateStudent(eq(1L), any(Student.class));
    }

    @Test
    void getAllStudents_ShouldReturnList() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(studentService, times(1)).getAllStudents();
    }

    @Test
    void getAllStudentsPaginated_ShouldReturnPage() throws Exception {
        when(studentService.getAllStudentsPaginated(any()))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/students/paginated")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(studentService, times(1)).getAllStudentsPaginated(any());
    }

    @Test
    void getStudentById_ShouldReturnStudent() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(studentService, times(1)).getStudentById(1L);
    }

    @Test
    void getStudentById_WhenNotFound_ShouldReturn404() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStudentByAdmissionNumber_ShouldReturnStudent() throws Exception {
        when(studentService.getStudentByAdmissionNumber("ADM001")).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/students/admission/ADM001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionNumber").value("ADM001"));

        verify(studentService, times(1)).getStudentByAdmissionNumber("ADM001");
    }

    @Test
    void searchStudents_ShouldReturnList() throws Exception {
        when(studentService.searchStudents("john")).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/search").param("term", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(studentService, times(1)).searchStudents("john");
    }

    @Test
    void getStudentsByClassId_ShouldReturnList() throws Exception {
        when(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(10L)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/class/id/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(studentRepository, times(1)).findBySchoolClassIdOrderByLastNameAscFirstNameAsc(10L);
    }

    @Test
    void getStudentsByState_ShouldReturnList() throws Exception {
        when(studentService.getStudentsByState("Anambra")).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/state/Anambra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStudentsByLGA_ShouldReturnList() throws Exception {
        when(studentService.getStudentsByLGA("Onitsha North")).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/lga/Onitsha North"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getActiveStudents_ShouldReturnList() throws Exception {
        when(studentService.getActiveStudents()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStudentsByStatus_ShouldReturnList() throws Exception {
        when(studentService.getStudentsByStatus(Student.StudentStatus.ACTIVE)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStudentStatistics_ShouldReturnMap() throws Exception {
        when(studentService.getTotalStudentCount()).thenReturn(10L);
        when(studentService.getActiveStudentCount()).thenReturn(8L);
        when(studentService.getStudentCountByClass()).thenReturn(Map.of("JSS1-A", 5L));
        when(studentService.getRecentAdmissions(30)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(10))
                .andExpect(jsonPath("$.activeStudents").value(8));

        verify(studentService, times(1)).getTotalStudentCount();
        verify(studentService, times(1)).getStudentCountByClass();
    }

    @Test
    void deleteStudent_ShouldReturnNoContent() throws Exception {
        doNothing().when(studentService).deleteStudent(1L);

        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isNoContent());

        verify(studentService, times(1)).deleteStudent(1L);
    }

    @Test
    void deleteStudentByAdmissionNumber_ShouldReturnNoContent() throws Exception {
        doNothing().when(studentService).deleteStudentByAdmissionNumber("ADM001");

        mockMvc.perform(delete("/api/students/admission/ADM001"))
                .andExpect(status().isNoContent());

        verify(studentService, times(1)).deleteStudentByAdmissionNumber("ADM001");
    }

    @Test
    void registerBulkStudents_ShouldReturnCreatedList() {
        when(studentService.registerBulkStudents(anyList())).thenReturn(List.of(student));

        ResponseEntity<List<StudentResponseDTO>> response =
                studentController.registerBulkStudents(List.of(requestDTO));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(studentService, times(1)).registerBulkStudents(anyList());
    }

    @Test
    void bulkUpdateClass_ShouldReturnOk() throws Exception {
        doNothing().when(studentService).updateBulkStudentClass(eq(List.of(1L, 2L)), eq(10L));

        mockMvc.perform(patch("/api/students/bulk/class")
                        .param("newClassId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk());

        verify(studentService, times(1)).updateBulkStudentClass(eq(List.of(1L, 2L)), eq(10L));
    }

    @Test
    void generateAdmissionNumber_ShouldReturnValue() throws Exception {
        when(studentService.generateAdmissionNumber()).thenReturn("ADM999");

        mockMvc.perform(get("/api/students/generate-admission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionNumber").value("ADM999"));
    }

    @Test
    void checkAdmissionNumber_ShouldReturnExistsFalse() throws Exception {
        when(studentService.isAdmissionNumberUnique("ADM001")).thenReturn(true);

        mockMvc.perform(get("/api/students/check-admission/ADM001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void generateStudentReport_ShouldReturnPdf() throws Exception {
        when(studentService.generateStudentReport(1L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/students/1/report"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void generateClassReport_ShouldReturnPdf() throws Exception {
        when(studentService.generateClassReport(10L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/students/class/id/10/report"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void getPromotionPreview_ShouldReturnMap() throws Exception {
        when(studentService.getPromotionPreview()).thenReturn(Map.of("eligible", 5L));

        mockMvc.perform(get("/api/students/promote/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(5));
    }

    @Test
    void getExcludedStudents_ShouldReturnList() throws Exception {
        when(studentService.getExcludedStudents()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students/excluded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void promoteAllStudents_ShouldReturnMap() throws Exception {
        when(studentService.promoteAllStudents()).thenReturn(Map.of("promoted", 10));

        mockMvc.perform(post("/api/students/promote/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promoted").value(10));
    }

    @Test
    void promoteSelectedStudents_ShouldReturnMap() throws Exception {
        when(studentService.promoteSelectedStudents(List.of(1L, 2L))).thenReturn(Map.of("promoted", 2));

        mockMvc.perform(post("/api/students/promote/selected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promoted").value(2));
    }

    @Test
    void togglePromotionExclusion_ShouldReturnStudent() throws Exception {
        when(studentService.togglePromotionExclusion(1L, true, "Hold")).thenReturn(student);

        mockMvc.perform(post("/api/students/1/toggle-exclusion")
                        .param("exclude", "true")
                        .param("reason", "Hold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void promoteClass_ShouldReturnMap() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("promoted", 4);

        when(studentService.promoteClass(10L)).thenReturn(result);

        mockMvc.perform(post("/api/students/promote/class/id/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promoted").value(4))
                .andExpect(jsonPath("$.classId").value(10));
    }

    @Test
    void getMyProfile_WhenNoUser_ShouldReturnUnauthorized() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(null);

        mockMvc.perform(get("/api/students/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not authenticated"));

        verify(studentService, never()).getStudentById(anyLong());
    }

    @Test
    void getMyProfile_WhenNoStudentLinked_ShouldReturnForbidden() throws Exception {
        User user = new User();
        user.setId(11L);
        user.setRole(User.Role.STUDENT);
        user.setStudent(null);

        when(securityUtils.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/students/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account is not linked to a student"));
    }

    @Test
    void getMyProfile_ShouldReturnStudent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(studentUser);
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/students/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(studentService, times(1)).getStudentById(1L);
    }
}