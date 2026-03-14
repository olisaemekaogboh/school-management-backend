// src/main/java/com/inkFront/schoolManagement/service/IMPL/ResultServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ResultRepository;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.SubjectRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResultServiceImpl implements ResultService {

    private final StudentRepository studentRepository;
    private final ResultRepository resultRepository;
    private final TermResultRepository termResultRepository;
    private final SessionResultRepository sessionResultRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;

    // ==================== ADD/UPDATE RESULT METHODS ====================

    @Override
    @Transactional
    public Result addOrUpdateResult(ResultRequestDTO request) {
        log.info("Adding/updating result using DTO for student: {}, subjectId: {}",
                request.getStudentId(), request.getSubjectId());

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with id: " + request.getStudentId()
                ));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject not found with id: " + request.getSubjectId()
                ));

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, request.getSession(), request.getTerm())
                .orElseGet(() -> {
                    TermResult newTermResult = new TermResult();
                    newTermResult.setStudent(student);
                    newTermResult.setSession(request.getSession());
                    newTermResult.setTerm(request.getTerm());
                    return termResultRepository.save(newTermResult);
                });

        Result result = resultRepository
                .findByStudentAndSubjectAndSessionAndTerm(
                        student,
                        subject,
                        request.getSession(),
                        request.getTerm()
                )
                .orElse(new Result());

        result.setTermResult(termResult);
        result.setStudent(student);
        result.setSubject(subject);
        result.setSession(request.getSession());
        result.setTerm(request.getTerm());

        result.setResumptionTest(Math.min(request.getResumptionTest() != null ? request.getResumptionTest() : 0.0, 5.0));
        result.setAssignments(Math.min(request.getAssignments() != null ? request.getAssignments() : 0.0, 10.0));
        result.setProject(Math.min(request.getProject() != null ? request.getProject() : 0.0, 10.0));
        result.setMidtermTest(Math.min(request.getMidtermTest() != null ? request.getMidtermTest() : 0.0, 10.0));
        result.setSecondTest(Math.min(request.getSecondTest() != null ? request.getSecondTest() : 0.0, 5.0));
        result.setExamination(Math.min(request.getExamination() != null ? request.getExamination() : 0.0, 60.0));

        Result savedResult = resultRepository.save(result);

        if (termResult.getSubjectResults() == null) {
            termResult.setSubjectResults(new java.util.ArrayList<>());
        }

        if (!termResult.getSubjectResults().contains(savedResult)) {
            termResult.addResult(savedResult);
            termResultRepository.save(termResult);
        }

        updateTermAverages(termResult);

        log.info("Result saved successfully with ID: {}", savedResult.getId());
        return savedResult;
    }
    @Override
    @Transactional
    public Result addOrUpdateResult(Long studentId, String subjectName, String session,
                                    Result.Term term, Map<String, Double> scores) {

        log.info("Adding/updating result for student: {}, subject: {}, session: {}, term: {}",
                studentId, subjectName, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Subject subject = resolveSubject(subjectName);

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, term)
                .orElseGet(() -> {
                    TermResult newTermResult = new TermResult();
                    newTermResult.setStudent(student);
                    newTermResult.setSession(session);
                    newTermResult.setTerm(term);
                    return termResultRepository.save(newTermResult);
                });

        Result result = resultRepository
                .findByStudentAndSubjectAndSessionAndTerm(student, subject, session, term)
                .orElse(new Result());

        result.setTermResult(termResult);
        result.setStudent(student);
        result.setSubject(subject);
        result.setSession(session);
        result.setTerm(term);

        result.setResumptionTest(Math.min(scores.getOrDefault("resumptionTest", 0.0), 5.0));
        result.setAssignments(Math.min(scores.getOrDefault("assignments", 0.0), 10.0));
        result.setProject(Math.min(scores.getOrDefault("project", 0.0), 10.0));
        result.setMidtermTest(Math.min(scores.getOrDefault("midtermTest", 0.0), 10.0));
        result.setSecondTest(Math.min(scores.getOrDefault("secondTest", 0.0), 5.0));
        result.setExamination(Math.min(scores.getOrDefault("examination", 0.0), 60.0));

        Result savedResult = resultRepository.save(result);

        if (termResult.getSubjectResults() == null) {
            termResult.setSubjectResults(new ArrayList<>());
        }

        if (!termResult.getSubjectResults().contains(savedResult)) {
            termResult.addResult(savedResult);
            termResultRepository.save(termResult);
        }

        updateTermAverages(termResult);

        log.info("Result saved successfully with ID: {}", savedResult.getId());
        return savedResult;
    }

    private Subject resolveSubject(String subjectName) {
        if (subjectName == null || subjectName.isBlank()) {
            throw new RuntimeException("Subject is required");
        }

        return subjectRepository.findByNameIgnoreCase(subjectName.trim())
                .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectName));
    }

    private void updateTermAverages(TermResult termResult) {
        List<Result> results = resultRepository.findByStudentAndSessionAndTerm(
                termResult.getStudent(),
                termResult.getSession(),
                termResult.getTerm());

        if (results.isEmpty()) return;

        double totalScore = results.stream().mapToDouble(Result::getTotal).sum();
        double average = totalScore / results.size();

        termResult.setTotalScore(totalScore);
        termResult.setAverage(average);

        termResultRepository.save(termResult);

        calculatePositions(termResult);
    }

    // ==================== GET RESULT METHODS ====================

    @Override
    public List<Result> getStudentResults(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return resultRepository.findByStudentAndSessionAndTerm(student, session, term);
    }

    // ==================== TERM RESULT CALCULATION ====================

    @Override
    public TermResult calculateTermResult(Long studentId, String session, Result.Term term) {
        log.info("Calculating term result for student: {}, session: {}, term: {}", studentId, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Result> subjectResults = resultRepository
                .findByStudentAndSessionAndTerm(student, session, term);

        if (subjectResults.isEmpty()) {
            throw new RuntimeException("No results found for this student in the specified term");
        }

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, term)
                .orElse(new TermResult());

        termResult.setStudent(student);
        termResult.setSession(session);
        termResult.setTerm(term);

        double totalScore = subjectResults.stream().mapToDouble(Result::getTotal).sum();
        double average = totalScore / subjectResults.size();

        termResult.setTotalScore(totalScore);
        termResult.setAverage(average);

        calculateTermAttendance(termResult);

        TermResult savedTermResult = termResultRepository.save(termResult);

        calculatePositions(savedTermResult);

        return termResultRepository.findById(savedTermResult.getId())
                .orElse(savedTermResult);
    }

    private void calculatePositions(TermResult termResult) {
        String className = termResult.getStudent().getStudentClass();
        String arm = termResult.getStudent().getClassArm();
        String session = termResult.getSession();
        Result.Term term = termResult.getTerm();

        log.info("Calculating positions for student {} in class {} arm {} session {} term {}",
                termResult.getStudent().getId(), className, arm, session, term);

        try {
            List<TermResult> classResults = termResultRepository
                    .findByStudent_StudentClassAndSessionAndTermOrderByAverageDesc(className, session, term);

            for (int i = 0; i < classResults.size(); i++) {
                TermResult tr = classResults.get(i);
                int position = i + 1;
                tr.setPositionInClass(position);

                if (tr.getId() != null && tr.getId().equals(termResult.getId())) {
                    termResult.setPositionInClass(position);
                }
            }

            if (arm != null && !arm.isBlank() && !"null".equalsIgnoreCase(arm)) {
                List<TermResult> armResults = termResultRepository
                        .findByStudent_StudentClassAndStudent_ClassArmAndSessionAndTermOrderByAverageDesc(
                                className, arm, session, term
                        );

                for (int i = 0; i < armResults.size(); i++) {
                    TermResult tr = armResults.get(i);
                    int position = i + 1;
                    tr.setPositionInArm(position);

                    if (tr.getId() != null && tr.getId().equals(termResult.getId())) {
                        termResult.setPositionInArm(position);
                    }
                }

                termResultRepository.saveAll(armResults);
            }

            List<Object[]> schoolRanking = resultRepository.getSchoolRanking(session, term);

            for (int i = 0; i < schoolRanking.size(); i++) {
                Object[] rank = schoolRanking.get(i);
                Student s = (Student) rank[0];

                if (s.getId().equals(termResult.getStudent().getId())) {
                    termResult.setPositionInSchool(i + 1);
                    break;
                }
            }

            termResultRepository.saveAll(classResults);
            termResultRepository.save(termResult);

        } catch (Exception e) {
            log.error("Error calculating positions: {}", e.getMessage(), e);
            termResult.setPositionInClass(1);
            termResult.setPositionInArm(1);
            termResult.setPositionInSchool(1);
            termResultRepository.save(termResult);
        }
    }

    @Override
    public SessionResult calculateSessionResult(Long studentId, String session) {
        log.info("Calculating session result for student: {}, session: {}", studentId, session);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult firstTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST)
                .orElse(null);

        TermResult secondTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND)
                .orElse(null);

        TermResult thirdTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD)
                .orElse(null);

        SessionResult sessionResult = sessionResultRepository
                .findByStudentAndSession(student, session)
                .orElse(new SessionResult());

        sessionResult.setStudent(student);
        sessionResult.setSession(session);

        sessionResult.setFirstTermTotal(firstTerm != null ? firstTerm.getTotalScore() : 0);
        sessionResult.setSecondTermTotal(secondTerm != null ? secondTerm.getTotalScore() : 0);
        sessionResult.setThirdTermTotal(thirdTerm != null ? thirdTerm.getTotalScore() : 0);

        sessionResult.setFirstTermAverage(firstTerm != null ? firstTerm.getAverage() : 0);
        sessionResult.setSecondTermAverage(secondTerm != null ? secondTerm.getAverage() : 0);
        sessionResult.setThirdTermAverage(thirdTerm != null ? thirdTerm.getAverage() : 0);

        sessionResult.setFirstTermPosition(firstTerm != null ? firstTerm.getPositionInClass() : null);
        sessionResult.setSecondTermPosition(secondTerm != null ? secondTerm.getPositionInClass() : null);
        sessionResult.setThirdTermPosition(thirdTerm != null ? thirdTerm.getPositionInClass() : null);

        calculateSessionAttendance(sessionResult);
        sessionResult.calculateAnnualAverage();
        calculateAnnualPositions(sessionResult);

        SessionResult savedResult = sessionResultRepository.save(sessionResult);
        log.info("Session result calculated for student: {}, annual average: {}",
                studentId, savedResult.getAnnualAverage());

        return savedResult;
    }

    private void calculateSessionAttendance(SessionResult sessionResult) {
        sessionResult.setTotalSchoolDays(270);
        sessionResult.setTotalDaysPresent(250);
        sessionResult.setTotalDaysAbsent(20);
        sessionResult.setAttendancePercentage(92.6);
    }

    private void calculateAnnualPositions(SessionResult sessionResult) {
        String className = sessionResult.getStudent().getStudentClass();
        String arm = sessionResult.getStudent().getClassArm();
        String session = sessionResult.getSession();

        log.info("Calculating annual positions for student {} in class {} arm {}",
                sessionResult.getStudent().getId(), className, arm);

        try {
            List<SessionResult> classResults = sessionResultRepository
                    .findByClassAndSessionOrderByAnnualAverageDesc(className, session);

            log.info("Found {} students in class for annual ranking", classResults.size());

            for (int i = 0; i < classResults.size(); i++) {
                SessionResult sr = classResults.get(i);
                int position = i + 1;
                sr.setAnnualPositionInClass(position);

                if (sr.getId() != null && sr.getId().equals(sessionResult.getId())) {
                    sessionResult.setAnnualPositionInClass(position);
                    log.info("Student annual position in class: {}", position);
                }
            }

            if (arm != null && !arm.isEmpty() && !"null".equalsIgnoreCase(arm)) {
                List<SessionResult> armResults = sessionResultRepository
                        .findByClassAndArmAndSessionOrderByAnnualAverageDesc(className, arm, session);

                log.info("Found {} students in arm for annual ranking", armResults.size());

                for (int i = 0; i < armResults.size(); i++) {
                    SessionResult sr = armResults.get(i);
                    int position = i + 1;
                    sr.setAnnualPositionInArm(position);

                    if (sr.getId() != null && sr.getId().equals(sessionResult.getId())) {
                        sessionResult.setAnnualPositionInArm(position);
                        log.info("Student annual position in arm: {}", position);
                    }
                }
            }

            List<SessionResult> schoolResults = sessionResultRepository
                    .findBySessionOrderByAnnualAverageDesc(session);

            log.info("Found {} students in school for annual ranking", schoolResults.size());

            for (int i = 0; i < schoolResults.size(); i++) {
                SessionResult sr = schoolResults.get(i);
                int position = i + 1;
                sr.setAnnualPositionInSchool(position);

                if (sr.getId() != null && sr.getId().equals(sessionResult.getId())) {
                    sessionResult.setAnnualPositionInSchool(position);
                    log.info("Student annual position in school: {}", position);
                }
            }

            sessionResultRepository.saveAll(classResults);
            log.info("Saved all class results with updated annual positions");

        } catch (Exception e) {
            log.error("Error calculating annual positions: {}", e.getMessage(), e);
            sessionResult.setAnnualPositionInClass(1);
            sessionResult.setAnnualPositionInArm(1);
            sessionResult.setAnnualPositionInSchool(1);
        }
    }

    // ==================== RANKING METHODS ====================

    @Override
    public Map<String, Object> getClassRankings(String className, String arm, String session, Result.Term term) {
        List<Object[]> rankings;

        if (arm != null && !arm.isBlank()) {
            rankings = resultRepository.getArmRankingNormalized(className, arm, session, term);
        } else {
            rankings = resultRepository.getClassRankingNormalized(className, session, term);
        }
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            Student student = (Student) rankings.get(i)[0];
            Double avgScore = (Double) rankings.get(i)[1];

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("average", avgScore);
            item.put("class", student.getStudentClass());
            item.put("arm", student.getClassArm());

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("className", className);
        response.put("arm", arm);
        response.put("session", session);
        response.put("term", term);
        response.put("totalStudents", resultList.size());
        response.put("rankings", resultList);

        return response;
    }

    @Override
    public Map<String, Object> getArmRankings(String className, String arm, String session, Result.Term term) {
        List<Object[]> rankings = resultRepository.getArmRankingNormalized(className, arm, session, term);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            Student student = (Student) rankings.get(i)[0];
            Double avgScore = (Double) rankings.get(i)[1];

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("average", avgScore);
            item.put("class", student.getStudentClass());
            item.put("arm", student.getClassArm());

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("className", className);
        response.put("arm", arm);
        response.put("session", session);
        response.put("term", term);
        response.put("totalStudents", resultList.size());
        response.put("rankings", resultList);

        return response;
    }

    @Override
    public Map<String, Object> getSchoolRankings(String session, Result.Term term) {
        List<Object[]> rankings = resultRepository.getSchoolRanking(session, term);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            Student student = (Student) rankings.get(i)[0];
            Double avgScore = (Double) rankings.get(i)[1];

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("average", avgScore);
            item.put("class", student.getStudentClass());
            item.put("arm", student.getClassArm());

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("session", session);
        response.put("term", term);
        response.put("totalStudents", resultList.size());
        response.put("rankings", resultList);

        return response;
    }

    // ==================== BULK CALCULATION METHODS ====================

    @Override
    public void calculateAllTermResults(String session, Result.Term term) {
        log.info("Calculating term results for all students in session: {}, term: {}", session, term);

        List<Student> allStudents = studentRepository.findAll();

        for (Student student : allStudents) {
            try {
                calculateTermResult(student.getId(), session, term);
            } catch (Exception e) {
                log.error("Error calculating term result for student {}: {}",
                        student.getId(), e.getMessage());
            }
        }

        log.info("Completed calculating term results for all students");
    }

    @Override
    public void calculateAllSessionResults(String session) {
        log.info("Calculating session results for all students in session: {}", session);

        List<Student> allStudents = studentRepository.findAll();

        for (Student student : allStudents) {
            try {
                calculateSessionResult(student.getId(), session);
            } catch (Exception e) {
                log.error("Error calculating session result for student {}: {}",
                        student.getId(), e.getMessage());
            }
        }

        log.info("Completed calculating session results for all students");
    }

    // ==================== RESULT SHEET GENERATION ====================

    @Override
    public Map<String, Object> generateResultSheet(Long studentId, String session, Result.Term term) {
        log.info("Generating result sheet for student: {}, session: {}, term: {}", studentId, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, term)
                .orElseThrow(() -> new RuntimeException("Term result not found for student: " + studentId));

        List<Result> results = resultRepository
                .findByStudentAndSessionAndTerm(student, session, term);

        String className = student.getStudentClass();
        List<Student> studentsInClass = studentRepository.findByStudentClass(className);
        int totalStudentsInClass = studentsInClass.size();

        int totalStudentsInArm = 0;
        String arm = student.getClassArm();
        if (arm != null && !arm.isEmpty()) {
            List<Student> studentsInArm = studentRepository.findByStudentClassAndClassArm(className, arm);
            totalStudentsInArm = studentsInArm.size();
        }

        Map<String, Object> resultSheet = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("name", student.getFirstName() + " " +
                (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                student.getLastName());
        studentInfo.put("fullName", student.getFirstName() + " " + student.getLastName());
        studentInfo.put("firstName", student.getFirstName());
        studentInfo.put("lastName", student.getLastName());
        studentInfo.put("middleName", student.getMiddleName());
        studentInfo.put("admissionNumber", student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "");
        studentInfo.put("class", student.getStudentClass() != null ? student.getStudentClass() : "");
        studentInfo.put("arm", student.getClassArm() != null ? student.getClassArm() : "");
        studentInfo.put("session", session != null ? session : "");
        studentInfo.put("term", term != null ? term.toString() : "");
        studentInfo.put("profilePictureUrl", student.getProfilePictureUrl());

        resultSheet.put("studentInfo", studentInfo);

        List<Map<String, Object>> subjectResults = results.stream()
                .map(r -> {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("subject", r.getSubject() != null ? r.getSubject().getName() : "");
                    subjectMap.put("resumptionTest", r.getResumptionTest());
                    subjectMap.put("assignments", r.getAssignments());
                    subjectMap.put("project", r.getProject());
                    subjectMap.put("midtermTest", r.getMidtermTest());
                    subjectMap.put("secondTest", r.getSecondTest());
                    subjectMap.put("continuousAssessment", r.getContinuousAssessment());
                    subjectMap.put("examination", r.getExamination());
                    subjectMap.put("total", r.getTotal());
                    subjectMap.put("grade", r.getGrade() != null ? r.getGrade() : "");
                    subjectMap.put("remarks", r.getRemarks() != null ? r.getRemarks() : "");
                    return subjectMap;
                })
                .collect(Collectors.toList());

        resultSheet.put("subjects", subjectResults);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalScore", termResult.getTotalScore());
        summary.put("average", termResult.getAverage());
        summary.put("positionInClass", termResult.getPositionInClass() != null ? termResult.getPositionInClass() : 0);
        summary.put("totalStudentsInClass", totalStudentsInClass);
        summary.put("positionInArm", termResult.getPositionInArm() != null ? termResult.getPositionInArm() : 0);
        summary.put("totalStudentsInArm", totalStudentsInArm);
        summary.put("positionInSchool", termResult.getPositionInSchool() != null ? termResult.getPositionInSchool() : 0);
        summary.put("totalSchoolDays", termResult.getTotalSchoolDays());
        summary.put("daysPresent", termResult.getDaysPresent());
        summary.put("daysAbsent", termResult.getDaysAbsent());
        summary.put("attendancePercentage", termResult.getAttendancePercentage());

        resultSheet.put("summary", summary);

        log.info("Result sheet generated successfully for student: {}", studentId);
        return resultSheet;
    }

    @Override
    public Map<String, Object> generateAnnualResultSheet(Long studentId, String session) {
        log.info("Generating annual result sheet for student: {}, session: {}", studentId, session);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        SessionResult sessionResult = sessionResultRepository
                .findByStudentAndSession(student, session)
                .orElseThrow(() -> new RuntimeException("Session result not found for student: " + studentId));

        TermResult firstTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST)
                .orElse(null);

        TermResult secondTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND)
                .orElse(null);

        TermResult thirdTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD)
                .orElse(null);

        Map<String, Object> resultSheet = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("name", student.getFirstName() + " " +
                (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                student.getLastName());
        studentInfo.put("fullName", student.getFirstName() + " " + student.getLastName());
        studentInfo.put("firstName", student.getFirstName());
        studentInfo.put("lastName", student.getLastName());
        studentInfo.put("admissionNumber", student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "");
        studentInfo.put("class", student.getStudentClass() != null ? student.getStudentClass() : "");
        studentInfo.put("arm", student.getClassArm() != null ? student.getClassArm() : "");
        studentInfo.put("session", session != null ? session : "");
        studentInfo.put("profilePictureUrl", student.getProfilePictureUrl());

        resultSheet.put("studentInfo", studentInfo);

        Map<String, Object> termSummaries = new HashMap<>();

        if (firstTerm != null) {
            Map<String, Object> firstTermMap = new HashMap<>();
            firstTermMap.put("total", firstTerm.getTotalScore());
            firstTermMap.put("average", firstTerm.getAverage());
            firstTermMap.put("position", firstTerm.getPositionInClass() != null ? firstTerm.getPositionInClass() : 0);
            termSummaries.put("firstTerm", firstTermMap);
        }

        if (secondTerm != null) {
            Map<String, Object> secondTermMap = new HashMap<>();
            secondTermMap.put("total", secondTerm.getTotalScore());
            secondTermMap.put("average", secondTerm.getAverage());
            secondTermMap.put("position", secondTerm.getPositionInClass() != null ? secondTerm.getPositionInClass() : 0);
            termSummaries.put("secondTerm", secondTermMap);
        }

        if (thirdTerm != null) {
            Map<String, Object> thirdTermMap = new HashMap<>();
            thirdTermMap.put("total", thirdTerm.getTotalScore());
            thirdTermMap.put("average", thirdTerm.getAverage());
            thirdTermMap.put("position", thirdTerm.getPositionInClass() != null ? thirdTerm.getPositionInClass() : 0);
            termSummaries.put("thirdTerm", thirdTermMap);
        }

        resultSheet.put("termResults", termSummaries);

        Map<String, Object> annualSummary = new HashMap<>();
        annualSummary.put("firstTermTotal", sessionResult.getFirstTermTotal());
        annualSummary.put("secondTermTotal", sessionResult.getSecondTermTotal());
        annualSummary.put("thirdTermTotal", sessionResult.getThirdTermTotal());
        annualSummary.put("annualTotal", sessionResult.getAnnualTotal());
        annualSummary.put("annualAverage", sessionResult.getAnnualAverage());
        annualSummary.put("positionInClass", sessionResult.getAnnualPositionInClass() != null ? sessionResult.getAnnualPositionInClass() : 0);
        annualSummary.put("positionInArm", sessionResult.getAnnualPositionInArm() != null ? sessionResult.getAnnualPositionInArm() : 0);
        annualSummary.put("positionInSchool", sessionResult.getAnnualPositionInSchool() != null ? sessionResult.getAnnualPositionInSchool() : 0);
        annualSummary.put("promoted", sessionResult.isPromoted());

        resultSheet.put("annualSummary", annualSummary);

        log.info("Annual result sheet generated successfully for student: {}", studentId);
        return resultSheet;
    }

    private void calculateTermAttendance(TermResult termResult) {
        List<Attendance> attendanceRecords = attendanceRepository
                .findByStudentAndSessionAndTermOrderByDateAsc(
                        termResult.getStudent(),
                        termResult.getSession(),
                        termResult.getTerm());

        if (attendanceRecords.isEmpty()) {
            termResult.setTotalSchoolDays(0);
            termResult.setDaysPresent(0);
            termResult.setDaysAbsent(0);
            termResult.setAttendancePercentage(0);
            return;
        }

        Set<LocalDate> uniqueDays = new HashSet<>();
        int present = 0;
        int absent = 0;

        for (Attendance attendance : attendanceRecords) {
            uniqueDays.add(attendance.getDate());
            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                present++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                absent++;
            }
        }

        termResult.setTotalSchoolDays(uniqueDays.size());
        termResult.setDaysPresent(present);
        termResult.setDaysAbsent(absent);
        termResult.setAttendancePercentage(uniqueDays.size() > 0
                ? (present * 100.0 / uniqueDays.size())
                : 0);
    }
}