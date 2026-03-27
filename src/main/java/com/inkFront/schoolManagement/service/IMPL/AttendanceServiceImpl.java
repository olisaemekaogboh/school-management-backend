package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.SessionResultRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TermResultRepository;
import com.inkFront.schoolManagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final TermResultRepository termResultRepository;
    private final SessionResultRepository sessionResultRepository;
    private final ClassRepository classRepository;

    @Override
    public Attendance markAttendance(Long studentId, LocalDate date, String session,
                                     Result.Term term, Attendance.AttendanceStatus status, String remarks) {

        log.info("Marking attendance for student: {}, date: {}, status: {}", studentId, date, status);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Attendance attendance = attendanceRepository
                .findByStudentAndDateAndSessionAndTerm(student, date, session, term)
                .orElse(new Attendance());

        attendance.setStudent(student);
        attendance.setDate(date);
        attendance.setSession(session);
        attendance.setTerm(term);
        attendance.setStatus(status);
        attendance.setRemarks(remarks);

        Attendance savedAttendance = attendanceRepository.save(attendance);

        updateTermAttendanceSummary(student, session, term);

        return savedAttendance;
    }

    @Override
    public List<Attendance> markBulkAttendance(List<Long> studentIds, LocalDate date,
                                               String session, Result.Term term,
                                               Attendance.AttendanceStatus status) {

        log.info("========== BULK ATTENDANCE DEBUG ==========");
        log.info("Student IDs: {}", studentIds);
        log.info("Date: {}", date);
        log.info("Session: {}", session);
        log.info("Term: {}", term);
        log.info("Status: {}", status);
        log.info("Student count: {}", studentIds.size());

        List<Attendance> attendances = new ArrayList<>();

        for (Long studentId : studentIds) {
            try {
                log.info("Processing student ID: {}", studentId);
                Attendance attendance = markAttendance(studentId, date, session, term, status, null);
                attendances.add(attendance);
                log.info("Successfully marked attendance for student: {}", studentId);
            } catch (Exception e) {
                log.error("Error marking attendance for student {}: {}", studentId, e.getMessage(), e);
            }
        }

        log.info("Completed bulk attendance. Successfully marked: {}", attendances.size());
        log.info("===========================================");

        return attendances;
    }

    @Override
    public Attendance getStudentAttendance(Long studentId, LocalDate date, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return attendanceRepository
                .findByStudentAndDateAndSessionAndTerm(student, date, session, term)
                .orElse(null);
    }

    @Override
    public List<Attendance> getStudentTermAttendance(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(student, session, term);
    }

    @Override
    public AttendanceSummary getStudentTermSummary(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        List<LocalDate> schoolDays = getSchoolDays(session, term);
        int totalSchoolDays = schoolDays.size();

        long present = attendanceRepository.countByStudentAndSessionAndTermAndStatus(
                student, session, term, Attendance.AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByStudentAndSessionAndTermAndStatus(
                student, session, term, Attendance.AttendanceStatus.ABSENT);
        long late = attendanceRepository.countByStudentAndSessionAndTermAndStatus(
                student, session, term, Attendance.AttendanceStatus.LATE);
        long excused = attendanceRepository.countByStudentAndSessionAndTermAndStatus(
                student, session, term, Attendance.AttendanceStatus.EXCUSED);

        AttendanceSummary summary = new AttendanceSummary();
        summary.setStudent(student);
        summary.setSession(session);
        summary.setTerm(term);
        summary.setTotalSchoolDays(totalSchoolDays);
        summary.setDaysPresent((int) present);
        summary.setDaysAbsent((int) absent);
        summary.setDaysLate((int) late);
        summary.setDaysExcused((int) excused);
        summary.calculatePercentage();
        return summary;
    }

    @Override
    public Map<String, Object> getStudentSessionSummary(Long studentId, String session) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        AttendanceSummary firstTerm = getStudentTermSummary(studentId, session, Result.Term.FIRST);
        AttendanceSummary secondTerm = getStudentTermSummary(studentId, session, Result.Term.SECOND);
        AttendanceSummary thirdTerm = getStudentTermSummary(studentId, session, Result.Term.THIRD);

        int totalDays = 0;
        int totalPresent = 0;
        int totalAbsent = 0;

        if (firstTerm != null) {
            totalDays += firstTerm.getTotalSchoolDays();
            totalPresent += firstTerm.getDaysPresent();
            totalAbsent += firstTerm.getDaysAbsent();
        }
        if (secondTerm != null) {
            totalDays += secondTerm.getTotalSchoolDays();
            totalPresent += secondTerm.getDaysPresent();
            totalAbsent += secondTerm.getDaysAbsent();
        }
        if (thirdTerm != null) {
            totalDays += thirdTerm.getTotalSchoolDays();
            totalPresent += thirdTerm.getDaysPresent();
            totalAbsent += thirdTerm.getDaysAbsent();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("studentId", student.getId());
        response.put("studentName", (student.getFirstName() + " " + student.getLastName()).trim());
        response.put("admissionNumber", student.getAdmissionNumber());
        response.put("classId", student.getSchoolClass() != null ? student.getSchoolClass().getId() : null);
        response.put("studentClass", student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : null);
        response.put("classArm", student.getSchoolClass() != null ? student.getSchoolClass().getArm() : null);
        response.put("classCode", student.getSchoolClass() != null ? student.getSchoolClass().getClassCode() : null);
        response.put("session", session);

        response.put("firstTerm", buildSummaryMap(firstTerm));
        response.put("secondTerm", buildSummaryMap(secondTerm));
        response.put("thirdTerm", buildSummaryMap(thirdTerm));

        Map<String, Object> sessionSummary = new HashMap<>();
        sessionSummary.put("totalSchoolDays", totalDays);
        sessionSummary.put("daysPresent", totalPresent);
        sessionSummary.put("daysAbsent", totalAbsent);
        sessionSummary.put("attendancePercentage", totalDays > 0 ? (totalPresent * 100.0 / totalDays) : 0.0);

        response.put("sessionSummary", sessionSummary);

        return response;
    }

    private Map<String, Object> buildSummaryMap(AttendanceSummary summary) {
        if (summary == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("session", summary.getSession());
        map.put("term", summary.getTerm() != null ? summary.getTerm().name() : null);
        map.put("totalSchoolDays", summary.getTotalSchoolDays());
        map.put("daysPresent", summary.getDaysPresent());
        map.put("daysAbsent", summary.getDaysAbsent());
        map.put("daysLate", summary.getDaysLate());
        map.put("daysExcused", summary.getDaysExcused());
        map.put("attendancePercentage", summary.getAttendancePercentage());
        return map;
    }

    @Override
    public List<Attendance> getClassAttendance(Long classId, LocalDate date, String session, Result.Term term) {
        List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);

        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Attendance> attendanceList = new ArrayList<>();

        for (Student student : students) {
            attendanceRepository.findByStudentAndDateAndSessionAndTerm(student, date, session, term)
                    .ifPresent(attendanceList::add);
        }

        return attendanceList;
    }

    @Override
    public Map<String, Object> getClassTermStatistics(Long classId, String session, Result.Term term) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);

        int totalStudents = students.size();
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;
        int excusedCount = 0;

        List<Map<String, Object>> studentAttendance = new ArrayList<>();

        for (Student student : students) {
            AttendanceSummary summary = getStudentTermSummary(student.getId(), session, term);

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("studentId", student.getId());
            studentData.put("studentName", student.getFirstName() + " " + student.getLastName());
            studentData.put("admissionNumber", student.getAdmissionNumber());
            studentData.put("classId", schoolClass.getId());
            studentData.put("class", schoolClass.getClassName());
            studentData.put("arm", schoolClass.getArm());
            studentData.put("classCode", schoolClass.getClassCode());
            studentData.put("present", summary.getDaysPresent());
            studentData.put("absent", summary.getDaysAbsent());
            studentData.put("late", summary.getDaysLate());
            studentData.put("excused", summary.getDaysExcused());
            studentData.put("percentage", summary.getAttendancePercentage());

            studentAttendance.add(studentData);

            presentCount += summary.getDaysPresent();
            absentCount += summary.getDaysAbsent();
            lateCount += summary.getDaysLate();
            excusedCount += summary.getDaysExcused();
        }

        double averageAttendance = 0.0;
        if (totalStudents > 0) {
            averageAttendance = studentAttendance.stream()
                    .mapToDouble(item -> ((Number) item.get("percentage")).doubleValue())
                    .average()
                    .orElse(0.0);
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("classId", schoolClass.getId());
        statistics.put("className", schoolClass.getClassName());
        statistics.put("arm", schoolClass.getArm());
        statistics.put("classCode", schoolClass.getClassCode());
        statistics.put("session", session);
        statistics.put("term", term.name());
        statistics.put("totalStudents", totalStudents);
        statistics.put("totalPresent", presentCount);
        statistics.put("totalAbsent", absentCount);
        statistics.put("totalLate", lateCount);
        statistics.put("totalExcused", excusedCount);
        statistics.put("averageAttendance", averageAttendance);
        statistics.put("studentAttendance", studentAttendance);

        return statistics;
    }

    @Override
    public Map<String, Object> getSchoolAttendanceStatistics(String session, Result.Term term) {
        List<Student> allStudents = studentRepository.findAll();

        int totalStudents = allStudents.size();
        int totalPresent = 0;
        int totalAbsent = 0;
        int totalLate = 0;
        int totalExcused = 0;

        Map<String, Integer> classStats = new HashMap<>();

        for (Student student : allStudents) {
            AttendanceSummary summary = getStudentTermSummary(student.getId(), session, term);

            totalPresent += summary.getDaysPresent();
            totalAbsent += summary.getDaysAbsent();
            totalLate += summary.getDaysLate();
            totalExcused += summary.getDaysExcused();

            String className = student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : "UNASSIGNED";
            String arm = student.getSchoolClass() != null ? student.getSchoolClass().getArm() : "";
            String key = arm != null && !arm.isBlank() ? className + "-" + arm : className;

            classStats.merge(key + "_present", summary.getDaysPresent(), Integer::sum);
            classStats.merge(key + "_absent", summary.getDaysAbsent(), Integer::sum);
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("session", session);
        statistics.put("term", term.name());
        statistics.put("totalStudents", totalStudents);
        statistics.put("totalPresent", totalPresent);
        statistics.put("totalAbsent", totalAbsent);
        statistics.put("totalLate", totalLate);
        statistics.put("totalExcused", totalExcused);
        statistics.put("attendanceRate", totalStudents > 0 ? (totalPresent * 100.0 / (totalStudents * 90.0)) : 0.0);
        statistics.put("classStatistics", classStats);

        return statistics;
    }

    @Override
    public List<LocalDate> initializeSchoolDays(List<LocalDate> dates, String session, Result.Term term) {
        log.info("Initializing {} school days for {} term {}", dates.size(), session, term);
        return dates;
    }

    @Override
    public void calculateAllTermSummaries(String session, Result.Term term) {
        log.info("Calculating attendance summaries for all students in {} term {}", session, term);

        List<Student> allStudents = studentRepository.findAll();

        for (Student student : allStudents) {
            try {
                AttendanceSummary summary = getStudentTermSummary(student.getId(), session, term);

                TermResult termResult = termResultRepository
                        .findByStudentAndSessionAndTerm(student, session, term)
                        .orElse(new TermResult());

                termResult.setStudent(student);
                termResult.setSession(session);
                termResult.setTerm(term);
                termResult.setTotalSchoolDays(summary.getTotalSchoolDays());
                termResult.setDaysPresent(summary.getDaysPresent());
                termResult.setDaysAbsent(summary.getDaysAbsent());
                termResult.setAttendancePercentage(summary.getAttendancePercentage());

                termResultRepository.save(termResult);

            } catch (Exception e) {
                log.error("Error calculating attendance for student {}: {}", student.getId(), e.getMessage(), e);
            }
        }
    }

    private List<LocalDate> getSchoolDays(String session, Result.Term term) {
        List<LocalDate> schoolDays = new ArrayList<>();
        LocalDate startDate;

        if (term == Result.Term.FIRST) {
            startDate = LocalDate.of(Integer.parseInt(session.split("/")[0]), 9, 8);
        } else if (term == Result.Term.SECOND) {
            startDate = LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 1, 6);
        } else {
            startDate = LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 4, 15);
        }

        for (int i = 0; i < 90; i++) {
            LocalDate date = startDate.plusDays(i);
            schoolDays.add(date);
        }

        return schoolDays;
    }

    private void updateTermAttendanceSummary(Student student, String session, Result.Term term) {
        AttendanceSummary summary = getStudentTermSummary(student.getId(), session, term);

        TermResult termResult = termResultRepository
                .findByStudentAndSessionAndTerm(student, session, term)
                .orElse(new TermResult());

        termResult.setStudent(student);
        termResult.setSession(session);
        termResult.setTerm(term);
        termResult.setTotalSchoolDays(summary.getTotalSchoolDays());
        termResult.setDaysPresent(summary.getDaysPresent());
        termResult.setDaysAbsent(summary.getDaysAbsent());
        termResult.setAttendancePercentage(summary.getAttendancePercentage());

        termResultRepository.save(termResult);
    }
}