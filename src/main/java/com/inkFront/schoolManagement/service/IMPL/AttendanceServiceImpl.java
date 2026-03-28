package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.AttendanceSummary;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.AttendanceRepository;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
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

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

    @Override
    public Attendance markAttendance(Long studentId, LocalDate date, String session, Result.Term term,
                                     Attendance.AttendanceStatus status, String remarks) {
        log.info("Marking attendance for student: {}, date: {}, session: {}, term: {}",
                studentId, date, session, term);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Attendance attendance = attendanceRepository
                .findByStudentAndDateAndSessionAndTerm(student, date, session, term)
                .orElse(new Attendance());

        attendance.setStudent(student);
        attendance.setDate(date);
        attendance.setSession(session);
        attendance.setTerm(term);
        attendance.setStatus(status);
        attendance.setRemarks(remarks);

        return attendanceRepository.save(attendance);
    }

    @Override
    public List<Attendance> markBulkAttendance(List<Long> studentIds, LocalDate date, String session,
                                               Result.Term term, Attendance.AttendanceStatus status) {
        List<Attendance> attendances = new ArrayList<>();

        for (Long studentId : studentIds) {
            attendances.add(markAttendance(studentId, date, session, term, status, null));
        }

        return attendances;
    }

    @Override
    @Transactional(readOnly = true)
    public Attendance getStudentAttendance(Long studentId, LocalDate date, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return attendanceRepository.findByStudentAndDateAndSessionAndTerm(student, date, session, term)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getStudentTermAttendance(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(student, session, term);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummary getStudentTermSummary(Long studentId, String session, Result.Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Attendance> records = attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                student, session, term
        );

        List<LocalDate> schoolDays = attendanceRepository.findDistinctDatesBySessionAndTerm(session, term);

        if ((schoolDays == null || schoolDays.isEmpty()) && !records.isEmpty()) {
            schoolDays = records.stream()
                    .map(Attendance::getDate)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        }

        int totalSchoolDays = schoolDays == null ? 0 : schoolDays.size();
        int daysPresent = 0;
        int daysAbsent = 0;
        int daysLate = 0;
        int daysExcused = 0;

        for (Attendance attendance : records) {
            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                daysPresent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                daysAbsent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.LATE) {
                daysLate++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                daysExcused++;
            }
        }

        int presentEquivalent = daysPresent + daysLate + daysExcused;
        double attendancePercentage = totalSchoolDays > 0
                ? (presentEquivalent * 100.0 / totalSchoolDays)
                : 0.0;

        AttendanceSummary summary = new AttendanceSummary();
        summary.setStudent(student);
        summary.setSession(session);
        summary.setTerm(term);
        summary.setTotalSchoolDays(totalSchoolDays);
        summary.setDaysPresent(presentEquivalent);
        summary.setDaysAbsent(daysAbsent);
        summary.setDaysLate(daysLate);
        summary.setDaysExcused(daysExcused);
        summary.setAttendancePercentage(attendancePercentage);

        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentSessionSummary(Long studentId, String session) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

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
        int late = 0;
        int excused = 0;

        for (Attendance attendance : allAttendance) {
            if (attendance.getDate() == null || attendance.getTerm() == null) {
                continue;
            }

            uniqueSchoolDays.add(attendance.getTerm().name() + "_" + attendance.getDate());

            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                presentEquivalent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                absent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.LATE) {
                late++;
                presentEquivalent++;
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                excused++;
                presentEquivalent++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("studentId", student.getId());
        response.put("studentName", student.getFirstName() + " " + student.getLastName());
        response.put("session", session);
        response.put("totalSchoolDays", uniqueSchoolDays.size());
        response.put("daysPresent", presentEquivalent);
        response.put("daysAbsent", absent);
        response.put("daysLate", late);
        response.put("daysExcused", excused);
        response.put("attendancePercentage",
                uniqueSchoolDays.isEmpty() ? 0.0 : (presentEquivalent * 100.0 / uniqueSchoolDays.size()));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getClassAttendance(Long classId, LocalDate date, String session, Result.Term term) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        return attendanceRepository
                .findByStudent_SchoolClass_IdAndDateAndSessionAndTermOrderByStudent_LastNameAscStudent_FirstNameAsc(
                        classId, date, session, term
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getClassTermStatistics(Long classId, String session, Result.Term term) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        List<LocalDate> schoolDays = attendanceRepository.findDistinctDatesBySessionAndTerm(session, term);
        int totalSchoolDays = schoolDays == null ? 0 : schoolDays.size();

        List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);
        int totalStudents = students.size();

        long totalPresent = attendanceRepository.countByClassIdAndSessionAndTermAndStatus(
                classId, session, term, Attendance.AttendanceStatus.PRESENT
        );
        long totalAbsent = attendanceRepository.countByClassIdAndSessionAndTermAndStatus(
                classId, session, term, Attendance.AttendanceStatus.ABSENT
        );
        long totalLate = attendanceRepository.countByClassIdAndSessionAndTermAndStatus(
                classId, session, term, Attendance.AttendanceStatus.LATE
        );
        long totalExcused = attendanceRepository.countByClassIdAndSessionAndTermAndStatus(
                classId, session, term, Attendance.AttendanceStatus.EXCUSED
        );

        long presentEquivalent = totalPresent + totalLate + totalExcused;
        double attendancePercentage = (totalSchoolDays <= 0 || totalStudents <= 0)
                ? 0.0
                : (presentEquivalent * 100.0) / (totalStudents * totalSchoolDays);

        List<Map<String, Object>> studentAttendance = new ArrayList<>();

        for (Student student : students) {
            List<Attendance> records = attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, term
            );

            long present = records.stream()
                    .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                    .count();

            long absent = records.stream()
                    .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT)
                    .count();

            long late = records.stream()
                    .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LATE)
                    .count();

            long excused = records.stream()
                    .filter(a -> a.getStatus() == Attendance.AttendanceStatus.EXCUSED)
                    .count();

            long studentPresentEquivalent = present + late + excused;
            double percentage = totalSchoolDays > 0
                    ? (studentPresentEquivalent * 100.0 / totalSchoolDays)
                    : 0.0;

            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("studentId", student.getId());
            studentMap.put("studentName",
                    (student.getFirstName() + " " + student.getLastName()).trim());
            studentMap.put("admissionNumber", student.getAdmissionNumber());
            studentMap.put("class", schoolClass.getClassName());
            studentMap.put("arm", schoolClass.getArm());
            studentMap.put("present", present);
            studentMap.put("absent", absent);
            studentMap.put("late", late);
            studentMap.put("excused", excused);
            studentMap.put("percentage", percentage);

            studentAttendance.add(studentMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("classId", schoolClass.getId());
        response.put("className", schoolClass.getClassName());
        response.put("arm", schoolClass.getArm());
        response.put("session", session);
        response.put("term", term.name());
        response.put("totalStudents", totalStudents);
        response.put("totalSchoolDays", totalSchoolDays);
        response.put("present", totalPresent);
        response.put("absent", totalAbsent);
        response.put("late", totalLate);
        response.put("excused", totalExcused);
        response.put("presentCount", totalPresent);
        response.put("absentCount", totalAbsent);
        response.put("lateCount", totalLate);
        response.put("excusedCount", totalExcused);
        response.put("attendancePercentage", attendancePercentage);
        response.put("studentAttendance", studentAttendance);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSchoolAttendanceStatistics(String session, Result.Term term) {
        List<LocalDate> schoolDays = attendanceRepository.findDistinctDatesBySessionAndTerm(session, term);
        int totalSchoolDays = schoolDays == null ? 0 : schoolDays.size();

        List<Student> students = studentRepository.findAll();
        int totalStudents = students.size();

        long totalPresent = 0;
        long totalAbsent = 0;
        long totalLate = 0;
        long totalExcused = 0;

        for (Student student : students) {
            List<Attendance> records = attendanceRepository.findByStudentAndSessionAndTermOrderByDateAsc(
                    student, session, term
            );

            for (Attendance attendance : records) {
                if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                    totalPresent++;
                } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                    totalAbsent++;
                } else if (attendance.getStatus() == Attendance.AttendanceStatus.LATE) {
                    totalLate++;
                } else if (attendance.getStatus() == Attendance.AttendanceStatus.EXCUSED) {
                    totalExcused++;
                }
            }
        }

        long presentEquivalent = totalPresent + totalLate + totalExcused;
        double attendancePercentage = (totalSchoolDays <= 0 || totalStudents <= 0)
                ? 0.0
                : (presentEquivalent * 100.0) / (totalStudents * totalSchoolDays);

        Map<String, Object> response = new HashMap<>();
        response.put("session", session);
        response.put("term", term.name());
        response.put("totalStudents", totalStudents);
        response.put("totalSchoolDays", totalSchoolDays);
        response.put("presentCount", totalPresent);
        response.put("absentCount", totalAbsent);
        response.put("lateCount", totalLate);
        response.put("excusedCount", totalExcused);
        response.put("attendancePercentage", attendancePercentage);

        return response;
    }

    @Override
    public List<Attendance> initializeSchoolDays(List<LocalDate> dates, String session, Result.Term term) {
        List<Attendance> initializedRecords = new ArrayList<>();

        List<Student> students = studentRepository.findAll();

        for (LocalDate date : dates) {
            for (Student student : students) {
                Optional<Attendance> existing = attendanceRepository
                        .findByStudentAndDateAndSessionAndTerm(student, date, session, term);

                if (existing.isEmpty()) {
                    Attendance attendance = new Attendance();
                    attendance.setStudent(student);
                    attendance.setDate(date);
                    attendance.setSession(session);
                    attendance.setTerm(term);
                    attendance.setStatus(Attendance.AttendanceStatus.ABSENT);
                    attendance.setRemarks("Initialized school day");

                    initializedRecords.add(attendance);
                }
            }
        }

        return initializedRecords.isEmpty()
                ? Collections.emptyList()
                : attendanceRepository.saveAll(initializedRecords);
    }

    @Override
    public void calculateAllTermSummaries(String session, Result.Term term) {
        List<Student> students = studentRepository.findAll();

        for (Student student : students) {
            try {
                getStudentTermSummary(student.getId(), session, term);
            } catch (Exception e) {
                log.error("Error calculating attendance summary for student {}: {}",
                        student.getId(), e.getMessage(), e);
            }
        }
    }
}