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
    @Transactional(readOnly = true)
    public Map<String, Object> generateResultSheet(Long studentId, String session, Result.Term term) {
        log.info("Generating term result sheet for student: {}, session: {}, term: {}", studentId, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, term)
                .orElseGet(() -> calculateTermResult(studentId, session, term));

        List<Result> subjectResults = resultRepository.findByStudentAndSessionAndTerm(student, session, term);

        Map<String, Object> report = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("id", student.getId());
        studentInfo.put("firstName", student.getFirstName());
        studentInfo.put("middleName", student.getMiddleName());
        studentInfo.put("lastName", student.getLastName());
        studentInfo.put("fullName", (
                (student.getFirstName() != null ? student.getFirstName() : "") + " " +
                        (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                        (student.getLastName() != null ? student.getLastName() : "")
        ).replaceAll("\\s+", " ").trim());
        studentInfo.put("admissionNumber", student.getAdmissionNumber());
        studentInfo.put("studentClass", student.getStudentClass());
        studentInfo.put("classArm", student.getClassArm());
        studentInfo.put("classCode", student.getSchoolClass() != null ? student.getSchoolClass().getClassCode() : null);
        studentInfo.put("session", session);
        studentInfo.put("term", term.name());
        studentInfo.put("profilePictureUrl", student.getProfilePictureUrl());
        studentInfo.put("dateOfBirth", student.getDateOfBirth());
        studentInfo.put("parentName", student.getParentName());
        studentInfo.put("parentPhone", student.getParentPhone());
        studentInfo.put("address", student.getAddress());
        report.put("studentInfo", studentInfo);

        List<Map<String, Object>> subjects = subjectResults.stream()
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", r.getId());
                    item.put("subject", r.getSubject() != null ? r.getSubject().getName() : "-");
                    item.put("resumptionTest", safeDouble(r.getResumptionTest()));
                    item.put("assignments", safeDouble(r.getAssignments()));
                    item.put("project", safeDouble(r.getProject()));
                    item.put("midtermTest", safeDouble(r.getMidtermTest()));
                    item.put("secondTest", safeDouble(r.getSecondTest()));
                    item.put("continuousAssessment", safeDouble(r.getContinuousAssessment()));
                    item.put("examination", safeDouble(r.getExamination()));
                    item.put("total", safeDouble(r.getTotal()));
                    item.put("grade", r.getGrade());
                    item.put("remarks", r.getRemarks());
                    return item;
                })
                .toList();

        report.put("subjects", subjects);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalScore", safeDouble(termResult.getTotalScore()));
        summary.put("average", safeDouble(termResult.getAverage()));
        summary.put("positionInClass", termResult.getPositionInClass());
        summary.put("positionInArm", termResult.getPositionInArm());
        summary.put("positionInSchool", termResult.getPositionInSchool());
        summary.put("totalSchoolDays", termResult.getTotalSchoolDays());
        summary.put("daysPresent", termResult.getDaysPresent());
        summary.put("daysAbsent", termResult.getDaysAbsent());
        summary.put("attendancePercentage", safeDouble(termResult.getAttendancePercentage()));
        summary.put("classTeacherComment", termResult.getClassTeacherComment());
        summary.put("principalComment", termResult.getPrincipalComment());
        report.put("summary", summary);

        return report;
    }
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateAnnualResultSheet(Long studentId, String session) {
        log.info("Generating annual result sheet for student: {}, session: {}", studentId, session);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        SessionResult sessionResult = sessionResultRepository.findByStudentAndSession(student, session)
                .orElseGet(() -> calculateSessionResult(studentId, session));

        TermResult firstTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST)
                .orElse(null);

        TermResult secondTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND)
                .orElse(null);

        TermResult thirdTerm = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD)
                .orElse(null);

        List<Result> firstTermResults = firstTerm == null
                ? List.of()
                : resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);

        List<Result> secondTermResults = secondTerm == null
                ? List.of()
                : resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);

        List<Result> thirdTermResults = thirdTerm == null
                ? List.of()
                : resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

        Map<String, Object> resultSheet = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("id", student.getId());
        studentInfo.put("name", (student.getFirstName() + " " + student.getLastName()).trim());
        studentInfo.put("firstName", student.getFirstName());
        studentInfo.put("lastName", student.getLastName());
        studentInfo.put("admissionNumber", student.getAdmissionNumber());
        studentInfo.put("class", student.getStudentClass());
        studentInfo.put("arm", student.getClassArm());
        studentInfo.put("session", session);
        studentInfo.put("profilePictureUrl", student.getProfilePictureUrl());
        studentInfo.put("dateOfBirth", student.getDateOfBirth());
        studentInfo.put("parentName", student.getParentName());
        studentInfo.put("parentPhone", student.getParentPhone());
        studentInfo.put("address", student.getAddress());
        resultSheet.put("studentInfo", studentInfo);

        Map<String, Object> termSummaries = new HashMap<>();

        if (firstTerm != null) {
            Map<String, Object> first = new HashMap<>();
            first.put("total", safeDouble(firstTerm.getTotalScore()));
            first.put("average", safeDouble(firstTerm.getAverage()));
            first.put("position", firstTerm.getPositionInClass());
            first.put("attendance", safeDouble(firstTerm.getAttendancePercentage()));
            first.put("subjects", firstTermResults.stream().map(this::toAnnualTermSubjectRow).toList());
            termSummaries.put("FIRST", first);
        }

        if (secondTerm != null) {
            Map<String, Object> second = new HashMap<>();
            second.put("total", safeDouble(secondTerm.getTotalScore()));
            second.put("average", safeDouble(secondTerm.getAverage()));
            second.put("position", secondTerm.getPositionInClass());
            second.put("attendance", safeDouble(secondTerm.getAttendancePercentage()));
            second.put("subjects", secondTermResults.stream().map(this::toAnnualTermSubjectRow).toList());
            termSummaries.put("SECOND", second);
        }

        if (thirdTerm != null) {
            Map<String, Object> third = new HashMap<>();
            third.put("total", safeDouble(thirdTerm.getTotalScore()));
            third.put("average", safeDouble(thirdTerm.getAverage()));
            third.put("position", thirdTerm.getPositionInClass());
            third.put("attendance", safeDouble(thirdTerm.getAttendancePercentage()));
            third.put("subjects", thirdTermResults.stream().map(this::toAnnualTermSubjectRow).toList());
            termSummaries.put("THIRD", third);
        }

        resultSheet.put("termSummaries", termSummaries);
        resultSheet.put("termResults", termSummaries); // frontend fallback support

        Map<String, Object> annualSummary = new HashMap<>();
        annualSummary.put("firstTermTotal", safeDouble(sessionResult.getFirstTermTotal()));
        annualSummary.put("secondTermTotal", safeDouble(sessionResult.getSecondTermTotal()));
        annualSummary.put("thirdTermTotal", safeDouble(sessionResult.getThirdTermTotal()));
        annualSummary.put("annualTotal", safeDouble(sessionResult.getAnnualTotal()));
        annualSummary.put("annualAverage", safeDouble(sessionResult.getAnnualAverage()));
        annualSummary.put("positionInClass", sessionResult.getAnnualPositionInClass() != null ? sessionResult.getAnnualPositionInClass() : 0);
        annualSummary.put("positionInArm", sessionResult.getAnnualPositionInArm() != null ? sessionResult.getAnnualPositionInArm() : 0);
        annualSummary.put("positionInSchool", sessionResult.getAnnualPositionInSchool() != null ? sessionResult.getAnnualPositionInSchool() : 0);
        annualSummary.put("promoted", sessionResult.isPromoted());
        annualSummary.put("remark", sessionResult.getPromotionRemark());
        annualSummary.put("subjectAverages", sessionResult.getSubjectAverages() != null
                ? sessionResult.getSubjectAverages()
                : Collections.emptyMap());
        resultSheet.put("annualSummary", annualSummary);

        Map<String, Object> attendance = new HashMap<>();
        attendance.put("totalSchoolDays", sessionResult.getTotalSchoolDays());
        attendance.put("daysPresent", sessionResult.getTotalDaysPresent());
        attendance.put("daysAbsent", sessionResult.getTotalDaysAbsent());
        attendance.put("attendancePercentage", sessionResult.getAttendancePercentage());
        resultSheet.put("attendance", attendance);

        Map<String, Object> promotion = new HashMap<>();
        promotion.put("promoted", sessionResult.isPromoted());
        promotion.put("remark", sessionResult.getPromotionRemark());
        resultSheet.put("promotion", promotion);

        Map<String, Double> firstScores = sessionResult.getFirstTermSubjectScores() != null
                ? sessionResult.getFirstTermSubjectScores()
                : Collections.emptyMap();
        Map<String, Double> secondScores = sessionResult.getSecondTermSubjectScores() != null
                ? sessionResult.getSecondTermSubjectScores()
                : Collections.emptyMap();
        Map<String, Double> thirdScores = sessionResult.getThirdTermSubjectScores() != null
                ? sessionResult.getThirdTermSubjectScores()
                : Collections.emptyMap();
        Map<String, Double> annualTotals = sessionResult.getSubjectAnnualTotals() != null
                ? sessionResult.getSubjectAnnualTotals()
                : Collections.emptyMap();
        Map<String, Double> subjectAverages = sessionResult.getSubjectAverages() != null
                ? sessionResult.getSubjectAverages()
                : Collections.emptyMap();

        Set<String> subjectNames = new TreeSet<>();
        subjectNames.addAll(firstScores.keySet());
        subjectNames.addAll(secondScores.keySet());
        subjectNames.addAll(thirdScores.keySet());
        subjectNames.addAll(annualTotals.keySet());
        subjectNames.addAll(subjectAverages.keySet());

        List<Map<String, Object>> annualSubjects = new ArrayList<>();
        for (String subject : subjectNames) {
            double first = safeDouble(firstScores.get(subject));
            double second = safeDouble(secondScores.get(subject));
            double third = safeDouble(thirdScores.get(subject));
            double total = annualTotals.containsKey(subject)
                    ? safeDouble(annualTotals.get(subject))
                    : (first + second + third);

            double average = subjectAverages.containsKey(subject)
                    ? safeDouble(subjectAverages.get(subject))
                    : computeAverageFromAvailableTerms(firstScores.containsKey(subject), secondScores.containsKey(subject), thirdScores.containsKey(subject), first, second, third);

            Map<String, Object> item = new HashMap<>();
            item.put("subject", subject);
            item.put("firstTerm", first);
            item.put("secondTerm", second);
            item.put("thirdTerm", third);
            item.put("total", total);
            item.put("annualTotal", total);
            item.put("average", average);
            item.put("annualAverage", average);
            item.put("grade", gradeFromScore(average));
            item.put("remark", remarkFromScore(average));

            Map<String, Double> termScores = new HashMap<>();
            termScores.put("FIRST", first);
            termScores.put("SECOND", second);
            termScores.put("THIRD", third);
            item.put("termScores", termScores);

            annualSubjects.add(item);
        }

        resultSheet.put("subjectPerformance", annualSubjects);
        resultSheet.put("subjects", annualSubjects); // frontend directSubjects support
        resultSheet.put("annualSubjects", annualSubjects);
        resultSheet.put("subjectAverages", subjectAverages);
        resultSheet.put("subjectAnnualTotals", annualTotals);

        log.info("Annual result sheet generated successfully for student: {}", studentId);
        return resultSheet;
    }

    private Map<String, Object> toAnnualTermSubjectRow(Result result) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", result.getId());
        row.put("subject", result.getSubject() != null ? result.getSubject().getName() : "-");
        row.put("subjectName", result.getSubject() != null ? result.getSubject().getName() : "-");
        row.put("resumptionTest", safeDouble(result.getResumptionTest()));
        row.put("assignments", safeDouble(result.getAssignments()));
        row.put("project", safeDouble(result.getProject()));
        row.put("midtermTest", safeDouble(result.getMidtermTest()));
        row.put("secondTest", safeDouble(result.getSecondTest()));
        row.put("continuousAssessment", safeDouble(result.getContinuousAssessment()));
        row.put("examination", safeDouble(result.getExamination()));
        row.put("total", safeDouble(result.getTotal()));
        row.put("totalScore", safeDouble(result.getTotal()));
        row.put("grade", result.getGrade());
        row.put("remarks", result.getRemarks());
        return row;
    }

    private double computeAverageFromAvailableTerms(
            boolean hasFirst,
            boolean hasSecond,
            boolean hasThird,
            double first,
            double second,
            double third
    ) {
        int count = 0;
        double sum = 0.0;

        if (hasFirst) {
            sum += first;
            count++;
        }
        if (hasSecond) {
            sum += second;
            count++;
        }
        if (hasThird) {
            sum += third;
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }

    private String gradeFromScore(double score) {
        if (score >= 70) return "A";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        if (score >= 45) return "D";
        if (score >= 40) return "E";
        return "F";
    }

    private String remarkFromScore(double score) {
        if (score >= 70) return "Excellent";
        if (score >= 60) return "Very Good";
        if (score >= 50) return "Good";
        if (score >= 45) return "Pass";
        if (score >= 40) return "Fair";
        return "Fail";
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