package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.ParentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ParentService parentService;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private PublicController publicController;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");

        // ✅ FIX
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassName("Grade 10");
        schoolClass.setArm("A");

        testStudent.setSchoolClass(schoolClass);
        testStudent.setStatus(Student.StudentStatus.ACTIVE);
    }

    @Test
    void verifyStudent_WhenFound_ShouldReturnStudentPublicInfo() throws Exception {
        when(studentRepository.findByAdmissionNumber("STU001"))
                .thenReturn(Optional.of(testStudent));

        mockMvc.perform(get("/api/public/verify-student")
                        .param("admissionNumber", "STU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionNumber").value("STU001"))
                .andExpect(jsonPath("$.className").value("Grade 10"))
                .andExpect(jsonPath("$.classArm").value("A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(studentRepository, times(1))
                .findByAdmissionNumber("STU001");
    }
}