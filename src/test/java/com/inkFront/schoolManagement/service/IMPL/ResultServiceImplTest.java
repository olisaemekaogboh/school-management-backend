// src/test/java/com/inkFront/schoolManagement/service/IMPL/ResultServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
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
import com.inkFront.schoolManagement.repository.SubjectRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private TermResultRepository termResultRepository;

    @Mock
    private SessionResultRepository sessionResultRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ClassRepository classRepository;

    @InjectMocks
    private ResultServiceImpl resultService;

    private Student testStudent;
    private Subject testSubject;
    private SchoolClass testClass;
    private TermResult testTermResult;
    private Result testResult;
    private ResultRequestDTO testRequest;

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

        testSubject = new Subject();
        testSubject.setId(1L);
        testSubject.setName("Mathematics");
        testSubject.setCode("MTH");

        testTermResult = new TermResult();
        testTermResult.setId(1L);
        testTermResult.setStudent(testStudent);
        testTermResult.setSession("2025/2026");
        testTermResult.setTerm(Result.Term.FIRST);

        testResult = new Result();
        testResult.setId(1L);
        testResult.setStudent(testStudent);
        testResult.setSubject(testSubject);
        testResult.setTermResult(testTermResult);
        testResult.setSession("2025/2026");
        testResult.setTerm(Result.Term.FIRST);
        testResult.setResumptionTest(5);
        testResult.setAssignments(10);
        testResult.setProject(10);
        testResult.setMidtermTest(10);
        testResult.setSecondTest(5);
        testResult.setExamination(55);
        testResult.setContinuousAssessment(40);
        testResult.setTotal(95);
        testResult.setGrade("A");
        testResult.setRemarks("Excellent");

        testRequest = ResultRequestDTO.builder()
                .studentId(1L)
                .subjectId(1L)
                .session("2025/2026")
                .term(Result.Term.FIRST)
                .resumptionTest(5.0)
                .assignments(10.0)
                .project(10.0)
                .midtermTest(10.0)
                .secondTest(5.0)
                .examination(55.0)
                .build();
    }

    @Test
    void addOrUpdateResult_ShouldCreateNewResult_WhenNoExistingResult() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.of(testTermResult));
        when(resultRepository.findDetailedByStudentAndSubjectAndSessionAndTerm(
                testStudent, testSubject, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.empty());
        when(resultRepository.save(any(Result.class))).thenReturn(testResult);
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testResult));
        when(termResultRepository.save(any(TermResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(termResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
                1L, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testTermResult));
        when(resultRepository.getSchoolRanking("2025/2026", Result.Term.FIRST))
                .thenReturn(Collections.singletonList(new Object[]{testStudent, 95.0}));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Collections.emptyList());

        Result result = resultService.addOrUpdateResult(testRequest);

        assertNotNull(result);
        assertEquals(1L, result.getStudent().getId());
        assertEquals(1L, result.getSubject().getId());
        verify(resultRepository).save(any(Result.class));
    }

    @Test
    void addOrUpdateResult_ShouldCreateTermResult_WhenMissing() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.empty());
        when(termResultRepository.save(any(TermResult.class))).thenReturn(testTermResult);
        when(resultRepository.findDetailedByStudentAndSubjectAndSessionAndTerm(
                testStudent, testSubject, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.empty());
        when(resultRepository.save(any(Result.class))).thenReturn(testResult);
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testResult));
        when(termResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
                1L, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testTermResult));
        when(resultRepository.getSchoolRanking("2025/2026", Result.Term.FIRST))
                .thenReturn(Collections.singletonList(new Object[]{testStudent, 95.0}));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Collections.emptyList());

        Result result = resultService.addOrUpdateResult(testRequest);

        assertNotNull(result);
        verify(termResultRepository, atLeastOnce()).save(any(TermResult.class));
    }

    @Test
    void addOrUpdateResult_ShouldThrow_WhenStudentMissing() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resultService.addOrUpdateResult(testRequest));
    }

    @Test
    void addOrUpdateResult_ShouldThrow_WhenSubjectMissing() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resultService.addOrUpdateResult(testRequest));
    }

    @Test
    void addOrUpdateResult_ShouldClampScoresToAllowedMaximums() {
        ResultRequestDTO request = ResultRequestDTO.builder()
                .studentId(1L)
                .subjectId(1L)
                .session("2025/2026")
                .term(Result.Term.FIRST)
                .resumptionTest(100.0)
                .assignments(100.0)
                .project(100.0)
                .midtermTest(100.0)
                .secondTest(100.0)
                .examination(100.0)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.of(testTermResult));
        when(resultRepository.findDetailedByStudentAndSubjectAndSessionAndTerm(
                testStudent, testSubject, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.empty());
        when(resultRepository.save(any(Result.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Collections.emptyList());

        resultService.addOrUpdateResult(request);

        ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);
        verify(resultRepository).save(captor.capture());
        Result saved = captor.getValue();

        assertEquals(5.0, saved.getResumptionTest());
        assertEquals(10.0, saved.getAssignments());
        assertEquals(10.0, saved.getProject());
        assertEquals(10.0, saved.getMidtermTest());
        assertEquals(5.0, saved.getSecondTest());
        assertEquals(60.0, saved.getExamination());
    }

    @Test
    void addOrUpdateResult_WithScoresMap_ShouldSaveResult() {
        Map<String, Double> scores = new HashMap<>();
        scores.put("resumptionTest", 4.0);
        scores.put("assignments", 8.0);
        scores.put("project", 7.0);
        scores.put("midtermTest", 9.0);
        scores.put("secondTest", 4.0);
        scores.put("examination", 50.0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findByNameIgnoreCase("Mathematics")).thenReturn(Optional.of(testSubject));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.of(testTermResult));
        when(resultRepository.findDetailedByStudentAndSubjectAndSessionAndTerm(
                testStudent, testSubject, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.empty());
        when(resultRepository.save(any(Result.class))).thenReturn(testResult);
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testResult));
        when(termResultRepository.save(any(TermResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(termResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
                1L, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testTermResult));
        when(resultRepository.getSchoolRanking("2025/2026", Result.Term.FIRST))
                .thenReturn(Collections.singletonList(new Object[]{testStudent, 95.0}));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Collections.emptyList());

        Result result = resultService.addOrUpdateResult(
                1L, "Mathematics", "2025/2026", Result.Term.FIRST, scores
        );

        assertNotNull(result);
        verify(resultRepository).save(any(Result.class));
    }

    @Test
    void getStudentResults_ShouldReturnResults() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testResult));

        List<Result> results = resultService.getStudentResults(1L, "2025/2026", Result.Term.FIRST);

        assertEquals(1, results.size());
    }

    @Test
    void calculateTermResult_ShouldCalculateAndSave() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testResult));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Optional.of(testTermResult));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of());
        when(termResultRepository.save(any(TermResult.class))).thenAnswer(invocation -> {
            TermResult tr = invocation.getArgument(0);
            if (tr.getId() == null) {
                tr.setId(1L);
            }
            return tr;
        });
        when(termResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
                1L, "2025/2026", Result.Term.FIRST
        )).thenReturn(List.of(testTermResult));
        when(resultRepository.getSchoolRanking("2025/2026", Result.Term.FIRST))
                .thenReturn(Collections.singletonList(new Object[]{testStudent, 95.0}));
        when(termResultRepository.findById(1L)).thenReturn(Optional.of(testTermResult));

        TermResult result = resultService.calculateTermResult(1L, "2025/2026", Result.Term.FIRST);

        assertNotNull(result);
        verify(termResultRepository, atLeastOnce()).save(any(TermResult.class));
    }

    @Test
    void calculateTermResult_ShouldThrow_WhenNoSubjectResults() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class,
                () -> resultService.calculateTermResult(1L, "2025/2026", Result.Term.FIRST));
    }

    @Test
    void calculateSessionResult_ShouldCalculateAndSave() {
        TermResult firstTerm = new TermResult();
        firstTerm.setId(1L);
        firstTerm.setStudent(testStudent);
        firstTerm.setSession("2025/2026");
        firstTerm.setTerm(Result.Term.FIRST);
        firstTerm.setTotalScore(250.0);
        firstTerm.setAverage(83.3);
        firstTerm.setPositionInClass(1);

        TermResult secondTerm = new TermResult();
        secondTerm.setId(2L);
        secondTerm.setStudent(testStudent);
        secondTerm.setSession("2025/2026");
        secondTerm.setTerm(Result.Term.SECOND);
        secondTerm.setTotalScore(240.0);
        secondTerm.setAverage(80.0);
        secondTerm.setPositionInClass(2);

        TermResult thirdTerm = new TermResult();
        thirdTerm.setId(3L);
        thirdTerm.setStudent(testStudent);
        thirdTerm.setSession("2025/2026");
        thirdTerm.setTerm(Result.Term.THIRD);
        thirdTerm.setTotalScore(255.0);
        thirdTerm.setAverage(85.0);
        thirdTerm.setPositionInClass(1);

        SessionResult sessionResult = new SessionResult();
        sessionResult.setId(1L);
        sessionResult.setStudent(testStudent);
        sessionResult.setSession("2025/2026");

        Attendance present = new Attendance();
        present.setDate(LocalDate.now());
        present.setTerm(Result.Term.FIRST);
        present.setStatus(Attendance.AttendanceStatus.PRESENT);

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
                .thenReturn(List.of(testResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of(testResult));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of(testResult));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(List.of(present));
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of());
        when(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of());
        when(sessionResultRepository.findDetailedByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(1L, "2025/2026"))
                .thenReturn(List.of(sessionResult));
        when(sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc("2025/2026"))
                .thenReturn(List.of(sessionResult));
        when(sessionResultRepository.save(any(SessionResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResult result = resultService.calculateSessionResult(1L, "2025/2026");

        assertNotNull(result);
        assertEquals(testStudent, result.getStudent());
        verify(sessionResultRepository, atLeastOnce()).save(any(SessionResult.class));
    }

    @Test
    void getClassRankings_ShouldReturnRankingMap() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(resultRepository.getClassRankingByClassId(1L, "2025/2026", Result.Term.FIRST))
                .thenReturn(Collections.singletonList(new Object[]{testStudent, 95.0}));
        Map<String, Object> result = resultService.getClassRankings(1L, "2025/2026", Result.Term.FIRST);

        assertNotNull(result);
        assertEquals(1L, result.get("classId"));
        assertEquals(1, ((List<?>) result.get("rankings")).size());
    }

    @Test
    void getSchoolRankings_ShouldReturnRankingMap() {
        when(resultRepository.getSchoolRanking("2025/2026", Result.Term.FIRST))
                .thenReturn(Collections.singletonList(new Object[]{testStudent, 95.0}));
        Map<String, Object> result = resultService.getSchoolRankings("2025/2026", Result.Term.FIRST);

        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("rankings")).size());
    }

    @Test
    void calculateAllTermResults_ShouldIterateStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(
                testStudent, "2025/2026", Result.Term.FIRST
        )).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> resultService.calculateAllTermResults("2025/2026", Result.Term.FIRST));
        verify(studentRepository).findAll();
    }

    @Test
    void calculateAllSessionResults_ShouldIterateStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(Optional.empty());
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(Optional.empty());
        when(termResultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(Optional.empty());
        when(sessionResultRepository.findDetailedByStudentAndSession(testStudent, "2025/2026"))
                .thenReturn(Optional.empty());
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.FIRST))
                .thenReturn(List.of());
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.SECOND))
                .thenReturn(List.of());
        when(resultRepository.findDetailedByStudentAndSessionAndTerm(testStudent, "2025/2026", Result.Term.THIRD))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> resultService.calculateAllSessionResults("2025/2026"));
        verify(studentRepository).findAll();
    }
}