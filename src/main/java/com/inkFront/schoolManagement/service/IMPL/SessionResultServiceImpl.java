package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.ResultRepository;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.service.SessionResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionResultServiceImpl implements SessionResultService {

    private final StudentRepository studentRepository;
    private final TermResultRepository termResultRepository;
    private final SessionResultRepository sessionResultRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassRepository classRepository;

    @Override
    public SessionResultResponseDTO calculateSessionResult(Long studentId, String session) {
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

        if (firstTerm == null && secondTerm == null && thirdTerm == null) {
            throw new RuntimeException("No term result found for this student in " + session);
        }

        SessionResult sessionResult = sessionResultRepository
                .findByStudentAndSession(student, session)
                .orElse(new SessionResult());

        sessionResult.setStudent(student);
        sessionResult.setSession(session);

        sessionResult.setFirstTermTotal(0);
        sessionResult.setSecondTermTotal(0);
        sessionResult.setThirdTermTotal(0);

        sessionResult.setFirstTermAverage(0);
        sessionResult.setSecondTermAverage(0);
        sessionResult.setThirdTermAverage(0);

        sessionResult.setFirstTermPosition(null);
        sessionResult.setSecondTermPosition(null);
        sessionResult.setThirdTermPosition(null);

        if (firstTerm != null) {
            sessionResult.setFirstTermTotal(safeDouble(firstTerm.getTotalScore()));
            sessionResult.setFirstTermAverage(safeDouble(firstTerm.getAverage()));
            sessionResult.setFirstTermPosition(firstTerm.getPositionInClass());
        }

        if (secondTerm != null) {
            sessionResult.setSecondTermTotal(safeDouble(secondTerm.getTotalScore()));
            sessionResult.setSecondTermAverage(safeDouble(secondTerm.getAverage()));
            sessionResult.setSecondTermPosition(secondTerm.getPositionInClass());
        }

        if (thirdTerm != null) {
            sessionResult.setThirdTermTotal(safeDouble(thirdTerm.getTotalScore()));
            sessionResult.setThirdTermAverage(safeDouble(thirdTerm.getAverage()));
            sessionResult.setThirdTermPosition(thirdTerm.getPositionInClass());
        }

        populateAttendance(sessionResult, student, session, firstTerm, secondTerm, thirdTerm);
        populateSubjectPerformance(sessionResult, student, session);
        populateAnnualSummary(sessionResult, firstTerm, secondTerm, thirdTerm);
        applyPromotionDecision(sessionResult);

        SessionResult savedResult = sessionResultRepository.save(sessionResult);

        calculateAnnualPositions(savedResult);

        SessionResult fresh = sessionResultRepository.findById(savedResult.getId())
                .orElse(savedResult);

        return SessionResultResponseDTO.fromEntity(fresh);
    }

    @Override
    public List<SessionResultResponseDTO> calculateAllSessionResults(String session) {
        log.info("Calculating session results for all students in session: {}", session);

        List<Student> allStudents = studentRepository.findAll();
        List<SessionResultResponseDTO> results = new ArrayList<>();

        for (Student student : allStudents) {
            try {
                SessionResultResponseDTO result = calculateSessionResult(student.getId(), session);
                results.add(result);
            } catch (Exception e) {
                log.error("Error calculating session result for student {}: {}", student.getId(), e.getMessage());
            }
        }

        calculateAllPositions(session);
        return results;
    }

    @Override
    public List<SessionResultResponseDTO> calculateClassArmSessionResults(String className, String arm, String session) {
        log.info("Calculating class arm session results for class: {} arm: {} session: {}", className, arm, session);

        List<Student> students = studentRepository.findByStudentClassAndClassArmNormalized(className, arm);

        log.info("Found {} students for normalized class lookup: class='{}', arm='{}'", students.size(), className, arm);

        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionResultResponseDTO> results = new ArrayList<>();

        for (Student student : students) {
            try {
                SessionResultResponseDTO result = calculateSessionResult(student.getId(), session);
                results.add(result);
            } catch (Exception e) {
                log.error(
                        "Error calculating session result for student {} ({} {}): {}",
                        student.getId(),
                        student.getFirstName(),
                        student.getLastName(),
                        e.getMessage(),
                        e
                );
            }
        }

        calculateAllPositions(session);

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResultResponseDTO getSessionResult(Long studentId, String session) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        SessionResult result = sessionResultRepository.findByStudentAndSession(student, session)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session result not found for student ID " + studentId + " in session " + session
                ));

        return SessionResultResponseDTO.fromEntity(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResultResponseDTO> getClassSessionResults(String className, String session) {
        return sessionResultRepository.findByClassAndSessionOrderByAnnualAverageDesc(className, session)
                .stream()
                .map(SessionResultResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResultResponseDTO> getArmSessionResults(String className, String arm, String session) {
        return sessionResultRepository.findByClassAndArmAndSessionOrderByAnnualAverageDesc(className, arm, session)
                .stream()
                .map(SessionResultResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public Map<String, Object> getSchoolSessionRankings(String session) {
        List<SessionResult> rankings = sessionResultRepository.findBySessionOrderByAnnualAverageDesc(session);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            SessionResult sr = rankings.get(i);
            Student student = sr.getStudent();

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("studentClass", student.getStudentClass());
            item.put("classArm", student.getClassArm());
            item.put("annualAverage", sr.getAnnualAverage());
            item.put("annualTotal", sr.getAnnualTotal());
            item.put("attendance", sr.getAttendancePercentage());
            item.put("promoted", sr.isPromoted());

            Map<String, Object> termAverages = new HashMap<>();
            termAverages.put("first", sr.getFirstTermAverage());
            termAverages.put("second", sr.getSecondTermAverage());
            termAverages.put("third", sr.getThirdTermAverage());
            item.put("termAverages", termAverages);

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("session", session);
        response.put("totalStudents", resultList.size());
        response.put("rankings", resultList);

        long promoted = sessionResultRepository.countPromotedStudents(session);
        long retained = sessionResultRepository.countRetainedStudents(session);

        Map<String, Object> stats = new HashMap<>();
        stats.put("promoted", promoted);
        stats.put("retained", retained);
        stats.put("promotionRate", resultList.size() > 0 ? (promoted * 100.0 / resultList.size()) : 0);

        response.put("statistics", stats);

        return response;
    }

    @Override
    public Map<String, Object> getClassRankings(String className, String session) {
        List<SessionResult> rankings = sessionResultRepository.findByClassAndSessionOrderByAnnualAverageDesc(className, session);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            SessionResult sr = rankings.get(i);
            Student student = sr.getStudent();

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("studentClass", student.getStudentClass());
            item.put("classArm", student.getClassArm());
            item.put("annualAverage", sr.getAnnualAverage());
            item.put("attendance", sr.getAttendancePercentage());
            item.put("promoted", sr.isPromoted());

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("className", className);
        response.put("session", session);
        response.put("totalStudents", resultList.size());
        response.put("rankings", resultList);

        return response;
    }

    @Override
    public Map<String, Object> getArmRankings(String className, String arm, String session) {
        List<SessionResult> rankings = sessionResultRepository.findByClassAndArmAndSessionOrderByAnnualAverageDesc(className, arm, session);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            SessionResult sr = rankings.get(i);
            Student student = sr.getStudent();

            Map<String, Object> item = new HashMap<>();
            item.put("position", i + 1);
            item.put("studentId", student.getId());
            item.put("studentName", student.getFirstName() + " " + student.getLastName());
            item.put("admissionNumber", student.getAdmissionNumber());
            item.put("studentClass", student.getStudentClass());
            item.put("classArm", student.getClassArm());
            item.put("annualAverage", sr.getAnnualAverage());
            item.put("attendance", sr.getAttendancePercentage());
            item.put("promoted", sr.isPromoted());

            resultList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("className", className);
        response.put("arm", arm);
        response.put("session", session);
        response.put("totalStudents", resultList.size());
        response.put("rankings", resultList);

        return response;
    }

    @Override
    public Map<String, Object> generateSessionReport(Long studentId, String session) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        calculateSessionResult(studentId, session);

        SessionResult sessionResult = sessionResultRepository.findByStudentAndSession(student, session)
                .orElseThrow(() -> new RuntimeException("Session result not found"));

        TermResult firstTerm = termResultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST).orElse(null);
        TermResult secondTerm = termResultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND).orElse(null);
        TermResult thirdTerm = termResultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD).orElse(null);

        Map<String, Object> report = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("id", student.getId());
        studentInfo.put("name", student.getFirstName() + " " + student.getLastName());
        studentInfo.put("admissionNumber", student.getAdmissionNumber());
        studentInfo.put("class", student.getStudentClass());
        studentInfo.put("arm", student.getClassArm());
        studentInfo.put("session", session);
        report.put("studentInfo", studentInfo);

        Map<String, Object> termSummaries = new HashMap<>();

        if (firstTerm != null) {
            Map<String, Object> first = new HashMap<>();
            first.put("total", firstTerm.getTotalScore());
            first.put("average", firstTerm.getAverage());
            first.put("position", firstTerm.getPositionInClass());
            first.put("attendance", firstTerm.getAttendancePercentage());
            termSummaries.put("firstTerm", first);
        }

        if (secondTerm != null) {
            Map<String, Object> second = new HashMap<>();
            second.put("total", secondTerm.getTotalScore());
            second.put("average", secondTerm.getAverage());
            second.put("position", secondTerm.getPositionInClass());
            second.put("attendance", secondTerm.getAttendancePercentage());
            termSummaries.put("secondTerm", second);
        }

        if (thirdTerm != null) {
            Map<String, Object> third = new HashMap<>();
            third.put("total", thirdTerm.getTotalScore());
            third.put("average", thirdTerm.getAverage());
            third.put("position", thirdTerm.getPositionInClass());
            third.put("attendance", thirdTerm.getAttendancePercentage());
            termSummaries.put("thirdTerm", third);
        }

        report.put("termSummaries", termSummaries);

        List<Result> firstTermResults = firstTerm == null
                ? List.of()
                : resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);

        List<Result> secondTermResults = secondTerm == null
                ? List.of()
                : resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);

        List<Result> thirdTermResults = thirdTerm == null
                ? List.of()
                : resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

        List<Map<String, Object>> subjectPerformance = new ArrayList<>();

        Map<String, Double> sortedSubjectAverages = new TreeMap<>(sessionResult.getSubjectAverages());

        for (Map.Entry<String, Double> entry : sortedSubjectAverages.entrySet()) {
            String subjectName = entry.getKey();

            Map<String, Object> subject = new HashMap<>();
            subject.put("subject", subjectName);
            subject.put("annualAverage", entry.getValue());

            Map<String, Double> termScores = new HashMap<>();

            firstTermResults.stream()
                    .filter(r -> r.getSubject() != null
                            && r.getSubject().getName() != null
                            && r.getSubject().getName().equalsIgnoreCase(subjectName))
                    .findFirst()
                    .ifPresent(r -> termScores.put("first", safeDouble(r.getTotal())));

            secondTermResults.stream()
                    .filter(r -> r.getSubject() != null
                            && r.getSubject().getName() != null
                            && r.getSubject().getName().equalsIgnoreCase(subjectName))
                    .findFirst()
                    .ifPresent(r -> termScores.put("second", safeDouble(r.getTotal())));

            thirdTermResults.stream()
                    .filter(r -> r.getSubject() != null
                            && r.getSubject().getName() != null
                            && r.getSubject().getName().equalsIgnoreCase(subjectName))
                    .findFirst()
                    .ifPresent(r -> termScores.put("third", safeDouble(r.getTotal())));

            subject.put("termScores", termScores);
            subjectPerformance.add(subject);
        }

        report.put("subjectPerformance", subjectPerformance);

        Map<String, Object> annualSummary = new HashMap<>();
        annualSummary.put("firstTermTotal", sessionResult.getFirstTermTotal());
        annualSummary.put("secondTermTotal", sessionResult.getSecondTermTotal());
        annualSummary.put("thirdTermTotal", sessionResult.getThirdTermTotal());
        annualSummary.put("annualTotal", sessionResult.getAnnualTotal());
        annualSummary.put("annualAverage", sessionResult.getAnnualAverage());
        annualSummary.put("positionInClass", sessionResult.getAnnualPositionInClass());
        annualSummary.put("positionInArm", sessionResult.getAnnualPositionInArm());
        annualSummary.put("positionInSchool", sessionResult.getAnnualPositionInSchool());
        report.put("annualSummary", annualSummary);

        Map<String, Object> attendanceSummary = new HashMap<>();
        attendanceSummary.put("totalSchoolDays", sessionResult.getTotalSchoolDays());
        attendanceSummary.put("daysPresent", sessionResult.getTotalDaysPresent());
        attendanceSummary.put("daysAbsent", sessionResult.getTotalDaysAbsent());
        attendanceSummary.put("attendancePercentage", sessionResult.getAttendancePercentage());
        report.put("attendance", attendanceSummary);

        Map<String, Object> promotion = new HashMap<>();
        promotion.put("promoted", sessionResult.isPromoted());
        promotion.put("remark", sessionResult.getPromotionRemark());
        report.put("promotion", promotion);

        return report;
    }

    @Override
    public Map<String, Object> getSessionStatistics(String session) {
        List<SessionResult> allResults = sessionResultRepository.findBySessionOrderByAnnualAverageDesc(session);

        if (allResults.isEmpty()) {
            return Map.of("message", "No session results found for " + session);
        }

        double totalAverage = allResults.stream().mapToDouble(SessionResult::getAnnualAverage).average().orElse(0);
        double highestAverage = allResults.stream().mapToDouble(SessionResult::getAnnualAverage).max().orElse(0);
        double lowestAverage = allResults.stream().mapToDouble(SessionResult::getAnnualAverage).min().orElse(0);

        List<Object[]> classAverages = sessionResultRepository.getClassAverageBySession(session);

        Map<String, Double> classPerformance = new HashMap<>();
        for (Object[] ca : classAverages) {
            classPerformance.put((String) ca[0], (Double) ca[1]);
        }

        long promoted = sessionResultRepository.countPromotedStudents(session);
        long retained = sessionResultRepository.countRetainedStudents(session);

        Map<String, Long> gradeDistribution = new HashMap<>();
        for (SessionResult sr : allResults) {
            String grade = getGradeFromAverage(sr.getAnnualAverage());
            gradeDistribution.merge(grade, 1L, Long::sum);
        }

        long excellentAttendance = allResults.stream().filter(sr -> sr.getAttendancePercentage() >= 90).count();
        long goodAttendance = allResults.stream().filter(sr -> sr.getAttendancePercentage() >= 75 && sr.getAttendancePercentage() < 90).count();
        long poorAttendance = allResults.stream().filter(sr -> sr.getAttendancePercentage() < 75).count();

        Map<String, Object> attendanceStats = new HashMap<>();
        attendanceStats.put("averageAttendance", allResults.stream().mapToDouble(SessionResult::getAttendancePercentage).average().orElse(0));
        attendanceStats.put("excellentAttendance", excellentAttendance);
        attendanceStats.put("goodAttendance", goodAttendance);
        attendanceStats.put("poorAttendance", poorAttendance);

        List<Map<String, Object>> topPerformers = new ArrayList<>();
        for (int i = 0; i < Math.min(3, allResults.size()); i++) {
            SessionResult sr = allResults.get(i);
            Student student = sr.getStudent();
            Map<String, Object> performer = new HashMap<>();
            performer.put("studentName", student.getFirstName() + " " + student.getLastName());
            performer.put("admissionNumber", student.getAdmissionNumber());
            performer.put("studentClass", student.getStudentClass());
            performer.put("classArm", student.getClassArm());
            performer.put("annualAverage", sr.getAnnualAverage());
            topPerformers.add(performer);
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("session", session);
        statistics.put("totalStudents", allResults.size());
        statistics.put("overallAverage", totalAverage);
        statistics.put("highestAverage", highestAverage);
        statistics.put("lowestAverage", lowestAverage);
        statistics.put("promoted", promoted);
        statistics.put("retained", retained);
        statistics.put("promotionRate", (promoted * 100.0 / allResults.size()));
        statistics.put("classPerformance", classPerformance);
        statistics.put("gradeDistribution", gradeDistribution);
        statistics.put("attendanceStats", attendanceStats);
        statistics.put("topPerformers", topPerformers);

        return statistics;
    }

    @Override
    public Map<String, Object> promoteStudents(String session) {
        log.info("Promoting students based on session results: {}", session);

        List<SessionResult> allResults = sessionResultRepository.findBySessionOrderByAnnualAverageDesc(session);

        int promoted = 0;
        int retained = 0;
        int graduated = 0;
        List<Map<String, String>> promotionDetails = new ArrayList<>();

        for (SessionResult sr : allResults) {
            Student student = sr.getStudent();
            String currentClass = student.getStudentClass();
            String nextClass = getNextClass(currentClass);

            Map<String, String> detail = new HashMap<>();
            detail.put("studentId", student.getId().toString());
            detail.put("studentName", student.getFirstName() + " " + student.getLastName());
            detail.put("currentClass", currentClass);
            detail.put("annualAverage", String.format("%.2f", sr.getAnnualAverage()));
            detail.put("attendance", String.format("%.2f", sr.getAttendancePercentage()));

            if (sr.isPromoted()) {
                if ("GRADUATED".equals(nextClass)) {
                    student.setStatus(Student.StudentStatus.GRADUATED);
                    detail.put("status", "GRADUATED");
                    detail.put("nextClass", "GRADUATED");
                    graduated++;
                } else {
                    SchoolClass nextSchoolClass = classRepository
                            .findByClassNameAndArmNormalized(nextClass, student.getClassArm())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Class not found for promotion: " + nextClass + " " + student.getClassArm()
                            ));

                    student.setSchoolClass(nextSchoolClass);
                    detail.put("status", "PROMOTED");
                    detail.put("nextClass", nextSchoolClass.getClassName());
                    promoted++;
                }
            } else {
                detail.put("status", "RETAINED");
                detail.put("nextClass", currentClass);
                retained++;
            }

            promotionDetails.add(detail);
            studentRepository.save(student);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("promoted", promoted);
        result.put("retained", retained);
        result.put("graduated", graduated);
        result.put("total", allResults.size());
        result.put("details", promotionDetails);
        result.put("session", session);

        return result;
    }

    @Override
    public List<Map<String, Object>> getGraduationList(String session) {
        List<SessionResult> allResults = sessionResultRepository.findBySessionOrderByAnnualAverageDesc(session);

        List<Map<String, Object>> graduates = new ArrayList<>();

        for (SessionResult sr : allResults) {
            Student student = sr.getStudent();
            if (isSeniorFinalClass(student.getStudentClass()) && sr.isPromoted()) {
                Map<String, Object> grad = new HashMap<>();
                grad.put("studentId", student.getId());
                grad.put("studentName", student.getFirstName() + " " + student.getLastName());
                grad.put("admissionNumber", student.getAdmissionNumber());
                grad.put("finalAverage", sr.getAnnualAverage());
                grad.put("attendance", sr.getAttendancePercentage());
                grad.put("position", sr.getAnnualPositionInClass());
                grad.put("subjectAverages", new HashMap<>(sr.getSubjectAverages()));
                graduates.add(grad);
            }
        }

        return graduates;
    }

    private void populateAttendance(SessionResult sessionResult,
                                    Student student,
                                    String session,
                                    TermResult firstTerm,
                                    TermResult secondTerm,
                                    TermResult thirdTerm) {

        List<Attendance> allAttendance = new ArrayList<>();

        if (firstTerm != null) {
            allAttendance.addAll(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, Result.Term.FIRST
            ));
        }
        if (secondTerm != null) {
            allAttendance.addAll(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, Result.Term.SECOND
            ));
        }
        if (thirdTerm != null) {
            allAttendance.addAll(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, Result.Term.THIRD
            ));
        }

        Set<String> uniqueSchoolDays = new HashSet<>();
        int totalPresentEquivalent = 0;
        int totalAbsent = 0;

        for (Attendance att : allAttendance) {
            uniqueSchoolDays.add(att.getTerm() + "_" + att.getDate());

            if (att.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || att.getStatus() == Attendance.AttendanceStatus.LATE
                    || att.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                totalPresentEquivalent++;
            } else if (att.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                totalAbsent++;
            }
        }

        int totalSchoolDays = uniqueSchoolDays.size();

        sessionResult.setTotalSchoolDays(totalSchoolDays);
        sessionResult.setTotalDaysPresent(totalPresentEquivalent);
        sessionResult.setTotalDaysAbsent(totalAbsent);
        sessionResult.setAttendancePercentage(
                totalSchoolDays > 0 ? (totalPresentEquivalent * 100.0 / totalSchoolDays) : 0
        );
    }

    private void populateSubjectPerformance(SessionResult sessionResult, Student student, String session) {
        List<Result> first = resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);
        List<Result> second = resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);
        List<Result> third = resultRepository.findByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

        List<Result> allResults = new ArrayList<>();
        allResults.addAll(first);
        allResults.addAll(second);
        allResults.addAll(third);

        Map<String, Double> subjectAnnualTotals = new HashMap<>();
        Map<String, Integer> subjectCount = new HashMap<>();

        for (Result result : allResults) {
            if (result.getSubject() == null || result.getSubject().getName() == null) {
                continue;
            }

            String subjectName = result.getSubject().getName();
            subjectAnnualTotals.merge(subjectName, safeDouble(result.getTotal()), Double::sum);
            subjectCount.merge(subjectName, 1, Integer::sum);
        }

        Map<String, Double> subjectAverages = new HashMap<>();
        for (Map.Entry<String, Double> entry : subjectAnnualTotals.entrySet()) {
            String subject = entry.getKey();
            int count = subjectCount.getOrDefault(subject, 0);
            subjectAverages.put(subject, count > 0 ? (entry.getValue() / count) : 0);
        }

        sessionResult.setSubjectAnnualTotals(subjectAnnualTotals);
        sessionResult.setSubjectAverages(subjectAverages);
    }

    private void populateAnnualSummary(SessionResult sessionResult,
                                       TermResult firstTerm,
                                       TermResult secondTerm,
                                       TermResult thirdTerm) {

        double annualTotal = 0;
        double averageSum = 0;
        int termCount = 0;

        if (firstTerm != null) {
            annualTotal += safeDouble(firstTerm.getTotalScore());
            averageSum += safeDouble(firstTerm.getAverage());
            termCount++;
        }
        if (secondTerm != null) {
            annualTotal += safeDouble(secondTerm.getTotalScore());
            averageSum += safeDouble(secondTerm.getAverage());
            termCount++;
        }
        if (thirdTerm != null) {
            annualTotal += safeDouble(thirdTerm.getTotalScore());
            averageSum += safeDouble(thirdTerm.getAverage());
            termCount++;
        }

        sessionResult.setAnnualTotal(annualTotal);
        sessionResult.setAnnualAverage(termCount > 0 ? (averageSum / termCount) : 0);
    }

    private void applyPromotionDecision(SessionResult sessionResult) {
        boolean promoted = sessionResult.getAnnualAverage() >= 50
                && sessionResult.getAttendancePercentage() >= 50;

        sessionResult.setPromoted(promoted);

        if (promoted) {
            sessionResult.setPromotionRemark("Promoted");
        } else if (sessionResult.getAnnualAverage() < 50) {
            sessionResult.setPromotionRemark("Retained due to low academic performance");
        } else {
            sessionResult.setPromotionRemark("Retained due to poor attendance");
        }
    }

    private void calculateAnnualPositions(SessionResult sessionResult) {
        String className = sessionResult.getStudent().getStudentClass();
        String arm = sessionResult.getStudent().getClassArm();
        String session = sessionResult.getSession();

        List<SessionResult> classResults = sessionResultRepository.findByClassAndSessionOrderByAnnualAverageDesc(className, session);

        for (int i = 0; i < classResults.size(); i++) {
            SessionResult sr = classResults.get(i);
            sr.setAnnualPositionInClass(i + 1);
            if (sr.getId().equals(sessionResult.getId())) {
                sessionResult.setAnnualPositionInClass(i + 1);
            }
        }

        if (arm != null && !arm.isEmpty()) {
            List<SessionResult> armResults = sessionResultRepository.findByClassAndArmAndSessionOrderByAnnualAverageDesc(className, arm, session);

            for (int i = 0; i < armResults.size(); i++) {
                SessionResult sr = armResults.get(i);
                sr.setAnnualPositionInArm(i + 1);
                if (sr.getId().equals(sessionResult.getId())) {
                    sessionResult.setAnnualPositionInArm(i + 1);
                }
            }

            sessionResultRepository.saveAll(armResults);
        }

        List<SessionResult> schoolResults = sessionResultRepository.findBySessionOrderByAnnualAverageDesc(session);

        for (int i = 0; i < schoolResults.size(); i++) {
            SessionResult sr = schoolResults.get(i);
            sr.setAnnualPositionInSchool(i + 1);
            if (sr.getId().equals(sessionResult.getId())) {
                sessionResult.setAnnualPositionInSchool(i + 1);
            }
        }

        sessionResultRepository.saveAll(classResults);
        sessionResultRepository.saveAll(schoolResults);
    }

    private void calculateAllPositions(String session) {
        List<SessionResult> schoolResults = sessionResultRepository.findBySessionOrderByAnnualAverageDesc(session);

        for (int i = 0; i < schoolResults.size(); i++) {
            schoolResults.get(i).setAnnualPositionInSchool(i + 1);
        }

        Map<String, List<SessionResult>> byClass = schoolResults.stream()
                .collect(Collectors.groupingBy(sr -> sr.getStudent().getStudentClass()));

        for (Map.Entry<String, List<SessionResult>> entry : byClass.entrySet()) {
            List<SessionResult> classResults = entry.getValue();
            classResults.sort((a, b) -> Double.compare(b.getAnnualAverage(), a.getAnnualAverage()));

            for (int i = 0; i < classResults.size(); i++) {
                classResults.get(i).setAnnualPositionInClass(i + 1);
            }
        }

        Map<String, List<SessionResult>> byArm = schoolResults.stream()
                .filter(sr -> sr.getStudent().getClassArm() != null)
                .collect(Collectors.groupingBy(
                        sr -> sr.getStudent().getStudentClass() + "_" + sr.getStudent().getClassArm()
                ));

        for (Map.Entry<String, List<SessionResult>> entry : byArm.entrySet()) {
            List<SessionResult> armResults = entry.getValue();
            armResults.sort((a, b) -> Double.compare(b.getAnnualAverage(), a.getAnnualAverage()));

            for (int i = 0; i < armResults.size(); i++) {
                armResults.get(i).setAnnualPositionInArm(i + 1);
            }
        }

        sessionResultRepository.saveAll(schoolResults);
    }

    private String getGradeFromAverage(double average) {
        if (average >= 70) return "A";
        if (average >= 60) return "B";
        if (average >= 50) return "C";
        if (average >= 45) return "D";
        if (average >= 40) return "E";
        return "F";
    }

    private String getNextClass(String currentClass) {
        if (currentClass == null) {
            return null;
        }

        String normalized = currentClass.trim().replaceAll("\\s+", "").toLowerCase();

        Map<String, String> progression = new HashMap<>();
        progression.put("nursery1", "nursery2");
        progression.put("nursery2", "kindergarten1");
        progression.put("kindergarten1", "kindergarten2");
        progression.put("kindergarten2", "primary1");
        progression.put("primary1", "primary2");
        progression.put("primary2", "primary3");
        progression.put("primary3", "primary4");
        progression.put("primary4", "primary5");
        progression.put("primary5", "primary6");
        progression.put("primary6", "jss1");
        progression.put("jss1", "jss2");
        progression.put("jss2", "jss3");
        progression.put("jss3", "ss1");
        progression.put("sss1", "ss2");
        progression.put("ss1", "ss2");
        progression.put("sss2", "ss3");
        progression.put("ss2", "ss3");
        progression.put("sss3", "GRADUATED");
        progression.put("ss3", "GRADUATED");

        return progression.getOrDefault(normalized, currentClass);
    }

    private boolean isSeniorFinalClass(String currentClass) {
        if (currentClass == null) {
            return false;
        }
        String normalized = currentClass.trim().replaceAll("\\s+", "").toLowerCase();
        return normalized.equals("sss3") || normalized.equals("ss3");
    }

    private double safeDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}