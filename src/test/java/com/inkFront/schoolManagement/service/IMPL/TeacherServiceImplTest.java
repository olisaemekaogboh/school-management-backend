// src/test/java/com/inkFront/schoolManagement/service/IMPL/TeacherServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.TeacherDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.TeacherInvitationRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceImplTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherInvitationRepository teacherInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private TeacherServiceImpl teacherService;

    private Teacher testTeacher;
    private TeacherDTO testTeacherDTO;

    @BeforeEach
    void setUp() {
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Doe");
        testTeacher.setEmail("john@example.com");
        testTeacher.setTeacherId("TCH260001");
        testTeacher.setEmployeeId("EMP260001");
        testTeacher.setDepartment("Science");
        testTeacher.setStatus(Teacher.TeacherStatus.ACTIVE);
        testTeacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        testTeacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
        testTeacher.setSubjects(new HashSet<>(List.of("Mathematics")));
        testTeacher.setQualifications(new HashSet<>(List.of("B.Ed")));

        testTeacherDTO = TeacherDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .teacherId("TCH260001")
                .employeeId("EMP260001")
                .department("Science")
                .status("ACTIVE")
                .employmentStatus("ACTIVE")
                .employmentType("FULL_TIME")
                .subjects(List.of("Mathematics"))
                .qualifications(List.of("B.Ed"))
                .build();
    }

    @Test
    void getTeacherByTeacherId_ShouldReturnTeacher() {
        when(teacherRepository.findByTeacherIdWithDetails("TCH260001")).thenReturn(Optional.of(testTeacher));

        TeacherDTO result = teacherService.getTeacherByTeacherId("TCH260001");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getAllTeachers_ShouldReturnList() {
        when(teacherRepository.findAll()).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getAllTeachers();

        assertEquals(1, result.size());
    }

    @Test
    void getAllTeachersPaginated_ShouldReturnPage() {
        when(teacherRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(testTeacher)));

        var result = teacherService.getAllTeachersPaginated(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTeacherDTO_ShouldReturnTeacher() {
        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testTeacher));

        TeacherDTO result = teacherService.getTeacherDTO(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getTeacher_ShouldReturnEntity() {
        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testTeacher));

        Teacher result = teacherService.getTeacher(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void createTeacher_ShouldCreateTeacherWithoutFile() {
        Teacher saved = new Teacher();
        saved.setId(1L);
        saved.setFirstName("John");
        saved.setLastName("Doe");
        saved.setEmail("john@example.com");
        saved.setTeacherId("TCH260001");
        saved.setEmployeeId("EMP260001");
        saved.setSubjects(new HashSet<>());
        saved.setQualifications(new HashSet<>());
        saved.setStatus(Teacher.TeacherStatus.ACTIVE);
        saved.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
        saved.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);

        TeacherDTO input = TeacherDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status("ACTIVE")
                .employmentType("FULL_TIME")
                .employmentStatus("ACTIVE")
                .subjects(List.of())
                .qualifications(List.of())
                .build();

        when(teacherRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(teacherRepository.findAllTeacherIds()).thenReturn(List.of("TCH260000"));
        when(teacherRepository.findAllEmployeeIds()).thenReturn(List.of("EMP260000"));
        when(teacherRepository.existsByTeacherId(anyString())).thenReturn(false);
        when(teacherRepository.existsByEmployeeId(anyString())).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(saved);

        TeacherDTO result = teacherService.createTeacher(input, null);

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void createTeacher_ShouldStoreProfilePicture_WhenProvided() {
        Teacher saved = new Teacher();
        saved.setId(1L);
        saved.setFirstName("John");
        saved.setLastName("Doe");
        saved.setEmail("john@example.com");
        saved.setTeacherId("TCH260001");
        saved.setEmployeeId("EMP260001");
        saved.setProfilePictureUrl("/uploads/teacher.jpg");
        saved.setSubjects(new HashSet<>());
        saved.setQualifications(new HashSet<>());
        saved.setStatus(Teacher.TeacherStatus.ACTIVE);
        saved.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
        saved.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);

        TeacherDTO input = TeacherDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status("ACTIVE")
                .employmentType("FULL_TIME")
                .employmentStatus("ACTIVE")
                .subjects(List.of())
                .qualifications(List.of())
                .build();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(fileStorageService.storeFile(multipartFile)).thenReturn("/uploads/teacher.jpg");
        when(teacherRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(teacherRepository.findAllTeacherIds()).thenReturn(List.of("TCH260000"));
        when(teacherRepository.findAllEmployeeIds()).thenReturn(List.of("EMP260000"));
        when(teacherRepository.existsByTeacherId(anyString())).thenReturn(false);
        when(teacherRepository.existsByEmployeeId(anyString())).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(saved);

        TeacherDTO result = teacherService.createTeacher(input, multipartFile);

        assertNotNull(result);
        verify(fileStorageService).storeFile(multipartFile);
    }

    @Test
    void createTeacher_ShouldThrow_WhenEmailExists() {
        when(teacherRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> teacherService.createTeacher(testTeacherDTO, null));
    }

    @Test
    void updateTeacher_ShouldUpdateTeacher() {
        Teacher existing = new Teacher();
        existing.setId(1L);
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setEmail("john@example.com");
        existing.setTeacherId("TCH260001");
        existing.setEmployeeId("EMP260001");
        existing.setSubjects(new HashSet<>(List.of("Math")));
        existing.setQualifications(new HashSet<>(List.of("B.Ed")));
        existing.setStatus(Teacher.TeacherStatus.ACTIVE);
        existing.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        existing.setEmploymentType(Teacher.EmploymentType.FULL_TIME);

        Teacher updated = new Teacher();
        updated.setId(1L);
        updated.setFirstName("Johnny");
        updated.setLastName("Doe");
        updated.setEmail("johnny@example.com");
        updated.setTeacherId("TCH260001");
        updated.setEmployeeId("EMP260001");
        updated.setSubjects(new HashSet<>(List.of("Physics")));
        updated.setQualifications(new HashSet<>(List.of("M.Ed")));
        updated.setStatus(Teacher.TeacherStatus.ACTIVE);
        updated.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        updated.setEmploymentType(Teacher.EmploymentType.FULL_TIME);

        TeacherDTO input = TeacherDTO.builder()
                .firstName("Johnny")
                .lastName("Doe")
                .email("johnny@example.com")
                .teacherId("TCH260001")
                .employeeId("EMP260001")
                .subjects(List.of("Physics"))
                .qualifications(List.of("M.Ed"))
                .status("ACTIVE")
                .employmentStatus("ACTIVE")
                .employmentType("FULL_TIME")
                .build();

        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.existsByEmail("johnny@example.com")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(updated);

        TeacherDTO result = teacherService.updateTeacher(1L, input, null);

        assertNotNull(result);
        assertEquals("johnny@example.com", result.getEmail());
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void updateTeacher_ShouldThrow_WhenTeacherMissing() {
        when(teacherRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.updateTeacher(99L, testTeacherDTO, null));
    }

    @Test
    void deleteTeacher_ShouldDeleteTeacher() {
        User linkedUser = new User();
        linkedUser.setId(2L);
        linkedUser.setTeacher(testTeacher);
        testTeacher.setUser(linkedUser);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(classRepository.clearTeacherFromClasses(1L)).thenReturn(1);
        doNothing().when(teacherRepository).delete(testTeacher);

        teacherService.deleteTeacher(1L);

        verify(classRepository).clearTeacherFromClasses(1L);
        verify(teacherRepository).delete(testTeacher);
        assertNull(testTeacher.getUser());
    }

    @Test
    void searchTeachers_ShouldReturnMatches() {
        when(teacherRepository.searchTeachers("john")).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.searchTeachers("john");

        assertEquals(1, result.size());
    }

    @Test
    void getTeachersByStatus_ShouldReturnMatches() {
        when(teacherRepository.findByStatus(Teacher.TeacherStatus.ACTIVE)).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getTeachersByStatus("ACTIVE");

        assertEquals(1, result.size());
    }

    @Test
    void getTeachersBySubject_ShouldReturnMatches() {
        when(teacherRepository.findBySubjectsContaining("Mathematics")).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getTeachersBySubject("Mathematics");

        assertEquals(1, result.size());
    }

    @Test
    void getTeachersByDepartment_ShouldReturnMatches() {
        when(teacherRepository.findByDepartment("Science")).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getTeachersByDepartment("Science");

        assertEquals(1, result.size());
    }

    @Test
    void getRecentTeachers_ShouldReturnMatches() {
        when(teacherRepository.findTeachersSince(any(LocalDateTime.class))).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getRecentTeachers(30);

        assertEquals(1, result.size());
    }

    @Test
    void getTeachersWithoutUserAccount_ShouldReturnMatches() {
        when(teacherRepository.findTeachersWithoutUserAccount()).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getTeachersWithoutUserAccount();

        assertEquals(1, result.size());
    }

    @Test
    void getTeachersByDateRange_ShouldReturnMatches() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        when(teacherRepository.findByCreatedAtBetween(start, end)).thenReturn(List.of(testTeacher));

        List<TeacherDTO> result = teacherService.getTeachersByDateRange(start, end);

        assertEquals(1, result.size());
    }

    @Test
    void addSubject_ShouldAddWhenMissing() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setSubjects(new HashSet<>());
        teacher.setQualifications(new HashSet<>());

        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherDTO result = teacherService.addSubject(1L, "Mathematics");

        assertNotNull(result);
        assertTrue(teacher.getSubjects().contains("Mathematics"));
        verify(teacherRepository).save(teacher);
    }

    @Test
    void removeSubject_ShouldRemoveWhenPresent() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setSubjects(new HashSet<>(List.of("Mathematics")));
        teacher.setQualifications(new HashSet<>());

        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherDTO result = teacherService.removeSubject(1L, "Mathematics");

        assertNotNull(result);
        assertFalse(teacher.getSubjects().contains("Mathematics"));
    }

    @Test
    void addQualification_ShouldAddWhenMissing() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setSubjects(new HashSet<>());
        teacher.setQualifications(new HashSet<>());

        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherDTO result = teacherService.addQualification(1L, "M.Ed");

        assertNotNull(result);
        assertTrue(teacher.getQualifications().contains("M.Ed"));
    }

    @Test
    void updateEmploymentStatus_ShouldUpdate() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        teacher.setSubjects(new HashSet<>());
        teacher.setQualifications(new HashSet<>());

        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherDTO result = teacherService.updateEmploymentStatus(1L, "ON_LEAVE");

        assertNotNull(result);
        assertEquals(Teacher.EmploymentStatus.ON_LEAVE, teacher.getEmploymentStatus());
    }

    @Test
    void updateEmploymentStatus_ShouldThrowOnInvalidStatus() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        teacher.setSubjects(new HashSet<>());
        teacher.setQualifications(new HashSet<>());

        when(teacherRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(teacher));

        assertThrows(BusinessException.class, () -> teacherService.updateEmploymentStatus(1L, "BAD_STATUS"));
    }

    @Test
    void getTeacherStatistics_ShouldReturnMap() {
        when(teacherRepository.count()).thenReturn(10L);
        when(teacherRepository.countByStatus(Teacher.TeacherStatus.ACTIVE)).thenReturn(6L);
        when(teacherRepository.countByStatus(Teacher.TeacherStatus.INACTIVE)).thenReturn(2L);
        when(teacherRepository.countByStatus(Teacher.TeacherStatus.ON_LEAVE)).thenReturn(1L);
        when(teacherRepository.countByStatus(Teacher.TeacherStatus.TERMINATED)).thenReturn(1L);
        when(teacherRepository.findTeachersWithoutUserAccount()).thenReturn(List.of(testTeacher));
        when(teacherInvitationRepository.countByUsedFalseAndExpiryDateAfter(any(LocalDateTime.class))).thenReturn(3L);
        when(teacherRepository.countByDepartment(anyString())).thenReturn(1L);
        when(teacherRepository.findByEmploymentType(any())).thenReturn(List.of(testTeacher));

        var result = teacherService.getTeacherStatistics();

        assertNotNull(result);
        assertEquals(10L, result.get("totalTeachers"));
    }

    @Test
    void generateTeacherId_ShouldReturnGeneratedValue() {
        when(teacherRepository.findAllTeacherIds()).thenReturn(List.of("TCH260001"));
        when(teacherRepository.existsByTeacherId(anyString())).thenReturn(false);

        String result = teacherService.generateTeacherId();

        assertNotNull(result);
        assertTrue(result.startsWith("TCH"));
    }

    @Test
    void checkEmailExists_ShouldReturnTrue() {
        when(teacherRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertTrue(teacherService.checkEmailExists("john@example.com"));
    }

    @Test
    void checkTeacherIdExists_ShouldReturnTrue() {
        when(teacherRepository.existsByTeacherId("TCH260001")).thenReturn(true);

        assertTrue(teacherService.checkTeacherIdExists("TCH260001"));
    }

    @Test
    void getTotalTeacherCount_ShouldReturnCount() {
        when(teacherRepository.count()).thenReturn(10L);

        assertEquals(10L, teacherService.getTotalTeacherCount());
    }

    @Test
    void getActiveTeacherCount_ShouldReturnCount() {
        when(teacherRepository.countByStatus(Teacher.TeacherStatus.ACTIVE)).thenReturn(6L);

        assertEquals(6L, teacherService.getActiveTeacherCount());
    }
}