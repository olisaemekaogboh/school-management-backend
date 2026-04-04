package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ResultVisibilityUpdateDTO;
import com.inkFront.schoolManagement.dto.SessionResultResponseDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.ResultVisibilityStatus;
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
                .findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.FIRST)
                .orElse(null);

        TermResult secondTerm = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.SECOND)
                .orElse(null);

        TermResult thirdTerm = termResultRepository
                .findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.THIRD)
                .orElse(null);

        if (firstTerm == null && secondTerm == null && thirdTerm == null) {
            throw new RuntimeException("No term result found for this student in " + session);
        }

        SessionResult sessionResult = sessionResultRepository
                .findDetailedByStudentAndSession(student, session)
                .orElse(new SessionResult());

        sessionResult.setStudent(student);
        sessionResult.setSession(session);

        sessionResult.setFirstTermTotal(0.0);
        sessionResult.setSecondTermTotal(0.0);
        sessionResult.setThirdTermTotal(0.0);

        sessionResult.setFirstTermAverage(0.0);
        sessionResult.setSecondTermAverage(0.0);
        sessionResult.setThirdTermAverage(0.0);

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
        syncVisibilityFromTerms(sessionResult, firstTerm, secondTerm, thirdTerm);

        SessionResult savedResult = sessionResultRepository.save(sessionResult);

        calculateAnnualPositions(savedResult);

        SessionResult fresh = sessionResultRepository.findDetailedById(savedResult.getId())
                .orElse(savedResult);

        return SessionResultResponseDTO.fromEntity(fresh);
    }

    @Override
    public SessionResultResponseDTO setSessionResultPrintableStatus(
            Long studentId,
            String session,
            boolean printable,
            String printLockMessage
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        SessionResult sessionResult = sessionResultRepository
                .findDetailedByStudentAndSession(student, session)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session result not found for student ID " + studentId + " in session " + session
                ));

        if (printable) {
            sessionResult.markPrintable(
                    printLockMessage,
                    sessionResult.getPublishedByName()
            );
        } else {
            if (sessionResult.getResultVisibilityStatus() == ResultVisibilityStatus.PRINTABLE) {
                sessionResult.markPublished(
                        sessionResult.getVisibilityMessage(),
                        sessionResult.getPublishedByName()
                );
            }
            sessionResult.setPrintable(false);
            sessionResult.setPrintLockMessage(
                    hasText(printLockMessage)
                            ? printLockMessage.trim()
                            : "Printable result is locked. The admin will unlock it when the result is ready."
            );
        }

        SessionResult saved = sessionResultRepository.save(sessionResult);
        return SessionResultResponseDTO.fromEntity(saved);
    }

    @Override
    public SessionResultResponseDTO updateSessionVisibility(
            Long studentId,
            String session,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        SessionResult sessionResult = sessionResultRepository
                .findDetailedByStudentAndSession(student, session)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session result not found for student ID " + studentId + " in session " + session
                ));

        if (request == null || request.getVisibilityStatus() == null) {
            throw new RuntimeException("Visibility status is required");
        }

        switch (request.getVisibilityStatus()) {
            case HIDDEN -> sessionResult.markHidden(request.getVisibilityMessage());
            case STAFF_ONLY -> sessionResult.markStaffOnly(request.getVisibilityMessage());
            case PUBLISHED -> sessionResult.markPublished(request.getVisibilityMessage(), publishedByName);
            case PRINTABLE -> sessionResult.markPrintable(request.getVisibilityMessage(), publishedByName);
        }

        SessionResult saved = sessionResultRepository.save(sessionResult);
        return SessionResultResponseDTO.fromEntity(saved);
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
                log.error("Error calculating session result for student {}: {}", student.getId(), e.getMessage(), e);
            }
        }

        calculateAllPositions(session);
        return results;
    }

    @Override
    public List<SessionResultResponseDTO> calculateClassArmSessionResults(String className, String arm, String session) {
        log.info("Calculating class arm session results for class: {} arm: {} session: {}", className, arm, session);

        List<Student> students = studentRepository.findByStudentClassAndClassArmNormalized(className, arm);

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

        SessionResult result = sessionResultRepository.findDetailedByStudentAndSession(student, session)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session result not found for student ID " + studentId + " in session " + session
                ));

        return SessionResultResponseDTO.fromEntity(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResultResponseDTO> getClassSessionResults(String className, String session) {
        return sessionResultRepository.findDetailedByClassAndSessionOrderByAnnualAverageDesc(className, session)
                .stream()
                .map(SessionResultResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResultResponseDTO> getArmSessionResults(String className, String arm, String session) {
        return sessionResultRepository.findDetailedByClassAndArmAndSessionOrderByAnnualAverageDesc(className, arm, session)
                .stream()
                .map(SessionResultResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public Map<String, Object> getSchoolSessionRankings(String session) {
        List<SessionResult> rankings = sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc(session);

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
        stats.put("promotionRate", resultList.isEmpty() ? 0.0 : (promoted * 100.0 / resultList.size()));

        response.put("statistics", stats);
        return response;
    }

    @Override
    public Map<String, Object> getClassRankings(String className, String session) {
        List<SessionResult> rankings = sessionResultRepository.findDetailedByClassAndSessionOrderByAnnualAverageDesc(className, session);

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
        List<SessionResult> rankings = sessionResultRepository.findDetailedByClassAndArmAndSessionOrderByAnnualAverageDesc(className, arm, session);

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

        SessionResult sessionResult = sessionResultRepository.findDetailedByStudentAndSession(student, session)
                .orElseThrow(() -> new RuntimeException("Session result not found"));

        TermResult firstTerm = termResultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.FIRST).orElse(null);
        TermResult secondTerm = termResultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.SECOND).orElse(null);
        TermResult thirdTerm = termResultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.THIRD).orElse(null);

        Map<String, Object> report = new HashMap<>();

        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("id", student.getId());
        studentInfo.put("name", (student.getFirstName() + " " + student.getLastName()).trim());
        studentInfo.put("fullName", (student.getFirstName() + " " + student.getLastName()).trim());
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
        report.put("studentInfo", studentInfo);

        Map<String, Object> termSummaries = new HashMap<>();

        if (firstTerm != null) {
            Map<String, Object> first = new HashMap<>();
            first.put("total", firstTerm.getTotalScore());
            first.put("average", firstTerm.getAverage());
            first.put("position", firstTerm.getPositionInClass());
            first.put("attendance", firstTerm.getAttendancePercentage());
            first.put("visibilityStatus", firstTerm.getVisibilityStatus() != null ? firstTerm.getVisibilityStatus().name() : null);
            termSummaries.put("firstTerm", first);
        }

        if (secondTerm != null) {
            Map<String, Object> second = new HashMap<>();
            second.put("total", secondTerm.getTotalScore());
            second.put("average", secondTerm.getAverage());
            second.put("position", secondTerm.getPositionInClass());
            second.put("attendance", secondTerm.getAttendancePercentage());
            second.put("visibilityStatus", secondTerm.getVisibilityStatus() != null ? secondTerm.getVisibilityStatus().name() : null);
            termSummaries.put("secondTerm", second);
        }

        if (thirdTerm != null) {
            Map<String, Object> third = new HashMap<>();
            third.put("total", thirdTerm.getTotalScore());
            third.put("average", thirdTerm.getAverage());
            third.put("position", thirdTerm.getPositionInClass());
            third.put("attendance", thirdTerm.getAttendancePercentage());
            third.put("visibilityStatus", thirdTerm.getVisibilityStatus() != null ? thirdTerm.getVisibilityStatus().name() : null);
            termSummaries.put("thirdTerm", third);
        }

        report.put("termSummaries", termSummaries);

        List<Result> firstTermResults = firstTerm == null
                ? List.of()
                : resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);

        List<Result> secondTermResults = secondTerm == null
                ? List.of()
                : resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);

        List<Result> thirdTermResults = thirdTerm == null
                ? List.of()
                : resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

        List<Map<String, Object>> subjectPerformance = new ArrayList<>();

        Map<String, Double> sortedSubjectAverages = new TreeMap<>(
                sessionResult.getSubjectAverages() != null ? sessionResult.getSubjectAverages() : Collections.emptyMap()
        );

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
        annualSummary.put("promoted", sessionResult.isPromoted());
        annualSummary.put("remark", sessionResult.getPromotionRemark());
        annualSummary.put("subjectAverages", sessionResult.getSubjectAverages());
        annualSummary.put("resultVisibilityStatus", sessionResult.getResultVisibilityStatus() != null ? sessionResult.getResultVisibilityStatus().name() : null);
        annualSummary.put("visibilityMessage", sessionResult.getVisibilityMessage());
        annualSummary.put("printable", sessionResult.isPrintable());
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

        report.put("subjectAverages", sessionResult.getSubjectAverages());
        report.put("subjectAnnualTotals", sessionResult.getSubjectAnnualTotals());
        report.put("resultVisibilityStatus", sessionResult.getResultVisibilityStatus() != null ? sessionResult.getResultVisibilityStatus().name() : null);
        report.put("visibilityMessage", sessionResult.getVisibilityMessage());
        report.put("printable", sessionResult.isPrintable());
        report.put("printLockMessage", sessionResult.getPrintLockMessage());
        report.put("publishedAt", sessionResult.getPublishedAt());
        report.put("publishedByName", sessionResult.getPublishedByName());

        return report;
    }

    @Override
    public Map<String, Object> getSessionStatistics(String session) {
        List<SessionResult> allResults = sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc(session);

        if (allResults.isEmpty()) {
            return Map.of("message", "No session results found for " + session);
        }

        double totalAverage = allResults.stream().mapToDouble(SessionResult::getAnnualAverage).average().orElse(0.0);
        double highestAverage = allResults.stream().mapToDouble(SessionResult::getAnnualAverage).max().orElse(0.0);
        double lowestAverage = allResults.stream().mapToDouble(SessionResult::getAnnualAverage).min().orElse(0.0);

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
        attendanceStats.put("averageAttendance", allResults.stream().mapToDouble(SessionResult::getAttendancePercentage).average().orElse(0.0));
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

        List<SessionResult> allResults = sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc(session);

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
        List<SessionResult> allResults = sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc(session);

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

        for (Attendance attendance : allAttendance) {
            if (attendance.getDate() == null || attendance.getTerm() == null) {
                continue;
            }

            uniqueSchoolDays.add(attendance.getTerm().name() + "_" + attendance.getDate());

            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || attendance.getStatus() == Attendance.AttendanceStatus.LATE
                    || attendance.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                totalPresentEquivalent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                totalAbsent++;
            }
        }

        if (uniqueSchoolDays.isEmpty()) {
            if (firstTerm != null) {
                totalPresentEquivalent += firstTerm.getDaysPresent();
                totalAbsent += firstTerm.getDaysAbsent();
                uniqueSchoolDays.addAll(
                        attendanceRepository.findDistinctDatesBySessionAndTerm(session, Result.Term.FIRST)
                                .stream()
                                .map(date -> "FIRST_" + date)
                                .toList()
                );
            }
            if (secondTerm != null) {
                totalPresentEquivalent += secondTerm.getDaysPresent();
                totalAbsent += secondTerm.getDaysAbsent();
                uniqueSchoolDays.addAll(
                        attendanceRepository.findDistinctDatesBySessionAndTerm(session, Result.Term.SECOND)
                                .stream()
                                .map(date -> "SECOND_" + date)
                                .toList()
                );
            }
            if (thirdTerm != null) {
                totalPresentEquivalent += thirdTerm.getDaysPresent();
                totalAbsent += thirdTerm.getDaysAbsent();
                uniqueSchoolDays.addAll(
                        attendanceRepository.findDistinctDatesBySessionAndTerm(session, Result.Term.THIRD)
                                .stream()
                                .map(date -> "THIRD_" + date)
                                .toList()
                );
            }
        }

        int totalSchoolDays = uniqueSchoolDays.size();

        sessionResult.setTotalSchoolDays(totalSchoolDays);
        sessionResult.setTotalDaysPresent(totalPresentEquivalent);
        sessionResult.setTotalDaysAbsent(totalAbsent);
        sessionResult.setAttendancePercentage(
                totalSchoolDays > 0 ? (totalPresentEquivalent * 100.0 / totalSchoolDays) : 0.0
        );
    }

    private void populateSubjectPerformance(SessionResult sessionResult, Student student, String session) {
        List<Result> first = resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.FIRST);
        List<Result> second = resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.SECOND);
        List<Result> third = resultRepository.findDetailedByStudentAndSessionAndTerm(student, session, Result.Term.THIRD);

        Map<String, Double> subjectAnnualTotals = new HashMap<>();
        Map<String, Integer> subjectCount = new HashMap<>();
        Map<String, Double> firstTermSubjectScores = new HashMap<>();
        Map<String, Double> secondTermSubjectScores = new HashMap<>();
        Map<String, Double> thirdTermSubjectScores = new HashMap<>();

        for (Result result : first) {
            if (result.getSubject() == null || result.getSubject().getName() == null) {
                continue;
            }
            String subjectName = result.getSubject().getName();
            double total = safeDouble(result.getTotal());

            firstTermSubjectScores.put(subjectName, total);
            subjectAnnualTotals.merge(subjectName, total, Double::sum);
            subjectCount.merge(subjectName, 1, Integer::sum);
        }

        for (Result result : second) {
            if (result.getSubject() == null || result.getSubject().getName() == null) {
                continue;
            }
            String subjectName = result.getSubject().getName();
            double total = safeDouble(result.getTotal());

            secondTermSubjectScores.put(subjectName, total);
            subjectAnnualTotals.merge(subjectName, total, Double::sum);
            subjectCount.merge(subjectName, 1, Integer::sum);
        }

        for (Result result : third) {
            if (result.getSubject() == null || result.getSubject().getName() == null) {
                continue;
            }
            String subjectName = result.getSubject().getName();
            double total = safeDouble(result.getTotal());

            thirdTermSubjectScores.put(subjectName, total);
            subjectAnnualTotals.merge(subjectName, total, Double::sum);
            subjectCount.merge(subjectName, 1, Integer::sum);
        }

        Map<String, Double> subjectAverages = new HashMap<>();
        for (Map.Entry<String, Double> entry : subjectAnnualTotals.entrySet()) {
            String subjectName = entry.getKey();
            int count = subjectCount.getOrDefault(subjectName, 1);
            subjectAverages.put(subjectName, entry.getValue() / count);
        }

        sessionResult.setFirstTermSubjectScores(firstTermSubjectScores);
        sessionResult.setSecondTermSubjectScores(secondTermSubjectScores);
        sessionResult.setThirdTermSubjectScores(thirdTermSubjectScores);
        sessionResult.setSubjectAnnualTotals(subjectAnnualTotals);
        sessionResult.setSubjectAverages(subjectAverages);
    }

    private void populateAnnualSummary(
            SessionResult sessionResult,
            TermResult firstTerm,
            TermResult secondTerm,
            TermResult thirdTerm
    ) {
        double firstTotal = firstTerm != null ? safeDouble(firstTerm.getTotalScore()) : 0.0;
        double secondTotal = secondTerm != null ? safeDouble(secondTerm.getTotalScore()) : 0.0;
        double thirdTotal = thirdTerm != null ? safeDouble(thirdTerm.getTotalScore()) : 0.0;

        double firstAverage = firstTerm != null ? safeDouble(firstTerm.getAverage()) : 0.0;
        double secondAverage = secondTerm != null ? safeDouble(secondTerm.getAverage()) : 0.0;
        double thirdAverage = thirdTerm != null ? safeDouble(thirdTerm.getAverage()) : 0.0;

        sessionResult.setAnnualTotal(firstTotal + secondTotal + thirdTotal);

        int divisor = 0;
        if (firstTerm != null) divisor++;
        if (secondTerm != null) divisor++;
        if (thirdTerm != null) divisor++;

        sessionResult.setAnnualAverage(divisor == 0 ? 0.0 : (firstAverage + secondAverage + thirdAverage) / divisor);
    }

    private void applyPromotionDecision(SessionResult sessionResult) {
        boolean promoted = safeDouble(sessionResult.getAnnualAverage()) >= 40.0;
        sessionResult.setPromoted(promoted);
        sessionResult.setPromotionRemark(promoted ? "Promoted to next class" : "Not promoted");
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

    private void calculateAllPositions(String session) {
        List<SessionResult> schoolResults = sessionResultRepository.findDetailedBySessionOrderByAnnualAverageDesc(session);

        for (int i = 0; i < schoolResults.size(); i++) {
            SessionResult sr = schoolResults.get(i);
            sr.setAnnualPositionInSchool(i + 1);
        }

        sessionResultRepository.saveAll(schoolResults);

        Map<Long, List<SessionResult>> byClassId = new HashMap<>();
        for (SessionResult sr : schoolResults) {
            if (sr.getStudent() != null && sr.getStudent().getSchoolClass() != null) {
                Long classId = sr.getStudent().getSchoolClass().getId();
                byClassId.computeIfAbsent(classId, ignored -> new ArrayList<>()).add(sr);
            }
        }

        for (List<SessionResult> classResults : byClassId.values()) {
            classResults.sort(Comparator.comparingDouble(SessionResult::getAnnualAverage).reversed());
            for (int i = 0; i < classResults.size(); i++) {
                SessionResult sr = classResults.get(i);
                sr.setAnnualPositionInClass(i + 1);
                sr.setAnnualPositionInArm(i + 1);
            }
            sessionResultRepository.saveAll(classResults);
        }
    }

    private void syncVisibilityFromTerms(
            SessionResult sessionResult,
            TermResult firstTerm,
            TermResult secondTerm,
            TermResult thirdTerm
    ) {
        List<TermResult> terms = new ArrayList<>();
        if (firstTerm != null) terms.add(firstTerm);
        if (secondTerm != null) terms.add(secondTerm);
        if (thirdTerm != null) terms.add(thirdTerm);

        if (terms.isEmpty()) {
            sessionResult.resetPublicationState("Session result is awaiting term result preparation.");
            return;
        }

        boolean allPrintable = terms.stream()
                .allMatch(term -> term.getVisibilityStatus() == TermResult.VisibilityStatus.PRINTABLE && term.isPrintable());

        boolean anyFamilyVisible = terms.stream()
                .anyMatch(term -> term.getVisibilityStatus() == TermResult.VisibilityStatus.PUBLISHED
                        || term.getVisibilityStatus() == TermResult.VisibilityStatus.PRINTABLE);

        if (allPrintable) {
            String publisher = terms.stream()
                    .map(TermResult::getPublishedByName)
                    .filter(this::hasText)
                    .findFirst()
                    .orElse(null);
            sessionResult.markPrintable(
                    "Session result is printable because all term results are printable.",
                    publisher
            );
            return;
        }

        if (anyFamilyVisible) {
            String publisher = terms.stream()
                    .map(TermResult::getPublishedByName)
                    .filter(this::hasText)
                    .findFirst()
                    .orElse(null);
            sessionResult.markPublished(
                    "Session result is viewable because one or more term results have been released.",
                    publisher
            );
            return;
        }

        sessionResult.markStaffOnly("Session result is available to staff only until admin releases it.");
    }

    private double safeDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private String getGradeFromAverage(Double average) {
        double value = safeDouble(average);
        if (value >= 70) return "A";
        if (value >= 60) return "B";
        if (value >= 50) return "C";
        if (value >= 45) return "D";
        if (value >= 40) return "E";
        return "F";
    }

    private String getNextClass(String currentClass) {
        if (currentClass == null) return null;

        return switch (currentClass.trim().toUpperCase()) {
            case "NURSERY" -> "Primary 1";
            case "PRIMARY 1" -> "Primary 2";
            case "PRIMARY 2" -> "Primary 3";
            case "PRIMARY 3" -> "Primary 4";
            case "PRIMARY 4" -> "Primary 5";
            case "PRIMARY 5" -> "Primary 6";
            case "PRIMARY 6" -> "JSS 1";
            case "JSS 1" -> "JSS 2";
            case "JSS 2" -> "JSS 3";
            case "JSS 3" -> "SSS 1";
            case "SSS 1" -> "SSS 2";
            case "SSS 2" -> "SSS 3";
            case "SSS 3" -> "GRADUATED";
            default -> currentClass;
        };
    }

    private boolean isSeniorFinalClass(String studentClass) {
        return studentClass != null && "SSS 3".equalsIgnoreCase(studentClass.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}