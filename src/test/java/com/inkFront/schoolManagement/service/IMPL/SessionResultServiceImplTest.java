// src/test/java/com/inkFront/schoolManagement/service/IMPL/SessionResultServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ResultRepository;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionResultServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TermResultRepository termResultRepository;

    @Mock
    private SessionResultRepository sessionResultRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassRepository classRepository;

    @InjectMocks
    private SessionResultServiceImpl sessionResultService;

    private Student testStudent;
    private SchoolClass testClass;
    private TermResult firstTerm;
    private TermResult secondTerm;
    private TermResult thirdTerm;
    private SessionResult sessionResult;
    private Result subjectResult;

    @BeforeEach
    void setUp() {
        testClass = new SchoolClass();
        testClass.setId(1L);
        testClass.setClassName("JSS 1");
        testClass.setArm("A");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");
        testStudent.setSchoolClass(testClass);

        firstTerm = new TermResult();
        firstTerm.setId(1L);
        firstTerm.setStudent(testStudent);
        firstTerm.setSession("2025/2026");
        firstTerm.setTerm(Result.Term.FIRST);
        firstTerm.setTotalScore(250.0);
        firstTerm.setAverage(83.3);
        firstTerm.setPositionInClass(1);
        firstTerm.setDaysPresent(20);
        firstTerm.setDaysAbsent(2);
        firstTerm.setAttendancePercentage(90.9);

        secondTerm = new TermResult();
        secondTerm.setId(2L);
        secondTerm.setStudent(testStudent);
        secondTerm.setSession("2025/2026");
        secondTerm.setTerm(Result.Term.SECOND);
        secondTerm.setTotalScore(240.0);
        secondTerm.setAverage(80.0);
        secondTerm.setPositionInClass(2);
        secondTerm.setDaysPresent(18);
        secondTerm.setDaysAbsent(3);
        secondTerm.setAttendancePercentage(85.7);

        thirdTerm = new TermResult();
        thirdTerm.setId(3L);
        thirdTerm.setStudent(testStudent);
        thirdTerm.setSession("2025/2026");
        thirdTerm.setTerm(Result.Term.THIRD);
        thirdTerm.setTotalScore(255.0);
        thirdTerm.setAverage(85.0);
        thirdTerm.setPositionInClass(1);
        thirdTerm.setDaysPresent(22);
        thirdTerm.setDaysAbsent(1);
        thirdTerm.setAttendancePercentage(95.6);

        sessionResult = new SessionResult();
        sessionResult.setId(1L);
        sessionResult.setStudent(testStudent);
        sessionResult.setSession("2025/2026");

        Subject subject = Subject.builder().id(1L).name("Mathematics").code("MTH").build();
        subjectResult = new Result();
        subjectResult.setId(1L);
        subjectResult.setStudent(testStudent);
        subjectResult.setSubject(subject);
        subjectResult.setSession("2025/2026");
        subjectResult.setTerm(Result.Term.FIRST);
        subjectResult.setTotal(90.0);
    }

    @Test
    void calculateSessionResult_ShouldCalculateAndReturnDto() {
        Attendance attendance = new Attendance();
        attendance.setDate(LocalDate.now());
        attendance.setTerm(Result.Term.FIRST);
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);

        sessionResult.setId(1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(Optional.of(firstTerm));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(Optional.of(secondTerm));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(Optional.of(thirdTerm));

        when(sessionResultRepository.findDetailedByStudentAndSession(testStudent, "2025/2026"))
                .thenReturn(Optional.of(sessionResult));

        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(List.of(subjectResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of(subjectResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of(subjectResult));

        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(List.of(attendance));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of());
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of());

        when(sessionResultRepository.save(any(SessionResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(sessionResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(1L, "2025/2026"))
                .thenReturn(List.of(sessionResult));
        lenient().when(sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc("2025/2026"))
                .thenReturn(List.of(sessionResult));
        lenient().when(sessionResultRepository.findDetailedById(1L))
                .thenReturn(Optional.of(sessionResult));

        SessionResultResponseDTO result = sessionResultService.calculateSessionResult(1L, "2025/2026");

        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
    }
    @Test
    void calculateSessionResult_ShouldThrow_WhenStudentMissing() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sessionResultService.calculateSessionResult(1L, "2025/2026"));
    }

    @Test
    void calculateSessionResult_ShouldThrow_WhenNoTermResultsExist() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(Optional.empty());
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(Optional.empty());
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> sessionResultService.calculateSessionResult(1L, "2025/2026"));
    }

    @Test
    void calculateAllSessionResults_ShouldIterateStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));

        // 🔥 CRITICAL FIX: provide at least one term result
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(Optional.of(firstTerm)); // ✅ NOT empty

        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(Optional.empty());

        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(Optional.empty());

        when(sessionResultRepository.findDetailedByStudentAndSession(testStudent, "2025/2026"))
                .thenReturn(Optional.of(sessionResult));

        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(List.of(subjectResult));

        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of());

        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of());

        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(any(), any(), any()))
                .thenReturn(List.of());

        when(sessionResultRepository.save(any(SessionResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<SessionResultResponseDTO> results =
                sessionResultService.calculateAllSessionResults("2025/2026");

        assertNotNull(results);
        assertEquals(1, results.size());
    }
    @Test
    void calculateClassArmSessionResults_ShouldReturnEmpty_WhenNoStudents() {
        when(studentRepository.findByStudentClassAndClassArmNormalized("JSS 1", "A"))
                .thenReturn(List.of());

        List<SessionResultResponseDTO> results =
                sessionResultService.calculateClassArmSessionResults("JSS 1", "A", "2025/2026");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getSessionResult_ShouldReturnDto() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(sessionResultRepository.findDetailedByStudentAndSession(testStudent, "2025/2026"))
                .thenReturn(Optional.of(sessionResult));

        SessionResultResponseDTO result = sessionResultService.getSessionResult(1L, "2025/2026");

        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
    }

    @Test
    void getClassSessionResults_ShouldReturnList() {
        when(sessionResultRepository.findDetailedByClassAndSessionOrderByAnnualAverageDesc("JSS 1", "2025/2026"))
                .thenReturn(List.of(sessionResult));

        List<SessionResultResponseDTO> results =
                sessionResultService.getClassSessionResults("JSS 1", "2025/2026");

        assertEquals(1, results.size());
    }

    @Test
    void getArmSessionResults_ShouldReturnList() {
        when(sessionResultRepository.findDetailedByClassAndArmAndSessionOrderByAnnualAverageDesc("JSS 1", "A", "2025/2026"))
                .thenReturn(List.of(sessionResult));

        List<SessionResultResponseDTO> results =
                sessionResultService.getArmSessionResults("JSS 1", "A", "2025/2026");

        assertEquals(1, results.size());
    }

    @Test
    void getSchoolSessionRankings_ShouldReturnMap() {
        sessionResult.setAnnualAverage(82.5);
        sessionResult.setAnnualTotal(750.0);
        sessionResult.setAttendancePercentage(90.0);
        sessionResult.setPromoted(true);

        when(sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc("2025/2026"))
                .thenReturn(List.of(sessionResult));
        when(sessionResultRepository.countPromotedStudents("2025/2026")).thenReturn(1L);
        when(sessionResultRepository.countRetainedStudents("2025/2026")).thenReturn(0L);

        Map<String, Object> result = sessionResultService.getSchoolSessionRankings("2025/2026");

        assertNotNull(result);
        assertEquals(1, result.get("totalStudents"));
    }

    @Test
    void getClassRankings_ShouldReturnMap() {
        sessionResult.setAnnualAverage(82.5);
        sessionResult.setAttendancePercentage(90.0);
        sessionResult.setPromoted(true);

        when(sessionResultRepository.findDetailedByClassAndSessionOrderByAnnualAverageDesc("JSS 1", "2025/2026"))
                .thenReturn(List.of(sessionResult));

        Map<String, Object> result = sessionResultService.getClassRankings("JSS 1", "2025/2026");

        assertNotNull(result);
        assertEquals("JSS 1", result.get("className"));
    }

    @Test
    void getArmRankings_ShouldReturnMap() {
        sessionResult.setAnnualAverage(82.5);
        sessionResult.setAttendancePercentage(90.0);
        sessionResult.setPromoted(true);

        when(sessionResultRepository.findDetailedByClassAndArmAndSessionOrderByAnnualAverageDesc("JSS 1", "A", "2025/2026"))
                .thenReturn(List.of(sessionResult));

        Map<String, Object> result = sessionResultService.getArmRankings("JSS 1", "A", "2025/2026");

        assertNotNull(result);
        assertEquals("A", result.get("arm"));
    }

    @Test
    void getSessionStatistics_ShouldReturnStats() {
        sessionResult.setAnnualAverage(82.5);
        sessionResult.setAttendancePercentage(90.0);
        sessionResult.setPromoted(true);

        when(sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc("2025/2026"))
                .thenReturn(List.of(sessionResult));
        when(sessionResultRepository.getClassAverageBySession("2025/2026"))
                .thenReturn(Collections.singletonList(new Object[]{"JSS 1", 82.5}));
        when(sessionResultRepository.countPromotedStudents("2025/2026")).thenReturn(1L);
        when(sessionResultRepository.countRetainedStudents("2025/2026")).thenReturn(0L);

        Map<String, Object> result = sessionResultService.getSessionStatistics("2025/2026");

        assertNotNull(result);
        assertEquals(1, result.get("totalStudents"));
    }

    @Test
    void promoteStudents_ShouldPromoteOrRetain() {
        sessionResult.setAnnualAverage(82.5);
        sessionResult.setPromoted(true);

        when(sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc("2025/2026"))
                .thenReturn(List.of(sessionResult));
        when(classRepository.findByClassNameAndArmNormalized(anyString(), eq("A")))
                .thenReturn(Optional.of(testClass));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = sessionResultService.promoteStudents("2025/2026");

        assertNotNull(result);
        assertEquals("2025/2026", result.get("session"));
        verify(studentRepository).save(any(Student.class));
    }
    @Test
    void generateSessionReport_ShouldReturnReportMap() {
        sessionResult.setAnnualAverage(82.5);
        sessionResult.setAnnualTotal(745.0);
        sessionResult.setPromoted(true);

        Map<String, Double> averages = new HashMap<>();
        averages.put("Mathematics", 85.0);
        sessionResult.setSubjectAverages(averages);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(Optional.of(firstTerm));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(Optional.of(secondTerm));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(Optional.of(thirdTerm));
        when(sessionResultRepository.findDetailedByStudentAndSession(testStudent, "2025/2026"))
                .thenReturn(Optional.of(sessionResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(List.of(subjectResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of(subjectResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of(subjectResult));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(sessionResultRepository.save(any(SessionResult.class)))
                .thenAnswer(invocation -> {
                    SessionResult sr = invocation.getArgument(0);
                    if (sr.getId() == null) {
                        sr.setId(1L);
                    }
                    return sr;
                });

        lenient().when(sessionResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(1L, "2025/2026"))
                .thenReturn(List.of(sessionResult));
        lenient().when(sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc("2025/2026"))
                .thenReturn(List.of(sessionResult));

        Map<String, Object> report = sessionResultService.generateSessionReport(1L, "2025/2026");

        assertNotNull(report);
        assertTrue(report.containsKey("studentInfo"));
        assertTrue(report.containsKey("annualSummary"));
    }
}