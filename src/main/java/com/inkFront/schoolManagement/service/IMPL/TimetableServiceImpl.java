package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.TimetableDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.Timetable;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.TimetableRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private TimetableDTO toDTO(Timetable t) {
        TimetableDTO dto = new TimetableDTO();
        dto.setId(t.getId());

        if (t.getSchoolClass() != null) {
            dto.setSchoolClassId(t.getSchoolClass().getId());
            dto.setClassName(t.getSchoolClass().getClassName());
            dto.setClassArm(t.getSchoolClass().getArm());
        }

        if (t.getTeacher() != null) {
            dto.setTeacherId(t.getTeacher().getId());
            dto.setTeacherName(
                    (
                            (t.getTeacher().getFirstName() != null ? t.getTeacher().getFirstName() : "") +
                                    " " +
                                    (t.getTeacher().getLastName() != null ? t.getTeacher().getLastName() : "")
                    ).trim()
            );
        }

        dto.setSubject(t.getSubject());
        dto.setDayOfWeek(t.getDayOfWeek() != null ? t.getDayOfWeek().name() : null);
        dto.setStartTime(t.getStartTime() != null ? t.getStartTime().toString() : null);
        dto.setEndTime(t.getEndTime() != null ? t.getEndTime().toString() : null);
        dto.setRoom(t.getRoom());
        dto.setSession(t.getSession());
        dto.setTerm(t.getTerm() != null ? t.getTerm().name() : null);
        dto.setActive(t.isActive());

        return dto;
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static Timetable.Term parseTerm(String term) {
        if (term == null) {
            throw new RuntimeException("Term is required");
        }
        try {
            return Timetable.Term.valueOf(term.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid term: " + term + ". Use FIRST, SECOND, or THIRD.");
        }
    }

    private static DayOfWeek parseDay(String day) {
        if (day == null) {
            throw new RuntimeException("Day is required");
        }
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid day: " + day + ". Use MONDAY..SUNDAY.");
        }
    }

    private static LocalTime parseTime(String time) {
        if (time == null) {
            throw new RuntimeException("Time is required");
        }
        try {
            return LocalTime.parse(time.trim());
        } catch (Exception e) {
            throw new RuntimeException("Invalid time: " + time + ". Use HH:mm.");
        }
    }

    private void apply(Timetable t, TimetableDTO dto) {
        if (dto.getSchoolClassId() == null) {
            throw new RuntimeException("schoolClassId is required");
        }
        if (dto.getTeacherId() == null) {
            throw new RuntimeException("teacherId is required");
        }
        if (dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new RuntimeException("subject is required");
        }
        if (dto.getDayOfWeek() == null || dto.getDayOfWeek().trim().isEmpty()) {
            throw new RuntimeException("dayOfWeek is required");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new RuntimeException("startTime and endTime are required");
        }
        if (dto.getSession() == null || dto.getSession().trim().isEmpty()) {
            throw new RuntimeException("session is required");
        }
        if (dto.getTerm() == null || dto.getTerm().trim().isEmpty()) {
            throw new RuntimeException("term is required");
        }

        SchoolClass schoolClass = classRepository.findById(dto.getSchoolClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        LocalTime start = parseTime(dto.getStartTime());
        LocalTime end = parseTime(dto.getEndTime());

        if (!end.isAfter(start)) {
            throw new RuntimeException("endTime must be after startTime");
        }

        t.setSchoolClass(schoolClass);
        t.setTeacher(teacher);
        t.setSubject(dto.getSubject().trim());
        t.setDayOfWeek(parseDay(dto.getDayOfWeek()));
        t.setStartTime(start);
        t.setEndTime(end);
        t.setRoom(dto.getRoom());
        t.setSession(dto.getSession().trim());
        t.setTerm(parseTerm(dto.getTerm()));

        if (dto.getActive() != null) {
            t.setActive(dto.getActive());
        }
    }

    @Override
    public TimetableDTO createEntry(TimetableDTO dto) {
        Timetable t = new Timetable();
        apply(t, dto);
        return toDTO(timetableRepository.save(t));
    }

    @Override
    public TimetableDTO updateEntry(Long id, TimetableDTO dto) {
        Timetable t = timetableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timetable entry not found"));
        apply(t, dto);
        return toDTO(timetableRepository.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableDTO getEntry(Long id) {
        return timetableRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Timetable entry not found"));
    }

    @Override
    public void deleteEntry(Long id) {
        timetableRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getClassTimetable(Long classId, String session, String term) {
        return timetableRepository
                .findBySchoolClass_IdAndSessionAndTerm(classId, session.trim(), parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getTeacherTimetable(Long teacherId, String session, String term) {
        return timetableRepository
                .findByTeacher_IdAndSessionAndTerm(teacherId, session.trim(), parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getSchoolTimetable(String session, String term) {
        return timetableRepository
                .findBySessionAndTerm(session.trim(), parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(
            Long teacherId,
            String day,
            String startTime,
            String endTime,
            String session,
            String term
    ) {
        boolean conflict =
                timetableRepository.existsByTeacher_IdAndSessionAndTermAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                        teacherId,
                        session.trim(),
                        parseTerm(term),
                        parseDay(day),
                        parseTime(endTime),
                        parseTime(startTime)
                );

        return !conflict;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getStudentOwnTimetable(String usernameOrEmail, String session, String term) {
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        Student student = user.getStudent();
        if (student == null) {
            throw new AccessDeniedException("This account is not linked to a student");
        }

        String className = student.getStudentClass();
        String classArm = student.getClassArm();

        System.out.println("=== STUDENT TIMETABLE DEBUG ===");
        System.out.println("usernameOrEmail = [" + usernameOrEmail + "]");
        System.out.println("studentId = [" + student.getId() + "]");
        System.out.println("studentClass = [" + className + "]");
        System.out.println("classArm = [" + classArm + "]");
        System.out.println("requestedSession = [" + session + "]");
        System.out.println("requestedTerm = [" + term + "]");

        if (className == null || className.isBlank() || classArm == null || classArm.isBlank()) {
            System.out.println("Student className/classArm is blank, returning empty list");
            System.out.println("===============================");
            return List.of();
        }

        SchoolClass schoolClass = classRepository
                .findByClassNameAndArmNormalized(className.trim(), classArm.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No matching class found for studentClass=" + className + ", classArm=" + classArm
                ));

        System.out.println("resolvedSchoolClassId = [" + schoolClass.getId() + "]");
        System.out.println("resolvedSchoolClassName = [" + schoolClass.getClassName() + "]");
        System.out.println("resolvedSchoolClassArm = [" + schoolClass.getArm() + "]");

        List<TimetableDTO> result = timetableRepository
                .findBySchoolClass_IdAndSessionAndTerm(
                        schoolClass.getId(),
                        session.trim(),
                        parseTerm(term)
                )
                .stream()
                .map(this::toDTO)
                .collect(toList());

        System.out.println("timetableResultCount = [" + result.size() + "]");
        System.out.println("===============================");

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getTeacherOwnTimetable(String usernameOrEmail, String session, String term) {
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        Teacher teacher = user.getTeacher();
        if (teacher == null) {
            throw new AccessDeniedException("This account is not linked to a teacher");
        }

        return timetableRepository
                .findByTeacher_IdAndSessionAndTerm(teacher.getId(), session.trim(), parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getParentWardTimetable(String usernameOrEmail, Long studentId, String session, String term) {
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        Parent parent = user.getParent();
        if (parent == null) {
            throw new AccessDeniedException("This account is not linked to a parent");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (student.getParent() == null || !student.getParent().getId().equals(parent.getId())) {
            throw new AccessDeniedException("You can only view timetable for your own ward");
        }

        String className = student.getStudentClass();
        String classArm = student.getClassArm();

        if (className == null || className.isBlank() || classArm == null || classArm.isBlank()) {
            return List.of();
        }

        SchoolClass schoolClass = classRepository
                .findByClassNameAndArmNormalized(className.trim(), classArm.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No matching class found for studentClass=" + className + ", classArm=" + classArm
                ));

        return timetableRepository
                .findBySchoolClass_IdAndSessionAndTerm(
                        schoolClass.getId(),
                        session.trim(),
                        parseTerm(term)
                )
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }
}