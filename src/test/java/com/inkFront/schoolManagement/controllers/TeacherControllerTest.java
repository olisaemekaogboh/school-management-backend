package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.model.TeacherInvitation;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.TeacherSubjectRepository;
import com.inkFront.schoolManagement.security.JwtService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AttendanceService;
import com.inkFront.schoolManagement.service.EmailService;
import com.inkFront.schoolManagement.service.ResultService;
import com.inkFront.schoolManagement.service.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TeacherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TeacherService teacherService;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private ResultService resultService;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;

    @InjectMocks
    private TeacherController teacherController;

    private ObjectMapper objectMapper;
    private TeacherDTO teacherDTO;
    private TeacherInviteDTO inviteDTO;
    private CompleteRegistrationDTO completeDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teacherController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        teacherDTO = new TeacherDTO();
        teacherDTO.setId(1L);
        teacherDTO.setFirstName("Jane");
        teacherDTO.setLastName("Doe");
        teacherDTO.setEmail("jane@example.com");

        inviteDTO = new TeacherInviteDTO();
        inviteDTO.setEmail("invite@example.com");
        inviteDTO.setFirstName("Jane");
        inviteDTO.setLastName("Doe");

        completeDTO = new CompleteRegistrationDTO();
        completeDTO.setToken("valid-token");
        completeDTO.setUsername("janedoe");
        completeDTO.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("janedoe");
        testUser.setEmail("jane@example.com");
        testUser.setRole(User.Role.TEACHER);
    }

    @Test
    void getAllTeachers_ShouldReturnList() throws Exception {
        when(teacherService.getAllTeachers()).thenReturn(List.of(teacherDTO));

        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTeachersPaginated_ShouldReturnPage() throws Exception {
        when(teacherService.getAllTeachersPaginated(any()))
                .thenReturn(new PageImpl<>(List.of(teacherDTO), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/teachers/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getTeacher_ShouldReturnTeacher() throws Exception {
        when(teacherService.getTeacherDTO(1L)).thenReturn(teacherDTO);

        mockMvc.perform(get("/api/teachers/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getTeacherByTeacherId_ShouldReturnTeacher() throws Exception {
        when(teacherService.getTeacherByTeacherId("TCH001")).thenReturn(teacherDTO);

        mockMvc.perform(get("/api/teachers/teacher-id/TCH001"))
                .andExpect(status().isOk());
    }

    @Test
    void createTeacher_ShouldReturnCreatedTeacher() throws Exception {
        when(teacherService.createTeacher(any(TeacherDTO.class), any())).thenReturn(teacherDTO);

        MockMultipartFile teacherPart = new MockMultipartFile(
                "teacher",
                "teacher.json",
                "application/json",
                objectMapper.writeValueAsBytes(teacherDTO)
        );

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "pic.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/api/teachers")
                        .file(teacherPart)
                        .file(file))
                .andExpect(status().isCreated());
    }

    @Test
    void updateTeacher_ShouldReturnUpdatedTeacher() throws Exception {
        when(teacherService.updateTeacher(eq(1L), any(TeacherDTO.class), any())).thenReturn(teacherDTO);

        MockMultipartFile teacherPart = new MockMultipartFile(
                "teacher",
                "teacher.json",
                "application/json",
                objectMapper.writeValueAsBytes(teacherDTO)
        );

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "pic.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/api/teachers/1")
                        .file(teacherPart)
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTeacher_ShouldReturnNoContent() throws Exception {
        doNothing().when(teacherService).deleteTeacher(1L);

        mockMvc.perform(delete("/api/teachers/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchTeachers_ShouldReturnList() throws Exception {
        when(teacherService.searchTeachers("jane")).thenReturn(List.of(teacherDTO));

        mockMvc.perform(get("/api/teachers/search").param("term", "jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTeachersByStatus_ShouldReturnList() throws Exception {
        when(teacherService.getTeachersByStatus("ACTIVE")).thenReturn(List.of(teacherDTO));

        mockMvc.perform(get("/api/teachers/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTeachersBySubject_ShouldReturnList() throws Exception {
        when(teacherService.getTeachersBySubject("Mathematics")).thenReturn(List.of(teacherDTO));

        mockMvc.perform(get("/api/teachers/subject/Mathematics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getTeachersByDepartment_ShouldReturnList() throws Exception {
        when(teacherService.getTeachersByDepartment("Science")).thenReturn(List.of(teacherDTO));

        mockMvc.perform(get("/api/teachers/department/Science"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void addSubject_ShouldReturnTeacher() throws Exception {
        when(teacherService.addSubject(1L, "Mathematics")).thenReturn(teacherDTO);

        mockMvc.perform(post("/api/teachers/1/subjects").param("subject", "Mathematics"))
                .andExpect(status().isOk());
    }

    @Test
    void removeSubject_ShouldReturnTeacher() throws Exception {
        when(teacherService.removeSubject(1L, "Mathematics")).thenReturn(teacherDTO);

        mockMvc.perform(delete("/api/teachers/1/subjects").param("subject", "Mathematics"))
                .andExpect(status().isOk());
    }

    @Test
    void addQualification_ShouldReturnTeacher() throws Exception {
        when(teacherService.addQualification(1L, "B.Ed")).thenReturn(teacherDTO);

        mockMvc.perform(post("/api/teachers/1/qualifications").param("qualification", "B.Ed"))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmploymentStatus_ShouldReturnTeacher() throws Exception {
        when(teacherService.updateEmploymentStatus(1L, "ACTIVE")).thenReturn(teacherDTO);

        mockMvc.perform(patch("/api/teachers/1/status").param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    void getTeacherStatistics_ShouldReturnMap() throws Exception {
        when(teacherService.getTeacherStatistics()).thenReturn(Map.of("totalTeachers", 8));

        mockMvc.perform(get("/api/teachers/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTeachers").value(8));
    }

    @Test
    void generateTeacherId_ShouldReturnValue() throws Exception {
        when(teacherService.generateTeacherId()).thenReturn("TCH009");

        mockMvc.perform(get("/api/teachers/generate-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value("TCH009"));
    }

    @Test
    void checkEmailExists_ShouldReturnExists() throws Exception {
        when(teacherService.checkEmailExists("jane@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/teachers/check-email").param("email", "jane@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void checkTeacherIdExists_ShouldReturnExists() throws Exception {
        when(teacherService.checkTeacherIdExists("TCH001")).thenReturn(true);

        mockMvc.perform(get("/api/teachers/check-teacher-id").param("teacherId", "TCH001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void exportToPdf_ShouldReturnBytes() throws Exception {
        when(teacherService.exportToPDF()).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/teachers/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void exportToExcel_ShouldReturnBytes() throws Exception {
        when(teacherService.exportToExcel()).thenReturn("xls".getBytes());

        mockMvc.perform(get("/api/teachers/export/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.ms-excel"));
    }

    @Test
    void inviteTeacher_ShouldReturnSuccess() throws Exception {
        doNothing().when(teacherService).createInvitation(any(TeacherInviteDTO.class), anyString());
        doNothing().when(emailService).sendTeacherInvitation(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/teachers/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void inviteTeacher_WhenEmailMissing_ShouldReturnBadRequest() throws Exception {
        TeacherInviteDTO bad = new TeacherInviteDTO();
        bad.setFirstName("Jane");
        bad.setLastName("Doe");

        mockMvc.perform(post("/api/teachers/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyInvitationToken_ShouldReturnValidResponse() throws Exception {
        TeacherInvitation invitation = new TeacherInvitation();
        invitation.setFirstName("Jane");
        invitation.setLastName("Doe");
        invitation.setEmail("invite@example.com");
        invitation.setPhoneNumber("08000000000");
        invitation.setExpiryDate(LocalDateTime.now().plusDays(2));

        when(teacherService.verifyInvitationToken("valid-token")).thenReturn(invitation);

        mockMvc.perform(get("/api/teachers/verify-invitation").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("invite@example.com"));
    }

    @Test
    void completeTeacherRegistration_ShouldReturnLoginResponse() throws Exception {
        when(teacherService.completeRegistration(any(CompleteRegistrationDTO.class))).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

        mockMvc.perform(post("/api/teachers/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void completeTeacherRegistration_WhenTokenMissing_ShouldReturnBadRequest() throws Exception {
        CompleteRegistrationDTO bad = new CompleteRegistrationDTO();
        bad.setUsername("janedoe");
        bad.setPassword("password123");

        mockMvc.perform(post("/api/teachers/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void resendInvitation_ShouldReturnSuccess() throws Exception {
        doNothing().when(teacherService).resendInvitation("invite@example.com");

        mockMvc.perform(post("/api/teachers/resend-invitation")
                        .param("email", "invite@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPendingInvitations_ShouldReturnList() throws Exception {
        TeacherInvitationDTO invitationDTO = new TeacherInvitationDTO();
        invitationDTO.setId(1L);
        invitationDTO.setEmail("invite@example.com");

        when(teacherService.getPendingInvitations()).thenReturn(List.of(invitationDTO));

        mockMvc.perform(get("/api/teachers/invitations/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cancelInvitation_ShouldReturnSuccess() throws Exception {
        doNothing().when(teacherService).cancelInvitation(1L);

        mockMvc.perform(delete("/api/teachers/invitations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}