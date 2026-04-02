// src/test/java/com/inkFront/schoolManagement/service/IMPL/StudentServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ParentRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private ClassRepository classRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student testStudent;
    private SchoolClass testClass;
    private Parent testParent;

    @BeforeEach
    void setUp() {
        testClass = SchoolClass.builder()
                .id(1L)
                .className("JSS 1")
                .arm("A")
                .classCode("JSS1-A")
                .build();

        testParent = Parent.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("parent@example.com")
                .phoneNumber("08012345678")
                .build();

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("NIS/2026/0001");
        testStudent.setSchoolClass(testClass);
        testStudent.setParentEmail("parent@example.com");
        testStudent.setParentName("Jane Doe");
        testStudent.setParentPhone("08012345678");
        testStudent.setStatus(Student.StudentStatus.ACTIVE);
        testStudent.setAdmissionDate(LocalDate.now());
        testStudent.setProfilePictureUrl("/uploads/profile-pictures/default.png");
    }

    @Test
    void registerStudent_ShouldRegisterStudent() {
        Student input = new Student();
        input.setFirstName("John");
        input.setLastName("Doe");
        input.setSchoolClass(testClass);
        input.setParentEmail("parent@example.com");

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(parentRepository.findByEmailIgnoreCase("parent@example.com")).thenReturn(Optional.of(testParent));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = studentService.registerStudent(input);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals(testClass, result.getSchoolClass());
        assertEquals(testParent, result.getParent());
        assertNotNull(result.getAdmissionNumber());
        assertEquals(Student.StudentStatus.ACTIVE, result.getStatus());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void registerStudent_ShouldThrow_WhenClassMissing() {
        Student input = new Student();
        input.setFirstName("John");
        input.setLastName("Doe");

        assertThrows(ResourceNotFoundException.class, () -> studentService.registerStudent(input));
    }

    @Test
    void updateStudent_ShouldUpdateStudent() {
        Student update = new Student();
        update.setFirstName("Johnny");
        update.setLastName("Doe");
        update.setMiddleName("Junior");
        update.setSchoolClass(testClass);
        update.setParentEmail("parent@example.com");
        update.setParentName("");
        update.setParentPhone("");
        update.setStatus(Student.StudentStatus.ACTIVE);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(parentRepository.findByEmailIgnoreCase("parent@example.com")).thenReturn(Optional.of(testParent));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = studentService.updateStudent(1L, update);

        assertNotNull(result);
        assertEquals("Johnny", result.getFirstName());
        assertEquals(testParent, result.getParent());
        assertEquals("Jane Doe", result.getParentName());
        assertEquals("08012345678", result.getParentPhone());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void getStudentById_ShouldReturnStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        Optional<Student> result = studentService.getStudentById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void getStudentByAdmissionNumber_ShouldReturnStudent() {
        when(studentRepository.findByAdmissionNumber("NIS/2026/0001")).thenReturn(Optional.of(testStudent));

        Optional<Student> result = studentService.getStudentByAdmissionNumber("NIS/2026/0001");

        assertTrue(result.isPresent());
        assertEquals("NIS/2026/0001", result.get().getAdmissionNumber());
    }

    @Test
    void getAllStudents_ShouldReturnSortedStudents() {
        when(studentRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName")))
                .thenReturn(List.of(testStudent));

        List<Student> result = studentService.getAllStudents();

        assertEquals(1, result.size());
    }

    @Test
    void getAllStudentsPaginated_ShouldReturnPage() {
        when(studentRepository.findAllWithDetails(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(testStudent)));

        var result = studentService.getAllStudentsPaginated(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void deleteStudent_ShouldDeleteStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        doNothing().when(studentRepository).delete(testStudent);

        studentService.deleteStudent(1L);

        verify(studentRepository).delete(testStudent);
    }

    @Test
    void deleteStudentByAdmissionNumber_ShouldDeleteStudent() {
        when(studentRepository.findByAdmissionNumber("NIS/2026/0001")).thenReturn(Optional.of(testStudent));
        doNothing().when(studentRepository).delete(testStudent);

        studentService.deleteStudentByAdmissionNumber("NIS/2026/0001");

        verify(studentRepository).delete(testStudent);
    }

    @Test
    void searchStudents_ShouldReturnMatches() {
        when(studentRepository.searchByName("john")).thenReturn(List.of(testStudent));

        List<Student> result = studentService.searchStudents("john");

        assertEquals(1, result.size());
    }

    @Test
    void getStudentsByClassId_ShouldReturnMatches() {
        when(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(1L))
                .thenReturn(List.of(testStudent));

        List<Student> result = studentService.getStudentsByClassId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getStudentsByState_ShouldReturnMatches() {
        when(studentRepository.findByStateOfOriginOrderByLastNameAscFirstNameAsc("Anambra"))
                .thenReturn(List.of(testStudent));

        List<Student> result = studentService.getStudentsByState("Anambra");

        assertEquals(1, result.size());
    }

    @Test
    void getStudentsByLGA_ShouldReturnMatches() {
        when(studentRepository.findByLocalGovtAreaOrderByLastNameAscFirstNameAsc("Onitsha North"))
                .thenReturn(List.of(testStudent));

        List<Student> result = studentService.getStudentsByLGA("Onitsha North");

        assertEquals(1, result.size());
    }

    @Test
    void getActiveStudents_ShouldReturnMatches() {
        when(studentRepository.findByStatusOrderByLastNameAscFirstNameAsc(Student.StudentStatus.ACTIVE))
                .thenReturn(List.of(testStudent));

        List<Student> result = studentService.getActiveStudents();

        assertEquals(1, result.size());
    }

    @Test
    void getStudentsByStatus_ShouldReturnMatches() {
        when(studentRepository.findByStatusOrderByLastNameAscFirstNameAsc(Student.StudentStatus.ACTIVE))
                .thenReturn(List.of(testStudent));

        List<Student> result = studentService.getStudentsByStatus(Student.StudentStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void getStudentCountByClass_ShouldReturnMap() {
        when(studentRepository.countStudentsByClassWithArm())
                .thenReturn(Collections.singletonList(new Object[]{1L, "JSS 1", "A", 5L}));

        Map<String, Long> result = studentService.getStudentCountByClass();

        assertEquals(1, result.size());
        assertEquals(5L, result.get("JSS 1 - A"));
    }

    @Test
    void getTotalStudentCount_ShouldReturnCount() {
        when(studentRepository.count()).thenReturn(10L);

        Long result = studentService.getTotalStudentCount();

        assertEquals(10L, result);
    }

    @Test
    void getActiveStudentCount_ShouldReturnCount() {
        when(studentRepository.countByStatus(Student.StudentStatus.ACTIVE)).thenReturn(8L);

        Long result = studentService.getActiveStudentCount();

        assertEquals(8L, result);
    }

    @Test
    void getRecentAdmissions_ShouldReturnStudents() {
        when(studentRepository.findRecentAdmissions(any(LocalDate.class))).thenReturn(List.of(testStudent));

        List<Student> result = studentService.getRecentAdmissions(30);

        assertEquals(1, result.size());
    }

    @Test
    void registerBulkStudents_ShouldRegisterAll() {
        Student second = new Student();
        second.setFirstName("Mary");
        second.setLastName("Smith");
        second.setSchoolClass(testClass);

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(parentRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Student> result = studentService.registerBulkStudents(List.of(testStudent, second));

        assertEquals(2, result.size());
        verify(studentRepository, times(2)).save(any(Student.class));
    }

    @Test
    void updateBulkStudentClass_ShouldUpdateAll() {
        Student second = new Student();
        second.setId(2L);
        second.setFirstName("Mary");
        second.setLastName("Smith");
        second.setSchoolClass(testClass);

        SchoolClass newClass = SchoolClass.builder()
                .id(2L)
                .className("JSS 2")
                .arm("A")
                .build();

        when(studentRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(testStudent, second));
        when(classRepository.findById(2L)).thenReturn(Optional.of(newClass));
        when(studentRepository.saveAll(anyList())).thenReturn(List.of(testStudent, second));

        studentService.updateBulkStudentClass(List.of(1L, 2L), 2L);

        assertEquals(newClass, testStudent.getSchoolClass());
        assertEquals(newClass, second.getSchoolClass());
        verify(studentRepository).saveAll(anyList());
    }

    @Test
    void isAdmissionNumberUnique_ShouldReturnTrueWhenMissing() {
        when(studentRepository.existsByAdmissionNumber("NIS/2026/0001")).thenReturn(false);

        boolean result = studentService.isAdmissionNumberUnique("NIS/2026/0001");

        assertTrue(result);
    }

    @Test
    void generateAdmissionNumber_ShouldGenerateValue() {
        when(studentRepository.count()).thenReturn(12L);

        String result = studentService.generateAdmissionNumber();

        assertNotNull(result);
        assertTrue(result.startsWith("NIS/"));
    }

    @Test
    void generateStudentReport_ShouldThrowWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.generateStudentReport(99L));
    }

    @Test
    void generateClassReport_ShouldThrowWhenClassMissing() {
        when(classRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.generateClassReport(99L));
    }
}