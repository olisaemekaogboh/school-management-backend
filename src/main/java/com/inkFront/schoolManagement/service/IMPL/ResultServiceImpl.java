package com.inkFront.schoolManagement.service.IMPL;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.AssessmentItemDTO;
import com.inkFront.schoolManagement.dto.ResultRequestDTO;
import com.inkFront.schoolManagement.dto.TermAssessmentUpdateDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
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
import java.time.LocalDateTime;
import java.util.*;

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
    private final ObjectMapper objectMapper;

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
                .findDetailedByStudentAndSessionAndTerm(student, request.getSession(), request.getTerm())
                .orElseGet(() -> {
                    TermResult newTermResult = new TermResult();
                    newTermResult.setStudent(student);
                    newTermResult.setSession(request.getSession());
                    newTermResult.setTerm(request.getTerm());
                    newTermResult.setPrintable(false);
                    newTermResult.setPrintLockMessage("Printable result is locked until admin approves");
                    newTermResult.setVisibilityStatus(ResultVisibilityStatus.HIDDEN);
                    newTermResult.setVisibilityMessage("Result is not yet published for student or parent access.");
                    resetApprovalState(newTermResult, "Result modified. Requires re-approval.");
                    return termResultRepository.save(newTermResult);
                });

        Result result = resultRepository
                .findDetailedByStudentAndSubjectAndSessionAndTerm(
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
        }

        resetApprovalState(termResult, "Result modified. Requires re-approval.");
        termResultRepository.save(termResult);

        updateTermAverages(termResult);

        log.info("Result saved successfully with ID: {}", savedResult.getId());
        return savedResult;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateAnnualResultSheet(Long studentId, String session) {
        SessionResult sessionResult = calculateSessionResult(studentId, session);

        Map<String, Object> report = new HashMap<>();
        report.put("studentId", sessionResult.getStudent() != null ? sessionResult.getStudent().getId() : null);
        report.put("session", sessionResult.getSession());
        report.put("firstTermTotal", sessionResult.getFirstTermTotal());
        report.put("secondTermTotal", sessionResult.getSecondTermTotal());
        report.put("thirdTermTotal", sessionResult.getThirdTermTotal());
        report.put("firstTermAverage", sessionResult.getFirstTermAverage());
        report.put("secondTermAverage", sessionResult.getSecondTermAverage());
        report.put("thirdTermAverage", sessionResult.getThirdTermAverage());
        report.put("annualTotal", sessionResult.getAnnualTotal());
        report.put("annualAverage", sessionResult.getAnnualAverage());
        report.put("annualPositionInClass", sessionResult.getAnnualPositionInClass());
        report.put("annualPositionInArm", sessionResult.getAnnualPositionInArm());
        report.put("annualPositionInSchool", sessionResult.getAnnualPositionInSchool());
        report.put("totalSchoolDays", sessionResult.getTotalSchoolDays());
        report.put("totalDaysPresent", sessionResult.getTotalDaysPresent());
        report.put("totalDaysAbsent", sessionResult.getTotalDaysAbsent());
        report.put("attendancePercentage", sessionResult.getAttendancePercentage());
        report.put("promoted", sessionResult.isPromoted());
        report.put("promotionRemark", sessionResult.getPromotionRemark());
        report.put("firstTermSubjectScores", sessionResult.getFirstTermSubjectScores());
        report.put("secondTermSubjectScores", sessionResult.getSecondTermSubjectScores());
        report.put("thirdTermSubjectScores", sessionResult.getThirdTermSubjectScores());
        report.put("subjectAnnualTotals", sessionResult.getSubjectAnnualTotals());
        report.put("subjectAverages", sessionResult.getSubjectAverages());
        report.put("printable", sessionResult.isPrintable());
        report.put("printLockMessage", sessionResult.getPrintLockMessage());
        report.put("visibilityStatus", sessionResult.getVisibilityStatus() != null ? sessionResult.getVisibilityStatus().name() : null);
        report.put("visibilityMessage", sessionResult.getVisibilityMessage());
        report.put("publishedAt", sessionResult.getPublishedAt());
        report.put("publishedByName", sessionResult.getPublishedByName());

        return report;
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
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseGet(() -> {
                    TermResult newTermResult = new TermResult();
                    newTermResult.setStudent(student);
                    newTermResult.setSession(session);
                    newTermResult.setTerm(term);
                    newTermResult.setPrintable(false);
                    newTermResult.setPrintLockMessage("Printable result is locked until admin approves");
                    newTermResult.setVisibilityStatus(ResultVisibilityStatus.HIDDEN);
                    newTermResult.setVisibilityMessage("Result is not yet published for student or parent access.");
                    resetApprovalState(newTermResult, "Result modified. Requires re-approval.");
                    return termResultRepository.save(newTermResult);
                });

        Result result = resultRepository
                .findDetailedByStudentAndSubjectAndSessionAndTerm(student, subject, session, term)
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
        }

        resetApprovalState(termResult, "Result modified. Requires re-approval.");
        termResultRepository.save(termResult);

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
        List<Result> results = resultRepository.findDetailedByStudentAndSessionAndTerm(
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

        return resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, term);
    }

    @Override
    public TermResult calculateTermResult(Long studentId, String session, Result.Term term) {
        log.info("Calculating term result for student: {}, session: {}, term: {}", studentId, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Result> subjectResults = resultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term);

        if (subjectResults.isEmpty()) {
            throw new RuntimeException("No results found for this student in the specified term");
        }

        TermResult termResult = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseGet(() -> {
                    TermResult tr = new TermResult();
                    tr.setPrintable(false);
                    tr.setPrintLockMessage("Printable result is locked until admin approves");
                    tr.setVisibilityStatus(ResultVisibilityStatus.HIDDEN);
                    tr.setVisibilityMessage("Result is not yet published for student or parent access.");
                    return tr;
                });

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
                    .findDetailedByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
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
                .findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.FIRST)
                .orElse(null);

        TermResult secondTerm = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.SECOND)
                .orElse(null);

        TermResult thirdTerm = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.THIRD)
                .orElse(null);

        SessionResult sessionResult = sessionResultRepository
                .findDetailedByStudentAndSession(student, session)
                .orElseGet(() -> {
                    SessionResult sr = new SessionResult();
                    sr.setPrintable(false);
                    sr.setPrintLockMessage("Printable result is locked until admin approves");
                    sr.setVisibilityStatus(ResultVisibilityStatus.HIDDEN);
                    sr.setVisibilityMessage("Result is not yet published for student or parent access.");
                    return sr;
                });

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
                resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);
        List<Result> secondTermResults =
                resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);
        List<Result> thirdTermResults =
                resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

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
                    .findDetailedByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(classId, session);

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
                    .findDetailedBySessionOrderByAnnualAverageDesc(session);

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
    public TermResult setTermResultPrintableStatus(
            Long studentId,
            String session,
            Result.Term term,
            boolean printable,
            String printLockMessage
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Term result not found for student ID " + studentId +
                                " in session " + session + " and term " + term
                ));

        boolean completed = Boolean.TRUE.equals(termResult.isCompleted());

        if (printable && !completed) {
            throw new RuntimeException("Result is incomplete. Required signatures are missing.");
        }

        termResult.setPrintable(printable);
        termResult.setPrintLockMessage(
                printLockMessage != null && !printLockMessage.trim().isEmpty()
                        ? printLockMessage.trim()
                        : (printable
                        ? "Printable result is available."
                        : "Printable result is locked. The admin will unlock it when the result is ready.")
        );

        if (printable) {
            termResult.setVisibilityStatus(ResultVisibilityStatus.PRINTABLE);
            if (termResult.getVisibilityMessage() == null || termResult.getVisibilityMessage().isBlank()) {
                termResult.setVisibilityMessage("Result has been published for viewing and printing.");
            }
        } else if (termResult.getVisibilityStatus() == ResultVisibilityStatus.PRINTABLE) {
            termResult.setVisibilityStatus(ResultVisibilityStatus.PUBLISHED);
        }

        return termResultRepository.save(termResult);
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

    @Override
    @Transactional
    public TermResult updateTermAssessment(
            Long studentId,
            String session,
            Result.Term term,
            TermAssessmentUpdateDTO request
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseGet(() -> {
                    TermResult newTermResult = new TermResult();
                    newTermResult.setStudent(student);
                    newTermResult.setSession(session);
                    newTermResult.setTerm(term);
                    newTermResult.setPrintable(false);
                    newTermResult.setPrintLockMessage("Printable result is locked until admin approves");
                    newTermResult.setVisibilityStatus(ResultVisibilityStatus.HIDDEN);
                    newTermResult.setVisibilityMessage("Result is not yet published for student or parent access.");
                    resetApprovalState(newTermResult, "Result modified. Requires re-approval.");
                    return newTermResult;
                });

        List<AssessmentItemDTO> characterTraits = sanitizeAssessmentItems(request.getCharacterTraits());
        List<AssessmentItemDTO> psychomotorTraits = sanitizeAssessmentItems(request.getPsychomotorTraits());

        termResult.setCharacterTraitsJson(toJson(
                characterTraits.isEmpty() ? defaultCharacterTraits() : characterTraits
        ));
        termResult.setPsychomotorTraitsJson(toJson(
                psychomotorTraits.isEmpty() ? defaultPsychomotorTraits() : psychomotorTraits
        ));

        if (request.getClassTeacherComment() != null) {
            termResult.setClassTeacherComment(request.getClassTeacherComment().trim());
        }

        if (request.getPrincipalComment() != null) {
            termResult.setPrincipalComment(request.getPrincipalComment().trim());
        }

        termResult.setNextTermBegins(request.getNextTermBegins());

        resetApprovalState(termResult, "Result modified. Requires re-approval.");

        return termResultRepository.save(termResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateResultSheet(Long studentId, String session, Result.Term term) {
        log.info("Generating term result sheet for student: {}, session: {}, term: {}", studentId, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseGet(() -> calculateTermResult(studentId, session, term));

        List<Result> subjectResults = resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, term);

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
                .map(this::buildSubjectRow)
                .toList();

        report.put("subjects", subjects);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalScore", termResult.getTotalScore());
        summary.put("average", termResult.getAverage());
        summary.put("positionInClass", termResult.getPositionInClass());
        summary.put("positionInArm", termResult.getPositionInArm());
        summary.put("positionInSchool", termResult.getPositionInSchool());
        summary.put("totalSchoolDays", termResult.getTotalSchoolDays());
        summary.put("daysPresent", termResult.getDaysPresent());
        summary.put("daysAbsent", termResult.getDaysAbsent());
        summary.put("attendancePercentage", termResult.getAttendancePercentage());
        summary.put("teacherComment", termResult.getClassTeacherComment());
        summary.put("principalComment", termResult.getPrincipalComment());
        summary.put("nextTermBegins", termResult.getNextTermBegins());
        report.put("summary", summary);

        report.put("characterTraits",
                fromJson(termResult.getCharacterTraitsJson()).isEmpty()
                        ? defaultCharacterTraits()
                        : fromJson(termResult.getCharacterTraitsJson()));

        report.put("psychomotorTraits",
                fromJson(termResult.getPsychomotorTraitsJson()).isEmpty()
                        ? defaultPsychomotorTraits()
                        : fromJson(termResult.getPsychomotorTraitsJson()));

        report.put("gradingScale", List.of(
                Map.of("grade", "A", "min", 70, "max", 100, "remark", "Excellent"),
                Map.of("grade", "B", "min", 60, "max", 69, "remark", "Very Good"),
                Map.of("grade", "C", "min", 50, "max", 59, "remark", "Good"),
                Map.of("grade", "D", "min", 45, "max", 49, "remark", "Pass"),
                Map.of("grade", "E", "min", 40, "max", 44, "remark", "Fair"),
                Map.of("grade", "F", "min", 0, "max", 39, "remark", "Fail")
        ));

        Map<String, Object> signatures = new HashMap<>();
        signatures.put("classTeacherSigned", safeBoolean(termResult.isClassTeacherSigned()));
        signatures.put("adminSigned", safeBoolean(termResult.isAdminSigned()));
        signatures.put("completed", safeBoolean(termResult.isCompleted()));
        signatures.put("classTeacherSignedAt", termResult.getClassTeacherSignedAt());
        signatures.put("adminSignedAt", termResult.getAdminSignedAt());
        signatures.put("classTeacherSignature", termResult.getClassTeacherSignatureUrl());
        signatures.put("adminSignature", termResult.getAdminSignatureUrl());
        report.put("signatures", signatures);

        report.put("completed", safeBoolean(termResult.isCompleted()));
        report.put("printable", termResult.isPrintable());
        report.put("printLockMessage", termResult.getPrintLockMessage());
        report.put("visibilityStatus", termResult.getVisibilityStatus() != null ? termResult.getVisibilityStatus().name() : null);
        report.put("visibilityMessage", termResult.getVisibilityMessage());
        report.put("publishedAt", termResult.getPublishedAt());
        report.put("publishedByName", termResult.getPublishedByName());

        return report;
    }

    private Map<String, Object> buildSubjectRow(Result result) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", result.getId());
        row.put("subject", result.getSubject() != null ? result.getSubject().getName() : "-");
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
        row.put("positionInClass", result.getPositionInClass());
        row.put("positionInArm", result.getPositionInArm());
        row.put("positionInSchool", result.getPositionInSchool());
        return row;
    }

    private String toJson(List<AssessmentItemDTO> items) {
        try {
            return objectMapper.writeValueAsString(items != null ? items : List.of());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize assessment items", e);
        }
    }

    private List<AssessmentItemDTO> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<AssessmentItemDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize assessment items json: {}", json, e);
            return new ArrayList<>();
        }
    }

    private List<AssessmentItemDTO> defaultCharacterTraits() {
        return List.of(
                new AssessmentItemDTO("Punctuality", 1),
                new AssessmentItemDTO("Attendance", 1),
                new AssessmentItemDTO("Neatness", 1),
                new AssessmentItemDTO("Politeness", 1),
                new AssessmentItemDTO("Honesty", 1),
                new AssessmentItemDTO("Relationship With Others", 1),
                new AssessmentItemDTO("Leadership", 1),
                new AssessmentItemDTO("Emotional Stability", 1)
        );
    }

    private List<AssessmentItemDTO> defaultPsychomotorTraits() {
        return List.of(
                new AssessmentItemDTO("Handwriting", 1),
                new AssessmentItemDTO("Verbal Fluency", 1),
                new AssessmentItemDTO("Sports", 1),
                new AssessmentItemDTO("Drawing / Creativity", 1),
                new AssessmentItemDTO("Craft", 1),
                new AssessmentItemDTO("Musical Skills", 1)
        );
    }

    private List<AssessmentItemDTO> sanitizeAssessmentItems(List<AssessmentItemDTO> items) {
        if (items == null) return new ArrayList<>();

        return items.stream()
                .filter(item -> item != null && item.getLabel() != null && !item.getLabel().trim().isEmpty())
                .map(item -> AssessmentItemDTO.builder()
                        .label(item.getLabel().trim())
                        .score(Math.max(1, Math.min(5, item.getScore() == null ? 1 : item.getScore())))
                        .build())
                .toList();
    }

    private boolean safeBoolean(Boolean value) {
        return value != null && value;
    }

    private void resetApprovalState(TermResult termResult, String lockMessage) {
        termResult.setClassTeacherSigned(false);
        termResult.setAdminSigned(false);
        termResult.setCompleted(false);

        termResult.setClassTeacherSignedAt(null);
        termResult.setAdminSignedAt(null);

        termResult.setClassTeacherSignatureUrl(null);
        termResult.setAdminSignatureUrl(null);

        termResult.setPrintable(false);
        termResult.setPrintLockMessage(
                lockMessage != null && !lockMessage.isBlank()
                        ? lockMessage
                        : "Result modified. Requires re-approval."
        );

        termResult.setVisibilityStatus(ResultVisibilityStatus.HIDDEN);
        termResult.setVisibilityMessage("Result modified. Requires admin republication.");
        termResult.setPublishedAt(null);
        termResult.setPublishedByName(null);
    }

    private void refreshCompletionStatus(TermResult termResult) {
        boolean completed =
                safeBoolean(termResult.isClassTeacherSigned()) &&
                        safeBoolean(termResult.isAdminSigned());

        termResult.setCompleted(completed);

        if (!completed) {
            termResult.setPrintable(false);

            if (termResult.getVisibilityStatus() == ResultVisibilityStatus.PRINTABLE) {
                termResult.setVisibilityStatus(ResultVisibilityStatus.PUBLISHED);
            }

            if (termResult.getPrintLockMessage() == null || termResult.getPrintLockMessage().isBlank()) {
                termResult.setPrintLockMessage("Result is incomplete. Awaiting required signatures.");
            }
        }
    }

    @Override
    public TermResult signByClassTeacher(
            Long studentId,
            String session,
            Result.Term term,
            String signatureUrl
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Term result not found for student ID " + studentId +
                                " in session " + session + " and term " + term
                ));

        termResult.setClassTeacherSigned(true);
        termResult.setClassTeacherSignedAt(LocalDateTime.now());
        termResult.setClassTeacherSignatureUrl(signatureUrl);

        refreshCompletionStatus(termResult);

        if (!safeBoolean(termResult.isCompleted())) {
            termResult.setPrintLockMessage("Result is incomplete. Awaiting admin approval.");
        }

        return termResultRepository.save(termResult);
    }

    @Override
    public TermResult signByAdmin(
            Long studentId,
            String session,
            Result.Term term,
            String signatureUrl
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        TermResult termResult = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, term)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Term result not found for student ID " + studentId +
                                " in session " + session + " and term " + term
                ));

        termResult.setAdminSigned(true);
        termResult.setAdminSignedAt(LocalDateTime.now());
        termResult.setAdminSignatureUrl(signatureUrl);

        refreshCompletionStatus(termResult);

        if (safeBoolean(termResult.isCompleted())) {
            termResult.setPrintLockMessage("Result fully approved.");
        } else {
            termResult.setPrintable(false);
            termResult.setPrintLockMessage("Result is incomplete. Awaiting class teacher signature.");
        }

        return termResultRepository.save(termResult);
    }
}