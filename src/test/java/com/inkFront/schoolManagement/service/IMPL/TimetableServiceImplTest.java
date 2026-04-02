// src/test/java/com/inkFront/schoolManagement/service/IMPL/TimetableServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.TimetableDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.Timetable;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.TimetableRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceImplTest {

    @Mock
    private TimetableRepository timetableRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private TimetableServiceImpl timetableService;

    private Timetable testTimetable;
    private TimetableDTO testDTO;
    private SchoolClass testClass;
    private Teacher testTeacher;
    private Student testStudent;
    private User testUser;

    @BeforeEach
    void setUp() {
        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");

        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setFirstName("John");
        testTeacher.setLastName("Smith");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Doe");
        testStudent.setSchoolClass(testClass);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("student");
        testUser.setEmail("student@example.com");
        testUser.setRole(User.Role.STUDENT);
        testUser.setStudent(testStudent);

        testTimetable = new Timetable();
        testTimetable.setId(1L);
        testTimetable.setSchoolClass(testClass);
        testTimetable.setTeacher(testTeacher);
        testTimetable.setSubject("Mathematics");
        testTimetable.setDayOfWeek(DayOfWeek.MONDAY);
        testTimetable.setStartTime(LocalTime.of(8, 0));
        testTimetable.setEndTime(LocalTime.of(9, 0));
        testTimetable.setRoom("Room 101");
        testTimetable.setSession("2023/2024");
        testTimetable.setTerm(Timetable.Term.FIRST);
        testTimetable.setActive(true);

        testDTO = new TimetableDTO();
        testDTO.setSchoolClassId(1L);
        testDTO.setTeacherId(1L);
        testDTO.setSubject("Mathematics");
        testDTO.setDayOfWeek("MONDAY");
        testDTO.setStartTime("08:00");
        testDTO.setEndTime("09:00");
        testDTO.setRoom("Room 101");
        testDTO.setSession("2023/2024");
        testDTO.setTerm("FIRST");
        testDTO.setActive(true);
    }

    @Test
    void createEntry_ShouldCreateTimetableEntry() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(timetableRepository.save(any(Timetable.class))).thenReturn(testTimetable);

        TimetableDTO result = timetableService.createEntry(testDTO);

        assertNotNull(result);
        assertEquals("Mathematics", result.getSubject());
        verify(timetableRepository).save(any(Timetable.class));
    }

    @Test
    void createEntry_WithInvalidTimes_ShouldThrowException() {
        testDTO.setEndTime("07:00");

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        assertThrows(RuntimeException.class, () -> timetableService.createEntry(testDTO));
    }

    @Test
    void updateEntry_ShouldUpdateTimetableEntry() {
        when(timetableRepository.findById(1L)).thenReturn(Optional.of(testTimetable));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(timetableRepository.save(any(Timetable.class))).thenReturn(testTimetable);

        TimetableDTO result = timetableService.updateEntry(1L, testDTO);

        assertNotNull(result);
        assertEquals("Mathematics", result.getSubject());
        verify(timetableRepository).save(any(Timetable.class));
    }

    @Test
    void updateEntry_WithNonExistentId_ShouldThrowException() {
        when(timetableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> timetableService.updateEntry(999L, testDTO));
    }

    @Test
    void getEntry_ShouldReturnTimetableEntry() {
        when(timetableRepository.findById(1L)).thenReturn(Optional.of(testTimetable));

        TimetableDTO result = timetableService.getEntry(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getEntry_WithNonExistentId_ShouldThrowException() {
        when(timetableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> timetableService.getEntry(999L));
    }

    @Test
    void deleteEntry_ShouldDeleteTimetableEntry() {
        doNothing().when(timetableRepository).deleteById(1L);

        timetableService.deleteEntry(1L);

        verify(timetableRepository).deleteById(1L);
    }

    @Test
    void getClassTimetable_ShouldReturnClassTimetable() {
        when(timetableRepository.findBySchoolClass_IdAndSessionAndTerm(
                1L, "2023/2024", Timetable.Term.FIRST
        )).thenReturn(List.of(testTimetable));

        List<TimetableDTO> result = timetableService.getClassTimetable(1L, "2023/2024", "FIRST");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getTeacherTimetable_ShouldReturnTeacherTimetable() {
        when(timetableRepository.findByTeacher_IdAndSessionAndTerm(
                1L, "2023/2024", Timetable.Term.FIRST
        )).thenReturn(List.of(testTimetable));

        List<TimetableDTO> result = timetableService.getTeacherTimetable(1L, "2023/2024", "FIRST");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getSchoolTimetable_ShouldReturnSchoolTimetable() {
        when(timetableRepository.findBySessionAndTerm(
                "2023/2024", Timetable.Term.FIRST
        )).thenReturn(List.of(testTimetable));

        List<TimetableDTO> result = timetableService.getSchoolTimetable("2023/2024", "FIRST");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void checkAvailability_WhenNoConflict_ShouldReturnTrue() {
        when(timetableRepository.existsByTeacher_IdAndSessionAndTermAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                1L,
                "2023/2024",
                Timetable.Term.FIRST,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(8, 0)
        )).thenReturn(false);

        boolean available = timetableService.checkAvailability(
                1L, "MONDAY", "08:00", "09:00", "2023/2024", "FIRST"
        );

        assertTrue(available);
    }

    @Test
    void getStudentOwnTimetable_ShouldReturnStudentTimetable() {
        when(userRepository.findByEmail("student")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(testUser));
        when(classRepository.findByClassNameAndArmNormalized("Grade 10", "A")).thenReturn(Optional.of(testClass));
        when(timetableRepository.findBySchoolClass_IdAndSessionAndTerm(
                1L, "2023/2024", Timetable.Term.FIRST
        )).thenReturn(List.of(testTimetable));

        List<TimetableDTO> result =
                timetableService.getStudentOwnTimetable("student", "2023/2024", "FIRST");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getStudentOwnTimetable_WhenUserHasNoStudent_ShouldThrowAccessDenied() {
        User userWithoutStudent = new User();
        userWithoutStudent.setId(5L);
        userWithoutStudent.setUsername("nostudent");
        userWithoutStudent.setEmail("nostudent@example.com");
        userWithoutStudent.setRole(User.Role.STUDENT);

        when(userRepository.findByEmail("nostudent")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nostudent")).thenReturn(Optional.of(userWithoutStudent));

        assertThrows(AccessDeniedException.class,
                () -> timetableService.getStudentOwnTimetable("nostudent", "2023/2024", "FIRST"));
    }
}