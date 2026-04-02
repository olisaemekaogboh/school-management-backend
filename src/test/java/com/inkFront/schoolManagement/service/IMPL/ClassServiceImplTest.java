package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ClassDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.ClassSubject;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ClassSubjectRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.SubjectRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceImplTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @InjectMocks
    private ClassServiceImpl classService;

    private SchoolClass testClass;
    private ClassDTO testClassDTO;
    private Teacher testTeacher;
    private Subject testSubject;
    private Subject englishSubject;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");
        testClass.setCategory(SchoolClass.ClassCategory.SENIOR_SECONDARY);
        testClass.setCapacity(30);
        testClass.setCurrentEnrollment(0);

        testClassDTO = new ClassDTO();
        testClassDTO.setClassName("Grade 10");
        testClassDTO.setArm("A");
        testClassDTO.setCategory(SchoolClass.ClassCategory.SENIOR_SECONDARY);
        testClassDTO.setCapacity(30);
        testClassDTO.setSubjects(Arrays.asList("Mathematics", "English"));

        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");

        testSubject = new Subject();
        testSubject.setId(1L);
        testSubject.setName("Mathematics");

        englishSubject = new Subject();
        englishSubject.setId(2L);
        englishSubject.setName("English");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Doe");
        testStudent.setSchoolClass(testClass);
    }

    @Test
    void createClass_ShouldCreateNewClass() {
        when(classRepository.existsByClassNameAndArm("Grade 10", "A")).thenReturn(false);
        when(classRepository.save(any(SchoolClass.class))).thenReturn(testClass);
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        when(subjectRepository.findByNameIgnoreCase("English")).thenReturn(Optional.of(englishSubject));
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));

        SchoolClass result = classService.createClass(testClassDTO);

        assertNotNull(result);
        assertEquals("Grade 10", result.getClassName());
        verify(classRepository, times(1)).save(any(SchoolClass.class));
    }

    @Test
    void createClass_WithExistingClass_ShouldThrowException() {
        when(classRepository.existsByClassNameAndArm("Grade 10", "A")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> classService.createClass(testClassDTO));
    }

    @Test
    void createClass_WithClassTeacher_ShouldAssignTeacher() {
        testClassDTO.setClassTeacherId(1L);

        when(classRepository.existsByClassNameAndArm("Grade 10", "A")).thenReturn(false);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(classRepository.save(any(SchoolClass.class))).thenReturn(testClass);
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        when(subjectRepository.findByNameIgnoreCase("English")).thenReturn(Optional.of(englishSubject));
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));

        SchoolClass result = classService.createClass(testClassDTO);

        assertNotNull(result);
        verify(teacherRepository, times(1)).findById(1L);
    }

    @Test
    void updateClass_ShouldUpdateExistingClass() {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.findByClassNameAndArm("Grade 10", "A")).thenReturn(Optional.empty());
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        when(subjectRepository.findByNameIgnoreCase("English")).thenReturn(Optional.of(englishSubject));
        when(classRepository.save(any(SchoolClass.class))).thenReturn(testClass);

        SchoolClass result = classService.updateClass(1L, testClassDTO);

        assertNotNull(result);
        verify(classRepository, times(1)).save(any(SchoolClass.class));
    }

    @Test
    void updateClass_WithNonExistentId_ShouldThrowException() {
        when(classRepository.findByIdWithTeacher(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> classService.updateClass(999L, testClassDTO));
    }

    @Test
    void getClass_ShouldReturnClass() {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));

        SchoolClass result = classService.getClass(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getClass_WithNonExistentId_ShouldThrowException() {
        when(classRepository.findByIdWithTeacher(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> classService.getClass(999L));
    }

    @Test
    void getClassByName_ShouldReturnClass() {
        when(classRepository.findByClassName("Grade 10")).thenReturn(Optional.of(testClass));

        SchoolClass result = classService.getClassByName("Grade 10");

        assertNotNull(result);
        assertEquals("Grade 10", result.getClassName());
    }

    @Test
    void deleteClass_ShouldDeleteWithAssignments() {
        ClassSubject classSubject = new ClassSubject();
        classSubject.setSchoolClass(testClass);
        classSubject.setSubject(testSubject);

        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(classSubjectRepository.findBySchoolClassOrderBySubject_NameAsc(testClass))
                .thenReturn(Arrays.asList(classSubject));

        classService.deleteClass(1L);

        verify(classSubjectRepository, times(1)).deleteAll(any(List.class));
        verify(classRepository, times(1)).delete(testClass);
    }

    @Test
    void getAllClasses_ShouldReturnList() {
        List<SchoolClass> classes = Arrays.asList(testClass);
        when(classRepository.findAllWithTeacher()).thenReturn(classes);

        List<ClassDTO> result = classService.getAllClasses();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getClassesByCategory_ShouldReturnFilteredList() {
        List<SchoolClass> classes = Arrays.asList(testClass);
        when(classRepository.findByCategoryWithTeacher(SchoolClass.ClassCategory.SENIOR_SECONDARY))
                .thenReturn(classes);

        List<ClassDTO> result = classService.getClassesByCategory("SENIOR_SECONDARY");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getClassesByCategory_WithInvalidCategory_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> classService.getClassesByCategory("INVALID"));
    }

    @Test
    void assignClassTeacher_ShouldAssignTeacher() {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(classRepository.save(any(SchoolClass.class))).thenReturn(testClass);

        SchoolClass result = classService.assignClassTeacher(1L, 1L);

        assertNotNull(result);
        assertEquals(testTeacher, result.getClassTeacher());
    }

    @Test
    void addSubject_ShouldAddSubjectToClass() {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        when(classSubjectRepository.findBySchoolClassAndSubject(testClass, testSubject))
                .thenReturn(Optional.empty());
        when(classSubjectRepository.save(any(ClassSubject.class))).thenReturn(new ClassSubject());
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));

        SchoolClass result = classService.addSubject(1L, "Mathematics");

        assertNotNull(result);
        verify(classSubjectRepository, times(1)).save(any(ClassSubject.class));
    }

    @Test
    void removeSubject_ShouldRemoveSubjectFromClass() {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        doNothing().when(classSubjectRepository).deleteBySchoolClassAndSubject(testClass, testSubject);
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));

        SchoolClass result = classService.removeSubject(1L, "Mathematics");

        assertNotNull(result);
        verify(classSubjectRepository, times(1)).deleteBySchoolClassAndSubject(testClass, testSubject);
    }

    @Test
    void getStudentsInClass_ShouldReturnStudents() {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByStudentClassAndClassArmNormalized("Grade 10", "A"))
                .thenReturn(Arrays.asList(testStudent));

        List<StudentResponseDTO> result = classService.getStudentsInClass(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Jane Doe", result.get(0).getFullName());
    }

    @Test
    void getClassStatistics_ShouldReturnStatistics() {
        List<SchoolClass> classes = Arrays.asList(testClass);
        when(classRepository.findAllWithTeacher()).thenReturn(classes);
        when(studentRepository.findByStudentClassAndClassArm("Grade 10", "A"))
                .thenReturn(Arrays.asList(testStudent));

        Map<String, Object> stats = classService.getClassStatistics();

        assertNotNull(stats);
        assertEquals(1, stats.get("totalClasses"));
        assertEquals(1, stats.get("totalStudents"));
        assertEquals(30, stats.get("totalCapacity"));
        assertEquals(29, stats.get("availableSeats"));
    }

    @Test
    void generateClassListPdf_ShouldGeneratePdf() throws Exception {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByStudentClassAndClassArm("Grade 10", "A"))
                .thenReturn(Arrays.asList(testStudent));

        byte[] pdf = classService.generateClassListPdf(1L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateClassListExcel_ShouldGenerateExcel() throws Exception {
        when(classRepository.findByIdWithTeacher(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByStudentClassAndClassArm("Grade 10", "A"))
                .thenReturn(Arrays.asList(testStudent));

        byte[] excel = classService.generateClassListExcel(1L);

        assertNotNull(excel);
        assertTrue(excel.length > 0);
    }
}