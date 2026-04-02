package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.ClassDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.service.ClassService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClassControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClassService classService;

    @InjectMocks
    private ClassController classController;

    private ObjectMapper objectMapper;
    private SchoolClass testClass;
    private ClassDTO testClassDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(classController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");
        testClass.setClassCode("G10A");

        // Use an enum value that actually exists in your project.
        // Change this one line if your enum names differ.
        testClass.setCategory(SchoolClass.ClassCategory.SENIOR_SECONDARY);

        testClassDTO = ClassDTO.fromEntity(testClass);
    }

    @Test
    void createClass_ShouldReturnCreatedClass() throws Exception {
        when(classService.createClass(any(ClassDTO.class))).thenReturn(testClass);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testClassDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.className").value("Grade 10"))
                .andExpect(jsonPath("$.arm").value("A"));

        verify(classService, times(1)).createClass(any(ClassDTO.class));
    }

    @Test
    void updateClass_ShouldReturnUpdatedClass() throws Exception {
        when(classService.updateClass(eq(1L), any(ClassDTO.class))).thenReturn(testClass);

        mockMvc.perform(put("/api/classes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testClassDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.className").value("Grade 10"));

        verify(classService, times(1)).updateClass(eq(1L), any(ClassDTO.class));
    }

    @Test
    void getClass_ShouldReturnClass() throws Exception {
        when(classService.getClassWithTeacher(1L)).thenReturn(testClass);

        mockMvc.perform(get("/api/classes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.className").value("Grade 10"));

        verify(classService, times(1)).getClassWithTeacher(1L);
    }

    @Test
    void getClassByName_ShouldReturnClass() throws Exception {
        when(classService.getClassByName("Grade 10")).thenReturn(testClass);

        mockMvc.perform(get("/api/classes/name/Grade 10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(classService, times(1)).getClassByName("Grade 10");
    }

    @Test
    void deleteClass_ShouldReturnNoContent() throws Exception {
        doNothing().when(classService).deleteClass(1L);

        mockMvc.perform(delete("/api/classes/1"))
                .andExpect(status().isNoContent());

        verify(classService, times(1)).deleteClass(1L);
    }

    @Test
    void getAllClasses_ShouldReturnList() throws Exception {
        List<ClassDTO> classes = Arrays.asList(testClassDTO);
        when(classService.getAllClasses()).thenReturn(classes);

        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(classService, times(1)).getAllClasses();
    }

    @Test
    void getClassesByCategory_ShouldReturnFilteredList() throws Exception {
        List<ClassDTO> classes = Arrays.asList(testClassDTO);

        // Keep request/service value aligned with actual enum naming in your app.
        when(classService.getClassesByCategory("SENIOR_SECONDARY")).thenReturn(classes);

        mockMvc.perform(get("/api/classes/category/SENIOR_SECONDARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(classService, times(1)).getClassesByCategory("SENIOR_SECONDARY");
    }

    @Test
    void assignClassTeacher_ShouldReturnUpdatedClass() throws Exception {
        when(classService.assignClassTeacher(1L, 10L)).thenReturn(testClass);

        mockMvc.perform(post("/api/classes/1/assign-teacher/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(classService, times(1)).assignClassTeacher(1L, 10L);
    }

    @Test
    void addSubject_ShouldReturnUpdatedClass() throws Exception {
        when(classService.addSubject(1L, "Physics")).thenReturn(testClass);

        mockMvc.perform(post("/api/classes/1/subjects")
                        .param("subject", "Physics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(classService, times(1)).addSubject(1L, "Physics");
    }

    @Test
    void removeSubject_ShouldReturnUpdatedClass() throws Exception {
        when(classService.removeSubject(1L, "Mathematics")).thenReturn(testClass);

        mockMvc.perform(delete("/api/classes/1/subjects")
                        .param("subject", "Mathematics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(classService, times(1)).removeSubject(1L, "Mathematics");
    }

    @Test
    void getStudentsInClass_ShouldReturnStudentList() throws Exception {
        List<StudentResponseDTO> students = Arrays.asList(new StudentResponseDTO(), new StudentResponseDTO());
        when(classService.getStudentsInClass(1L)).thenReturn(students);

        mockMvc.perform(get("/api/classes/1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(classService, times(1)).getStudentsInClass(1L);
    }

    @Test
    void getClassStatistics_ShouldReturnStatistics() throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalClasses", 10);
        statistics.put("totalStudents", 300);
        statistics.put("averageClassSize", 30);

        when(classService.getClassStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/api/classes/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClasses").value(10))
                .andExpect(jsonPath("$.totalStudents").value(300));

        verify(classService, times(1)).getClassStatistics();
    }

    @Test
    void exportClassListPdf_ShouldReturnPdfFile() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(classService.generateClassListPdf(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/classes/1/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=class_list.pdf"));

        verify(classService, times(1)).generateClassListPdf(1L);
    }

    @Test
    void exportClassListExcel_ShouldReturnExcelFile() throws Exception {
        byte[] excelBytes = "EXCEL_CONTENT".getBytes();
        when(classService.generateClassListExcel(1L)).thenReturn(excelBytes);

        mockMvc.perform(get("/api/classes/1/export/excel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=class_list.xlsx"));

        verify(classService, times(1)).generateClassListExcel(1L);
    }

    @Test
    void createClass_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        ClassDTO invalidDTO = new ClassDTO();

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(classService, never()).createClass(any());
    }

    @Test
    void getClass_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(classService.getClassWithTeacher(999L))
                .thenThrow(new RuntimeException("Class not found"));

        mockMvc.perform(get("/api/classes/999"))
                .andExpect(status().isNotFound());

        verify(classService, times(1)).getClassWithTeacher(999L);
    }
}