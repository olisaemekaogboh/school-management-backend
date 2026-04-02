package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassRepository classRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Student testStudent;
    private SchoolClass testClass;
    private Attendance testAttendance;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");

        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("Grade 10");
        testClass.setArm("A");

        testStudent.setSchoolClass(testClass);

        testDate = LocalDate.of(2024, 1, 15);

        testAttendance = new Attendance();
        testAttendance.setId(1L);
        testAttendance.setStudent(testStudent);
        testAttendance.setDate(testDate);
        testAttendance.setSession("2023/2024");
        testAttendance.setTerm(Result.Term.FIRST);
        testAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);
    }

    @Test
    void markAttendance_ShouldCreateNewAttendance() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndDateAndSessionAndTerm(
                any(Student.class), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        Attendance result = attendanceService.markAttendance(
                1L, testDate, "2023/2024", Result.Term.FIRST,
                Attendance.AttendanceStatus.PRESENT, "On time"
        );

        assertNotNull(result);
        assertEquals(Attendance.AttendanceStatus.PRESENT, result.getStatus());
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    void markAttendance_ShouldUpdateExistingAttendance() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndDateAndSessionAndTerm(
                any(Student.class), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(Optional.of(testAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        Attendance result = attendanceService.markAttendance(
                1L, testDate, "2023/2024", Result.Term.FIRST,
                Attendance.AttendanceStatus.ABSENT, "Sick"
        );

        assertNotNull(result);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    void markAttendance_WithInvalidStudent_ShouldThrowException() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                attendanceService.markAttendance(
                        999L, testDate, "2023/2024",
                        Result.Term.FIRST, Attendance.AttendanceStatus.PRESENT, null
                )
        );
    }

    @Test
    void markAttendance_WithNullDate_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.markAttendance(
                        1L, null, "2023/2024",
                        Result.Term.FIRST, Attendance.AttendanceStatus.PRESENT, null
                )
        );
    }

    @Test
    void markAttendance_WithNullSession_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.markAttendance(
                        1L, testDate, null,
                        Result.Term.FIRST, Attendance.AttendanceStatus.PRESENT, null
                )
        );
    }

    @Test
    void markBulkAttendance_ShouldMarkMultipleStudents() {
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L);
        Student student2 = new Student();
        student2.setId(2L);
        Student student3 = new Student();
        student3.setId(3L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student3));
        when(attendanceRepository.findByStudentAndDateAndSessionAndTerm(
                any(Student.class), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        List<Attendance> results = attendanceService.markBulkAttendance(
                studentIds, testDate, "2023/2024",
                Result.Term.FIRST, Attendance.AttendanceStatus.PRESENT
        );

        assertNotNull(results);
        assertEquals(3, results.size());
        verify(attendanceRepository, times(3)).save(any(Attendance.class));
    }

    @Test
    void markBulkAttendance_WithEmptyList_ShouldReturnEmpty() {
        List<Attendance> results = attendanceService.markBulkAttendance(
                new ArrayList<>(), testDate, "2023/2024",
                Result.Term.FIRST, Attendance.AttendanceStatus.PRESENT
        );

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getStudentAttendance_ShouldReturnAttendance() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndDateAndSessionAndTerm(
                any(Student.class), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(Optional.of(testAttendance));

        Attendance result = attendanceService.getStudentAttendance(1L, testDate, "2023/2024", Result.Term.FIRST);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getStudentAttendance_WhenNotFound_ShouldReturnNull() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndDateAndSessionAndTerm(
                any(Student.class), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(Optional.empty());

        Attendance result = attendanceService.getStudentAttendance(1L, testDate, "2023/2024", Result.Term.FIRST);

        assertNull(result);
    }

    @Test
    void getStudentTermAttendance_ShouldReturnList() {
        List<Attendance> attendances = List.of(testAttendance);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), any(Result.Term.class)))
                .thenReturn(attendances);

        List<Attendance> result = attendanceService.getStudentTermAttendance(1L, "2023/2024", Result.Term.FIRST);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getStudentTermSummary_ShouldCalculateSummary() {
        List<Attendance> attendances = Arrays.asList(
                createAttendance(Attendance.AttendanceStatus.PRESENT),
                createAttendance(Attendance.AttendanceStatus.PRESENT),
                createAttendance(Attendance.AttendanceStatus.ABSENT),
                createAttendance(Attendance.AttendanceStatus.LATE),
                createAttendance(Attendance.AttendanceStatus.EXCUSED)
        );

        List<LocalDate> schoolDays = Arrays.asList(
                testDate, testDate.plusDays(1), testDate.plusDays(2),
                testDate.plusDays(3), testDate.plusDays(4)
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), any(Result.Term.class)))
                .thenReturn(attendances);
        when(attendanceRepository.findDistinctDatesBySessionAndTerm(anyString(), any(Result.Term.class)))
                .thenReturn(schoolDays);

        AttendanceSummary summary = attendanceService.getStudentTermSummary(1L, "2023/2024", Result.Term.FIRST);

        assertNotNull(summary);
        assertEquals(5, summary.getTotalSchoolDays());
        assertEquals(4, summary.getDaysPresent());
        assertEquals(1, summary.getDaysAbsent());
        assertEquals(1, summary.getDaysLate());
        assertEquals(1, summary.getDaysExcused());
        assertEquals(80.0, summary.getAttendancePercentage());
    }

    @Test
    void getStudentSessionSummary_ShouldReturnSessionSummary() {
        List<Attendance> firstTermAttendances = Arrays.asList(
                createAttendance(Attendance.AttendanceStatus.PRESENT),
                createAttendance(Attendance.AttendanceStatus.ABSENT),
                createAttendance(Attendance.AttendanceStatus.LATE)
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), eq(Result.Term.FIRST)))
                .thenReturn(firstTermAttendances);
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), eq(Result.Term.SECOND)))
                .thenReturn(new ArrayList<>());
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), eq(Result.Term.THIRD)))
                .thenReturn(new ArrayList<>());

        Map<String, Object> summary = attendanceService.getStudentSessionSummary(1L, "2023/2024");

        assertNotNull(summary);
        assertEquals(1L, summary.get("studentId"));
        assertEquals(1, summary.get("totalSchoolDays"));
    }

    @Test
    void getClassAttendance_ShouldReturnAttendance() {
        List<Student> students = List.of(testStudent);
        List<Attendance> existingAttendance = List.of(testAttendance);

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(1L)).thenReturn(students);
        when(attendanceRepository.findByStudent_SchoolClass_IdAndDateAndSessionAndTermOrderByStudent_LastNameAscStudent_FirstNameAsc(
                eq(1L), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(existingAttendance);

        List<Attendance> result = attendanceService.getClassAttendance(1L, testDate, "2023/2024", Result.Term.FIRST);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getClassTermStatistics_ShouldReturnStatistics() {
        List<Student> students = List.of(testStudent);
        List<LocalDate> schoolDays = Arrays.asList(testDate, testDate.plusDays(1));

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(attendanceRepository.findDistinctDatesBySessionAndTerm(anyString(), any(Result.Term.class)))
                .thenReturn(schoolDays);
        when(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(1L)).thenReturn(students);
        when(attendanceRepository.countByClassIdAndSessionAndTermAndStatus(
                eq(1L), anyString(), any(Result.Term.class), any(Attendance.AttendanceStatus.class)))
                .thenReturn(1L);
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), any(Result.Term.class)))
                .thenReturn(Arrays.asList(
                        createAttendance(Attendance.AttendanceStatus.PRESENT),
                        createAttendance(Attendance.AttendanceStatus.PRESENT)
                ));

        Map<String, Object> stats = attendanceService.getClassTermStatistics(1L, "2023/2024", Result.Term.FIRST);

        assertNotNull(stats);
        assertEquals(1L, stats.get("classId"));
        assertEquals(1, stats.get("totalStudents"));
        assertEquals(2, stats.get("totalSchoolDays"));
    }

    @Test
    void getSchoolAttendanceStatistics_ShouldReturnStatistics() {
        List<Student> students = List.of(testStudent);
        List<LocalDate> schoolDays = Arrays.asList(testDate, testDate.plusDays(1));

        when(attendanceRepository.findDistinctDatesBySessionAndTerm(anyString(), any(Result.Term.class)))
                .thenReturn(schoolDays);
        when(studentRepository.findAll()).thenReturn(students);
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), any(Result.Term.class)))
                .thenReturn(Arrays.asList(
                        createAttendance(Attendance.AttendanceStatus.PRESENT),
                        createAttendance(Attendance.AttendanceStatus.PRESENT)
                ));

        Map<String, Object> stats = attendanceService.getSchoolAttendanceStatistics("2023/2024", Result.Term.FIRST);

        assertNotNull(stats);
        assertEquals(1, stats.get("totalStudents"));
        assertEquals(2, stats.get("totalSchoolDays"));
    }

    @Test
    void initializeSchoolDays_ShouldCreatePlaceholderAttendance() {
        List<LocalDate> dates = Arrays.asList(testDate, testDate.plusDays(1));
        List<Student> students = List.of(testStudent);

        when(studentRepository.findAll()).thenReturn(students);
        when(attendanceRepository.findByStudentAndDateAndSessionAndTerm(
                any(Student.class), any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        List<Attendance> results = attendanceService.initializeSchoolDays(dates, "2023/2024", Result.Term.FIRST);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
    }

    @Test
    void getSchoolAttendanceStatisticsForDate_ShouldReturnStats() {
        when(attendanceRepository.countByDateAndSessionAndTermAndStatus(
                any(LocalDate.class), anyString(), any(Result.Term.class), any(Attendance.AttendanceStatus.class)))
                .thenReturn(5L);
        when(attendanceRepository.countDistinctStudentsMarkedByDateAndSessionAndTerm(
                any(LocalDate.class), anyString(), any(Result.Term.class)))
                .thenReturn(10L);

        Map<String, Object> stats = attendanceService.getSchoolAttendanceStatisticsForDate(
                testDate, "2023/2024", Result.Term.FIRST
        );

        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        verify(attendanceRepository, atLeastOnce()).countByDateAndSessionAndTermAndStatus(
                any(LocalDate.class), anyString(), any(Result.Term.class), any(Attendance.AttendanceStatus.class));
        verify(attendanceRepository, times(1)).countDistinctStudentsMarkedByDateAndSessionAndTerm(
                testDate, "2023/2024", Result.Term.FIRST);
    }

    @Test
    void calculateAllTermSummaries_ShouldCalculateForAllStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                any(Student.class), anyString(), any(Result.Term.class)))
                .thenReturn(List.of(createAttendance(Attendance.AttendanceStatus.PRESENT)));
        when(attendanceRepository.findDistinctDatesBySessionAndTerm(anyString(), any(Result.Term.class)))
                .thenReturn(List.of(testDate));

        assertDoesNotThrow(() ->
                attendanceService.calculateAllTermSummaries("2023/2024", Result.Term.FIRST)
        );
    }

    private Attendance createAttendance(Attendance.AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setStudent(testStudent);
        attendance.setDate(testDate);
        attendance.setSession("2023/2024");
        attendance.setTerm(Result.Term.FIRST);
        attendance.setStatus(status);
        return attendance;
    }
}