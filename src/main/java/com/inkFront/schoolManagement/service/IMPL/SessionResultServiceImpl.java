package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ResultRepository;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.service.SessionResultService;
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
public class SessionResultServiceImpl implements SessionResultService {

    private final StudentRepository studentRepository;
    private final TermResultRepository termResultRepository;
    private final SessionResultRepository sessionResultRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;

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

        int totalSchoolDays = 0;
        int totalDaysPresent = 0;
        int totalDaysAbsent = 0;

        List<Attendance> allAttendance = new ArrayList<>();

        if (firstTerm != null) {
            allAttendance.addAll(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, Result.Term.FIRST));
        }
        if (secondTerm != null) {
            allAttendance.addAll(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, Result.Term.SECOND));
        }
        if (thirdTerm != null) {
            allAttendance.addAll(attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, Result.Term.THIRD));
        }

        Map<LocalDate, Boolean> uniqueDays = new HashMap<>();
        for (Attendance att : allAttendance) {
            uniqueDays.put(att.getDate(), true);

            if (att.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || att.getStatus() == Attendance.AttendanceStatus.LATE
                    || att.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                totalDaysPresent++;
            } else if (att.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                totalDaysAbsent++;
            }
        }
        totalSchoolDays = uniqueDays.size();

        Map<String, Double> subjectAnnualTotals = new HashMap<>();
        Map<String, Integer> subjectCount = new HashMap<>();

        List<Result> allResults = new ArrayList<>();
        if (firstTerm != null && firstTerm.getSubjectResults() != null) {
            allResults.addAll(firstTerm.getSubjectResults());
        }
        if (secondTerm != null && secondTerm.getSubjectResults() != null) {
            allResults.addAll(secondTerm.getSubjectResults());
        }
        if (thirdTerm != null && thirdTerm.getSubjectResults() != null) {
            allResults.addAll(thirdTerm.getSubjectResults());
        }

        for (Result result : allResults) {
            String subject = result.getSubject().getName();
            subjectAnnualTotals.merge(subject, result.getTotal(), Double::sum);
            subjectCount.merge(subject, 1, Integer::sum);
        }

        Map<String, Double> subjectAverages = new HashMap<>();
        for (Map.Entry<String, Double> entry : subjectAnnualTotals.entrySet()) {
            String subject = entry.getKey();
            Double total = entry.getValue();
            Integer count = subjectCount.get(subject);
            if (count != null && count > 0) {
                subjectAverages.put(subject, total / count);
            }
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
            sessionResult.setFirstTermTotal(firstTerm.getTotalScore());
            sessionResult.setFirstTermAverage(firstTerm.getAverage());
            sessionResult.setFirstTermPosition(firstTerm.getPositionInClass());
        }

        if (secondTerm != null) {
            sessionResult.setSecondTermTotal(secondTerm.getTotalScore());
            sessionResult.setSecondTermAverage(secondTerm.getAverage());
            sessionResult.setSecondTermPosition(secondTerm.getPositionInClass());
        }

        if (thirdTerm != null) {
            sessionResult.setThirdTermTotal(thirdTerm.getTotalScore());
            sessionResult.setThirdTermAverage(thirdTerm.getAverage());
            sessionResult.setThirdTermPosition(thirdTerm.getPositionInClass());
        }

        sessionResult.setTotalSchoolDays(totalSchoolDays);
        sessionResult.setTotalDaysPresent(totalDaysPresent);
        sessionResult.setTotalDaysAbsent(totalDaysAbsent);
        sessionResult.setAttendancePercentage(totalSchoolDays > 0
                ? (totalDaysPresent * 100.0 / totalSchoolDays)
                : 0);

        sessionResult.setSubjectAnnualTotals(subjectAnnualTotals);
        sessionResult.setSubjectAverages(subjectAverages);

        sessionResult.calculateAnnualAverage();

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
    @Transactional(readOnly = true)
    public SessionResultResponseDTO getSessionResult(Long studentId, String session) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        SessionResult result = sessionResultRepository.findByStudentAndSession(student, session)
                .orElseThrow(() -> new RuntimeException("Session result not found"));

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

        List<Map<String, Object>> subjectPerformance = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sessionResult.getSubjectAverages().entrySet()) {
            Map<String, Object> subject = new HashMap<>();
            subject.put("subject", entry.getKey());
            subject.put("annualAverage", entry.getValue());

            Map<String, Double> termScores = new HashMap<>();
            if (firstTerm != null && firstTerm.getSubjectResults() != null) {
                firstTerm.getSubjectResults().stream()
                        .filter(r -> r.getSubject().equals(entry.getKey()))
                        .findFirst()
                        .ifPresent(r -> termScores.put("first", r.getTotal()));
            }
            if (secondTerm != null && secondTerm.getSubjectResults() != null) {
                secondTerm.getSubjectResults().stream()
                        .filter(r -> r.getSubject().equals(entry.getKey()))
                        .findFirst()
                        .ifPresent(r -> termScores.put("second", r.getTotal()));
            }
            if (thirdTerm != null && thirdTerm.getSubjectResults() != null) {
                thirdTerm.getSubjectResults().stream()
                        .filter(r -> r.getSubject().equals(entry.getKey()))
                        .findFirst()
                        .ifPresent(r -> termScores.put("third", r.getTotal()));
            }

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
                    student.setStudentClass(nextClass);
                    detail.put("status", "PROMOTED");
                    detail.put("nextClass", nextClass);
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
            if ("SSS 3".equals(student.getStudentClass()) && sr.isPromoted()) {
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
        Map<String, String> progression = new HashMap<>();
        progression.put("Nursery 1", "Nursery 2");
        progression.put("Nursery 2", "Kindergarten 1");
        progression.put("Kindergarten 1", "Kindergarten 2");
        progression.put("Kindergarten 2", "Primary 1");
        progression.put("Primary 1", "Primary 2");
        progression.put("Primary 2", "Primary 3");
        progression.put("Primary 3", "Primary 4");
        progression.put("Primary 4", "Primary 5");
        progression.put("Primary 5", "Primary 6");
        progression.put("Primary 6", "JSS 1");
        progression.put("JSS 1", "JSS 2");
        progression.put("JSS 2", "JSS 3");
        progression.put("JSS 3", "SSS 1");
        progression.put("SSS 1", "SSS 2");
        progression.put("SSS 2", "SSS 3");
        progression.put("SSS 3", "GRADUATED");

        return progression.getOrDefault(currentClass, currentClass);
    }
}