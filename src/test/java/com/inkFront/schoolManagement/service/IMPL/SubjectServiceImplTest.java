package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.*;
import com.inkFront.schoolManagement.repository.*;
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
class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Subject testSubject;
    private SubjectRequestDTO testRequest;
    private SchoolClass testClass;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        testSubject = new Subject();
        testSubject.setId(1L);
        testSubject.setName("Mathematics");
        testSubject.setCode("MATH101");
        testSubject.setActive(true);

        testRequest = new SubjectRequestDTO();
        testRequest.setName("Mathematics");
        testRequest.setCode("MATH101");
        testRequest.setActive(true);

        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");

        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");
    }

    @Test
    void createSubject_ShouldCreateSubject() {
        when(subjectRepository.existsByNameIgnoreCase("Mathematics")).thenReturn(false);
        when(subjectRepository.existsByCodeIgnoreCase("MATH101")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        SubjectResponseDTO result = subjectService.createSubject(testRequest);

        assertNotNull(result);
        assertEquals("Mathematics", result.getName());
        verify(subjectRepository, times(1)).save(any(Subject.class));
    }

    @Test
    void createSubject_WithExistingName_ShouldThrowException() {
        when(subjectRepository.existsByNameIgnoreCase("Mathematics")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            subjectService.createSubject(testRequest);
        });
    }

    @Test
    void createSubject_WithExistingCode_ShouldThrowException() {
        when(subjectRepository.existsByNameIgnoreCase("Mathematics")).thenReturn(false);
        when(subjectRepository.existsByCodeIgnoreCase("MATH101")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            subjectService.createSubject(testRequest);
        });
    }

    @Test
    void updateSubject_ShouldUpdateSubject() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        when(subjectRepository.findByCodeIgnoreCase("MATH101")).thenReturn(Optional.of(testSubject));
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        SubjectResponseDTO result = subjectService.updateSubject(1L, testRequest);

        assertNotNull(result);
        verify(subjectRepository, times(1)).save(any(Subject.class));
    }

    @Test
    void updateSubject_WithNonExistentId_ShouldThrowException() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            subjectService.updateSubject(999L, testRequest);
        });
    }

    @Test
    void deleteSubject_ShouldDeleteSubject() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        doNothing().when(subjectRepository).delete(testSubject);

        subjectService.deleteSubject(1L);

        verify(subjectRepository, times(1)).delete(testSubject);
    }

    @Test
    void getSubjectById_ShouldReturnSubject() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

        SubjectResponseDTO result = subjectService.getSubjectById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllSubjects_ShouldReturnList() {
        List<Subject> subjects = Arrays.asList(testSubject);
        when(subjectRepository.findAll()).thenReturn(subjects);

        List<SubjectResponseDTO> result = subjectService.getAllSubjects();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getActiveSubjects_ShouldReturnList() {
        List<Subject> subjects = Arrays.asList(testSubject);
        when(subjectRepository.findByActiveTrue()).thenReturn(subjects);

        List<SubjectResponseDTO> result = subjectService.getActiveSubjects();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void toggleSubjectStatus_ShouldToggle() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        SubjectResponseDTO result = subjectService.toggleSubjectStatus(1L, false);

        assertNotNull(result);
        verify(subjectRepository, times(1)).save(any(Subject.class));
    }

    @Test
    void assignSubjectToClass_ShouldAssign() {
        ClassSubjectRequestDTO request = new ClassSubjectRequestDTO();
        request.setSubjectId(1L);
        request.setClassName("Grade 10");
        request.setClassArm("A");

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(classRepository.findByClassNameAndArm("Grade 10", "A")).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassAndSubject(testClass, testSubject))
                .thenReturn(Optional.empty());
        when(classSubjectRepository.save(any(ClassSubject.class))).thenReturn(new ClassSubject());

        ClassSubjectResponseDTO result = subjectService.assignSubjectToClass(request);

        assertNotNull(result);
        verify(classSubjectRepository, times(1)).save(any(ClassSubject.class));
    }

    @Test
    void assignSubjectToClass_AlreadyAssigned_ShouldThrowException() {
        ClassSubjectRequestDTO request = new ClassSubjectRequestDTO();
        request.setSubjectId(1L);
        request.setClassName("Grade 10");
        request.setClassArm("A");

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(classRepository.findByClassNameAndArm("Grade 10", "A")).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassAndSubject(testClass, testSubject))
                .thenReturn(Optional.of(new ClassSubject()));

        assertThrows(RuntimeException.class, () -> {
            subjectService.assignSubjectToClass(request);
        });
    }

    @Test
    void removeSubjectFromClass_ShouldRemove() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(classRepository.findByClassNameAndArm("Grade 10", "A")).thenReturn(Optional.of(testClass));
        doNothing().when(classSubjectRepository).deleteBySchoolClassAndSubject(testClass, testSubject);

        subjectService.removeSubjectFromClass("Grade 10", "A", 1L);

        verify(classSubjectRepository, times(1)).deleteBySchoolClassAndSubject(testClass, testSubject);
    }

    @Test
    void getSubjectsForClass_ShouldReturnList() {
        ClassSubject classSubject = new ClassSubject();
        classSubject.setSubject(testSubject);

        when(classRepository.findByClassNameAndArm("Grade 10", "A")).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(testClass))
                .thenReturn(Arrays.asList(classSubject));

        List<ClassSubjectResponseDTO> result = subjectService.getSubjectsForClass("Grade 10", "A");

        assertNotNull(result);
        assertEquals(1, result.size());
    }
    @Test
    void assignSubjectToTeacher_ShouldAssign() {
        TeacherSubjectRequestDTO request = new TeacherSubjectRequestDTO();
        request.setTeacherId(1L);
        request.setSubjectId(1L);
        request.setClassName("Grade 10");
        request.setClassArm("A");

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(teacherSubjectRepository.findByTeacherAndSubjectAndClassNameAndClassArm(
                testTeacher, testSubject, "Grade 10", "A"))
                .thenReturn(Optional.empty());

        TeacherSubject savedTeacherSubject = new TeacherSubject();
        savedTeacherSubject.setId(1L);
        savedTeacherSubject.setTeacher(testTeacher);
        savedTeacherSubject.setSubject(testSubject);
        savedTeacherSubject.setClassName("Grade 10");
        savedTeacherSubject.setClassArm("A");

        when(teacherSubjectRepository.save(any(TeacherSubject.class))).thenReturn(savedTeacherSubject);

        TeacherSubjectResponseDTO result = subjectService.assignSubjectToTeacher(request);

        assertNotNull(result);
        assertEquals("John Smith", result.getTeacherName());
        assertEquals("Mathematics", result.getSubjectName());
        verify(teacherSubjectRepository, times(1)).save(any(TeacherSubject.class));
    }
    @Test
    void removeTeacherSubject_ShouldRemove() {
        TeacherSubject teacherSubject = new TeacherSubject();
        teacherSubject.setId(1L);

        when(teacherSubjectRepository.findById(1L)).thenReturn(Optional.of(teacherSubject));
        doNothing().when(teacherSubjectRepository).delete(teacherSubject);

        subjectService.removeTeacherSubject(1L);

        verify(teacherSubjectRepository, times(1)).delete(teacherSubject);
    }

    void getTeacherSubjects_ShouldReturnList() {
        TeacherSubject teacherSubject = new TeacherSubject();
        teacherSubject.setId(1L);
        teacherSubject.setTeacher(testTeacher);
        teacherSubject.setSubject(testSubject);
        teacherSubject.setClassName("Grade 10");
        teacherSubject.setClassArm("A");

        when(teacherSubjectRepository.findByTeacher_IdOrderByClassNameAscClassArmAsc(1L))
                .thenReturn(Arrays.asList(teacherSubject));

        List<TeacherSubjectResponseDTO> result = subjectService.getTeacherSubjects(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).getTeacherName());
        assertEquals("Mathematics", result.get(0).getSubjectName());
    }
}