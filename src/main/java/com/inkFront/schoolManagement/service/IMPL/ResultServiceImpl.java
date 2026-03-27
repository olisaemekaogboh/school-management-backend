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
    private final ClassRepository classRepository;

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

        if (results.isEmpty()) {
            return;
        }

        double totalScore = results.stream().mapToDouble(Result::getTotal).sum();
        double average = totalScore / results.size();

        termResult.setTotalScore(totalScore);
        termResult.setAverage(average);

        calculateTermAttendance(termResult);
        termResultRepository.save(termResult);

        calculatePositions(termResult);
    }

    @Override
    public List<Result> getStudentResults(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return resultRepository.findByStudentAndSessionAndTerm(student, session, term);
    }

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
        Student student = termResult.getStudent();
        String session = termResult.getSession();
        Result.Term term = termResult.getTerm();

        if (student == null || student.getSchoolClass() == null) {
            termResult.setPositionInClass(1);
            termResult.setPositionInArm(1);
            termResult.setPositionInSchool(1);
            termResultRepository.save(termResult);
            return;
        }

        Long classId = student.getSchoolClass().getId();

        try {
            List<TermResult> classResults = termResultRepository
                    .findByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
                            classId, session, term
                    );

            for (int i = 0; i < classResults.size(); i++) {
                TermResult tr = classResults.get(i);
                int position = i + 1;
                tr.setPositionInClass(position);
                tr.setPositionInArm(position);

                if (tr.getId() != null && tr.getId().equals(termResult.getId())) {
                    termResult.setPositionInClass(position);
                    termResult.setPositionInArm(position);
                }
            }

            List<Object[]> schoolRanking = resultRepository.getSchoolRanking(session, term);

            for (int i = 0; i < schoolRanking.size(); i++) {
                Object[] rank = schoolRanking.get(i);
                Student rankedStudent = (Student) rank[0];

                if (rankedStudent.getId().equals(student.getId())) {
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

        double firstTermTotal = firstTerm != null ? safeDouble(firstTerm.getTotalScore()) : 0.0;
        double secondTermTotal = secondTerm != null ? safeDouble(secondTerm.getTotalScore()) : 0.0;
        double thirdTermTotal = thirdTerm != null ? safeDouble(thirdTerm.getTotalScore()) : 0.0;

        double firstTermAverage = firstTerm != null ? safeDouble(firstTerm.getAverage()) : 0.0;
        double secondTermAverage = secondTerm != null ? safeDouble(secondTerm.getAverage()) : 0.0;
        double thirdTermAverage = thirdTerm != null ? safeDouble(thirdTerm.getAverage()) : 0.0;

        sessionResult.setFirstTermTotal(firstTermTotal);
        sessionResult.setSecondTermTotal(secondTermTotal);
        sessionResult.setThirdTermTotal(thirdTermTotal);

        sessionResult.setFirstTermAverage(firstTermAverage);
        sessionResult.setSecondTermAverage(secondTermAverage);
        sessionResult.setThirdTermAverage(thirdTermAverage);

        sessionResult.setFirstTermPosition(firstTerm != null ? firstTerm.getPositionInClass() : null);
        sessionResult.setSecondTermPosition(secondTerm != null ? secondTerm.getPositionInClass() : null);
        sessionResult.setThirdTermPosition(thirdTerm != null ? thirdTerm.getPositionInClass() : null);

        Map<String, Double> firstTermSubjectScores = new HashMap<>();
        Map<String, Double> secondTermSubjectScores = new HashMap<>();
        Map<String, Double> thirdTermSubjectScores = new HashMap<>();
        Map<String, Double> subjectAnnualTotals = new HashMap<>();
        Map<String, Double> subjectAverages = new HashMap<>();

        List<Result> firstTermResults =
                resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);
        List<Result> secondTermResults =
                resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);
        List<Result> thirdTermResults =
                resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

        for (Result result : firstTermResults) {
            if (result.getSubject() != null && result.getSubject().getName() != null) {
                String subjectName = result.getSubject().getName();
                double total = safeDouble(result.getTotal());
                firstTermSubjectScores.put(subjectName, total);
                subjectAnnualTotals.put(subjectName, subjectAnnualTotals.getOrDefault(subjectName, 0.0) + total);
            }
        }

        for (Result result : secondTermResults) {
            if (result.getSubject() != null && result.getSubject().getName() != null) {
                String subjectName = result.getSubject().getName();
                double total = safeDouble(result.getTotal());
                secondTermSubjectScores.put(subjectName, total);
                subjectAnnualTotals.put(subjectName, subjectAnnualTotals.getOrDefault(subjectName, 0.0) + total);
            }
        }

        for (Result result : thirdTermResults) {
            if (result.getSubject() != null && result.getSubject().getName() != null) {
                String subjectName = result.getSubject().getName();
                double total = safeDouble(result.getTotal());
                thirdTermSubjectScores.put(subjectName, total);
                subjectAnnualTotals.put(subjectName, subjectAnnualTotals.getOrDefault(subjectName, 0.0) + total);
            }
        }

        for (Map.Entry<String, Double> entry : subjectAnnualTotals.entrySet()) {
            subjectAverages.put(entry.getKey(), entry.getValue() / 3.0);
        }

        sessionResult.setFirstTermSubjectScores(firstTermSubjectScores);
        sessionResult.setSecondTermSubjectScores(secondTermSubjectScores);
        sessionResult.setThirdTermSubjectScores(thirdTermSubjectScores);
        sessionResult.setSubjectAnnualTotals(subjectAnnualTotals);
        sessionResult.setSubjectAverages(subjectAverages);

        calculateSessionAttendance(sessionResult);

        // IMPORTANT: compute annual values here directly
        sessionResult.setAnnualTotal(firstTermTotal + secondTermTotal + thirdTermTotal);
        sessionResult.setAnnualAverage((firstTermAverage + secondTermAverage + thirdTermAverage) / 3.0);

        boolean promoted = sessionResult.getAnnualAverage() >= 40.0;
        sessionResult.setPromoted(promoted);
        sessionResult.setPromotionRemark(promoted ? "Promoted to next class" : "Not promoted");

        calculateAnnualPositions(sessionResult);

        SessionResult savedResult = sessionResultRepository.save(sessionResult);
        log.info("Session result calculated for student: {}, annual average: {}",
                studentId, savedResult.getAnnualAverage());

        return savedResult;
    }

    private double safeDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }
    private void calculateSessionAttendance(SessionResult sessionResult) {
        Student student = sessionResult.getStudent();
        String session = sessionResult.getSession();

        if (student == null || session == null || session.isBlank()) {
            sessionResult.setTotalSchoolDays(0);
            sessionResult.setTotalDaysPresent(0);
            sessionResult.setTotalDaysAbsent(0);
            sessionResult.setAttendancePercentage(0.0);
            return;
        }

        List<Attendance> firstTermAttendance =
                attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(student, session, Result.Term.FIRST);
        List<Attendance> secondTermAttendance =
                attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(student, session, Result.Term.SECOND);
        List<Attendance> thirdTermAttendance =
                attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(student, session, Result.Term.THIRD);

        List<Attendance> allAttendance = new ArrayList<>();
        allAttendance.addAll(firstTermAttendance);
        allAttendance.addAll(secondTermAttendance);
        allAttendance.addAll(thirdTermAttendance);

        Set<String> uniqueSchoolDays = new HashSet<>();
        int presentEquivalent = 0;
        int absent = 0;

        for (Attendance attendance : allAttendance) {
            if (attendance.getDate() == null || attendance.getTerm() == null) {
                continue;
            }

            uniqueSchoolDays.add(attendance.getTerm().name() + "_" + attendance.getDate());

            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || attendance.getStatus() == Attendance.AttendanceStatus.LATE
                    || attendance.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                presentEquivalent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                absent++;
            }
        }

        sessionResult.setTotalSchoolDays(uniqueSchoolDays.size());
        sessionResult.setTotalDaysPresent(presentEquivalent);
        sessionResult.setTotalDaysAbsent(absent);
        sessionResult.setAttendancePercentage(
                uniqueSchoolDays.isEmpty() ? 0 : (presentEquivalent * 100.0 / uniqueSchoolDays.size())
        );
    }

    private void calculateAnnualPositions(SessionResult sessionResult) {
        Student student = sessionResult.getStudent();
        String session = sessionResult.getSession();

        if (student == null || student.getSchoolClass() == null) {
            sessionResult.setAnnualPositionInClass(1);
            sessionResult.setAnnualPositionInArm(1);
            sessionResult.setAnnualPositionInSchool(1);
            sessionResultRepository.save(sessionResult);
            return;
        }

        Long classId = student.getSchoolClass().getId();

        try {
            List<SessionResult> classResults = sessionResultRepository
                    .findByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(classId, session);

            for (int i = 0; i < classResults.size(); i++) {
                SessionResult sr = classResults.get(i);
                int position = i + 1;
                sr.setAnnualPositionInClass(position);
                sr.setAnnualPositionInArm(position);

                if (sr.getId() != null && sr.getId().equals(sessionResult.getId())) {
                    sessionResult.setAnnualPositionInClass(position);
                    sessionResult.setAnnualPositionInArm(position);
                }
            }

            List<SessionResult> schoolResults = sessionResultRepository
                    .findBySessionOrderByAnnualAverageDesc(session);

            for (int i = 0; i < schoolResults.size(); i++) {
                SessionResult sr = schoolResults.get(i);
                int position = i + 1;
                sr.setAnnualPositionInSchool(position);

                if (sr.getStudent() != null && sr.getStudent().getId().equals(student.getId())) {
                    sessionResult.setAnnualPositionInSchool(position);
                }
            }

            sessionResultRepository.saveAll(classResults);
            sessionResultRepository.saveAll(schoolResults);
            sessionResultRepository.save(sessionResult);

        } catch (Exception e) {
            log.error("Error calculating annual positions: {}", e.getMessage(), e);
            sessionResult.setAnnualPositionInClass(1);
            sessionResult.setAnnualPositionInArm(1);
            sessionResult.setAnnualPositionInSchool(1);
            sessionResultRepository.save(sessionResult);
        }
    }

    @Override
    public Map<String, Object> getClassRankings(Long classId, String session, Result.Term term) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        List<Object[]> rankings = resultRepository.getClassRankingByClassId(classId, session, term);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            Student student = (Student) rankings.get(i)[0];
            Number avgNumber = (Number) rankings.get(i)[1];
            double avgScore = avgNumber != null ? avgNumber.doubleValue() : 0.0;

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("average", avgScore);
            item.put("classId", schoolClass.getId());
            item.put("className", schoolClass.getClassName());
            item.put("arm", schoolClass.getArm());

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("classId", schoolClass.getId());
        response.put("className", schoolClass.getClassName());
        response.put("arm", schoolClass.getArm());
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
            Number avgNumber = (Number) rankings.get(i)[1];
            double avgScore = avgNumber != null ? avgNumber.doubleValue() : 0.0;

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("average", avgScore);
            item.put("classId", student.getSchoolClass() != null ? student.getSchoolClass().getId() : null);
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

    @Override
    public void calculateAllTermResults(String session, Result.Term term) {
        List<Student> allStudents = studentRepository.findAll();

        for (Student student : allStudents) {
            try {
                calculateTermResult(student.getId(), session, term);
            } catch (Exception e) {
                log.error("Error calculating term result for student {}: {}",
                        student.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void calculateAllSessionResults(String session) {
        List<Student> allStudents = studentRepository.findAll();

        for (Student student : allStudents) {
            try {
                calculateSessionResult(student.getId(), session);
            } catch (Exception e) {
                log.error("Error calculating session result for student {}: {}",
                        student.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public Map<String, Object> generateResultSheet(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, term)
                .orElseThrow(() -> new RuntimeException("Term result not found for student: " + studentId));

        List<Result> results = resultRepository
                .findByStudentAndSessionAndTerm(student, session, term);

        Long classId = student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;
        int totalStudentsInClass = classId != null ? studentRepository.findBySchoolClassId(classId).size() : 0;

        Map<String, Object> resultSheet = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("id", student.getId());
        studentInfo.put("name", student.getFirstName() + " " +
                (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                student.getLastName());
        studentInfo.put("fullName", student.getFirstName() + " " + student.getLastName());
        studentInfo.put("firstName", student.getFirstName());
        studentInfo.put("lastName", student.getLastName());
        studentInfo.put("middleName", student.getMiddleName());
        studentInfo.put("admissionNumber", student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "");
        studentInfo.put("classId", classId);
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
        summary.put("totalStudentsInArm", totalStudentsInClass);
        summary.put("positionInSchool", termResult.getPositionInSchool() != null ? termResult.getPositionInSchool() : 0);
        summary.put("totalSchoolDays", termResult.getTotalSchoolDays());
        summary.put("daysPresent", termResult.getDaysPresent());
        summary.put("daysAbsent", termResult.getDaysAbsent());
        summary.put("attendancePercentage", termResult.getAttendancePercentage());

        resultSheet.put("summary", summary);

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
        studentInfo.put("id", student.getId());
        studentInfo.put("name", student.getFirstName() + " " +
                (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                student.getLastName());
        studentInfo.put("fullName", student.getFirstName() + " " + student.getLastName());
        studentInfo.put("firstName", student.getFirstName());
        studentInfo.put("lastName", student.getLastName());
        studentInfo.put("middleName", student.getMiddleName());
        studentInfo.put("admissionNumber", student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "");
        studentInfo.put("classId", student.getSchoolClass() != null ? student.getSchoolClass().getId() : null);
        studentInfo.put("class", student.getStudentClass() != null ? student.getStudentClass() : "");
        studentInfo.put("arm", student.getClassArm() != null ? student.getClassArm() : "");
        studentInfo.put("session", session != null ? session : "");
        studentInfo.put("profilePictureUrl", student.getProfilePictureUrl());

        if (student.getParent() != null) {
            String parentName = "";
            if (student.getParent().getFirstName() != null) {
                parentName += student.getParent().getFirstName();
            }
            if (student.getParent().getLastName() != null) {
                parentName += (parentName.isBlank() ? "" : " ") + student.getParent().getLastName();
            }
            studentInfo.put("parentName", parentName.isBlank() ? "N/A" : parentName);
            studentInfo.put("parentPhone",
                    student.getParent().getPhoneNumber() != null ? student.getParent().getPhoneNumber() : "N/A");
            studentInfo.put("address",
                    student.getParent().getAddress() != null ? student.getParent().getAddress() : "N/A");
        } else {
            studentInfo.put("parentName", "N/A");
            studentInfo.put("parentPhone", "N/A");
            studentInfo.put("address", "N/A");
        }

        resultSheet.put("studentInfo", studentInfo);

        Map<String, Object> termSummaries = new HashMap<>();

        if (firstTerm != null) {
            Map<String, Object> firstTermMap = new HashMap<>();
            firstTermMap.put("totalScore", firstTerm.getTotalScore());
            firstTermMap.put("average", firstTerm.getAverage());
            firstTermMap.put("positionInClass", firstTerm.getPositionInClass() != null ? firstTerm.getPositionInClass() : 0);
            termSummaries.put("FIRST", firstTermMap);
        }

        if (secondTerm != null) {
            Map<String, Object> secondTermMap = new HashMap<>();
            secondTermMap.put("totalScore", secondTerm.getTotalScore());
            secondTermMap.put("average", secondTerm.getAverage());
            secondTermMap.put("positionInClass", secondTerm.getPositionInClass() != null ? secondTerm.getPositionInClass() : 0);
            termSummaries.put("SECOND", secondTermMap);
        }

        if (thirdTerm != null) {
            Map<String, Object> thirdTermMap = new HashMap<>();
            thirdTermMap.put("totalScore", thirdTerm.getTotalScore());
            thirdTermMap.put("average", thirdTerm.getAverage());
            thirdTermMap.put("positionInClass", thirdTerm.getPositionInClass() != null ? thirdTerm.getPositionInClass() : 0);
            termSummaries.put("THIRD", thirdTermMap);
        }

        resultSheet.put("termSummaries", termSummaries);

        // keep backward compatibility too
        Map<String, Object> termResults = new HashMap<>();
        if (firstTerm != null) {
            Map<String, Object> m = new HashMap<>();
            m.put("total", firstTerm.getTotalScore());
            m.put("average", firstTerm.getAverage());
            m.put("position", firstTerm.getPositionInClass() != null ? firstTerm.getPositionInClass() : 0);
            termResults.put("firstTerm", m);
        }
        if (secondTerm != null) {
            Map<String, Object> m = new HashMap<>();
            m.put("total", secondTerm.getTotalScore());
            m.put("average", secondTerm.getAverage());
            m.put("position", secondTerm.getPositionInClass() != null ? secondTerm.getPositionInClass() : 0);
            termResults.put("secondTerm", m);
        }
        if (thirdTerm != null) {
            Map<String, Object> m = new HashMap<>();
            m.put("total", thirdTerm.getTotalScore());
            m.put("average", thirdTerm.getAverage());
            m.put("position", thirdTerm.getPositionInClass() != null ? thirdTerm.getPositionInClass() : 0);
            termResults.put("thirdTerm", m);
        }
        resultSheet.put("termResults", termResults);

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

        Map<String, Object> attendance = new HashMap<>();
        attendance.put("totalSchoolDays", sessionResult.getTotalSchoolDays());
        attendance.put("daysPresent", sessionResult.getTotalDaysPresent());
        attendance.put("daysAbsent", sessionResult.getTotalDaysAbsent());
        attendance.put("attendancePercentage", sessionResult.getAttendancePercentage());
        resultSheet.put("attendance", attendance);

        Map<String, Object> promotion = new HashMap<>();
        promotion.put("promoted", sessionResult.isPromoted());
        promotion.put("remark", sessionResult.isPromoted() ? "Promoted to next class" : "Not promoted");
        resultSheet.put("promotion", promotion);

        List<Map<String, Object>> subjectPerformance = new ArrayList<>();

        Set<String> subjectNames = new TreeSet<>();
        if (sessionResult.getSubjectAverages() != null) {
            subjectNames.addAll(sessionResult.getSubjectAverages().keySet());
        }
        if (sessionResult.getSubjectAnnualTotals() != null) {
            subjectNames.addAll(sessionResult.getSubjectAnnualTotals().keySet());
        }

        for (String subject : subjectNames) {
            Map<String, Object> item = new HashMap<>();
            item.put("subject", subject);

            Map<String, Object> termScores = new HashMap<>();
            termScores.put("FIRST", 0.0);
            termScores.put("SECOND", 0.0);
            termScores.put("THIRD", 0.0);

            item.put("termScores", termScores);
            item.put("annualAverage",
                    sessionResult.getSubjectAverages() != null && sessionResult.getSubjectAverages().get(subject) != null
                            ? sessionResult.getSubjectAverages().get(subject)
                            : 0.0);

            subjectPerformance.add(item);
        }

        resultSheet.put("subjectPerformance", subjectPerformance);

        // also expose raw maps for the frontend if needed
        resultSheet.put("subjectAverages",
                sessionResult.getSubjectAverages() != null ? sessionResult.getSubjectAverages() : Collections.emptyMap());
        resultSheet.put("subjectAnnualTotals",
                sessionResult.getSubjectAnnualTotals() != null ? sessionResult.getSubjectAnnualTotals() : Collections.emptyMap());

        log.info("Annual result sheet generated successfully for student: {}", studentId);
        return resultSheet;
    }

    private void calculateTermAttendance(TermResult termResult) {
        List<Attendance> attendanceRecords = attendanceRepository
                .findByStudentAndSessionAndTermOrderByDateAsc(
                        termResult.getStudent(),
                        termResult.getSession(),
                        termResult.getTerm());

        List<LocalDate> schoolDays = attendanceRepository.findDistinctDatesBySessionAndTerm(
                termResult.getSession(),
                termResult.getTerm()
        );

        if ((schoolDays == null || schoolDays.isEmpty()) && !attendanceRecords.isEmpty()) {
            schoolDays = attendanceRecords.stream()
                    .map(Attendance::getDate)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        }

        int totalSchoolDays = schoolDays == null ? 0 : schoolDays.size();
        int presentEquivalent = 0;
        int absent = 0;

        for (Attendance attendance : attendanceRecords) {
            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || attendance.getStatus() == Attendance.AttendanceStatus.LATE
                    || attendance.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                presentEquivalent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                absent++;
            }
        }

        termResult.setTotalSchoolDays(totalSchoolDays);
        termResult.setDaysPresent(presentEquivalent);
        termResult.setDaysAbsent(absent);
        termResult.setAttendancePercentage(
                totalSchoolDays > 0 ? (presentEquivalent * 100.0 / totalSchoolDays) : 0
        );
    }
}