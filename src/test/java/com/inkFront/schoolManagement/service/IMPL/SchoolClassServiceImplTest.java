package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SchoolClassDTO;
import com.inkFront.schoolManagement.model.ClassSubject;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ClassSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolClassServiceImplTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @InjectMocks
    private SchoolClassServiceImpl schoolClassService;

    private SchoolClass testClass;
    private SchoolClassDTO testClassDTO;
    private Teacher testTeacher;
    private Subject testSubject;

    @BeforeEach
    void setUp() {
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");

        testSubject = new Subject();
        testSubject.setId(1L);
        testSubject.setName("Mathematics"); // Changed from setSessionName to setName

        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");
        testClass.setClassCode("G10A");
        testClass.setCategory(SchoolClass.ClassCategory.SENIOR_SECONDARY); // Changed from SECONDARY to SENIOR_SECONDARY
        testClass.setCapacity(30);
        testClass.setCurrentEnrollment(25);
        testClass.setClassTeacher(testTeacher);

        testClassDTO = new SchoolClassDTO();
        testClassDTO.setId(1L);
        testClassDTO.setClassName("Grade 10");
        testClassDTO.setArm("A");
        testClassDTO.setClassCode("G10A");
        testClassDTO.setCategory("SENIOR_SECONDARY"); // Changed from SECONDARY to SENIOR_SECONDARY
        testClassDTO.setCapacity(30);
        testClassDTO.setCurrentEnrollment(25);
    }

    @Test
    void createClass_ShouldCreateClass() {
        when(classRepository.save(any(SchoolClass.class))).thenReturn(testClass);

        SchoolClassDTO result = schoolClassService.createClass(testClassDTO);

        assertNotNull(result);
        assertEquals("Grade 10", result.getClassName());
        verify(classRepository, times(1)).save(any(SchoolClass.class));
    }

    @Test
    void updateClass_ShouldUpdateClass() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.save(any(SchoolClass.class))).thenReturn(testClass);

        SchoolClassDTO result = schoolClassService.updateClass(1L, testClassDTO);

        assertNotNull(result);
        verify(classRepository, times(1)).save(any(SchoolClass.class));
    }

    @Test
    void updateClass_WithNonExistentId_ShouldThrowException() {
        when(classRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            schoolClassService.updateClass(999L, testClassDTO);
        });
    }

    @Test
    void getClass_ShouldReturnClass() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(testClass))
                .thenReturn(Arrays.asList());

        SchoolClassDTO result = schoolClassService.getClass(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getClass_WithNonExistentId_ShouldThrowException() {
        when(classRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            schoolClassService.getClass(999L);
        });
    }

    @Test
    void getAllClasses_ShouldReturnList() {
        List<SchoolClass> classes = Arrays.asList(testClass);
        when(classRepository.findAll()).thenReturn(classes);
        when(classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(any(SchoolClass.class)))
                .thenReturn(Arrays.asList());

        List<SchoolClassDTO> result = schoolClassService.getAllClasses();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void deleteClass_ShouldDeleteClass() {
        doNothing().when(classRepository).deleteById(1L);

        schoolClassService.deleteClass(1L);

        verify(classRepository, times(1)).deleteById(1L);
    }

    @Test
    void getClass_WithSubjects_ShouldIncludeSubjects() {
        ClassSubject classSubject = new ClassSubject();
        classSubject.setSubject(testSubject);

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(testClass))
                .thenReturn(Arrays.asList(classSubject));

        SchoolClassDTO result = schoolClassService.getClass(1L);

        assertNotNull(result);
        assertEquals(1, result.getSubjects().size());
        assertEquals("Mathematics", result.getSubjects().get(0));
    }

    @Test
    void getClass_WithClassTeacher_ShouldIncludeTeacher() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(testClass))
                .thenReturn(Arrays.asList());

        SchoolClassDTO result = schoolClassService.getClass(1L);

        assertNotNull(result);
        assertEquals(1L, result.getClassTeacherId());
        assertEquals("John Smith", result.getClassTeacherName());
    }
}