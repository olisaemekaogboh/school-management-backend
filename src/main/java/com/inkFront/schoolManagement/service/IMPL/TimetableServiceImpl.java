package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.TimetableDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.Timetable;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.TimetableRepository;
import com.inkFront.schoolManagement.service.TimetableService;
import lombok.RequiredArgsConstructor;
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

    private TimetableDTO toDTO(Timetable t) {
        TimetableDTO dto = new TimetableDTO();
        dto.setId(t.getId());

        dto.setSchoolClassId(t.getSchoolClass() != null ? t.getSchoolClass().getId() : null);
        dto.setTeacherId(t.getTeacher() != null ? t.getTeacher().getId() : null);

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

    private static Timetable.Term parseTerm(String term) {
        if (term == null) throw new RuntimeException("Term is required");
        try {
            return Timetable.Term.valueOf(term.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid term: " + term + ". Use FIRST, SECOND, or THIRD.");
        }
    }

    private static DayOfWeek parseDay(String day) {
        if (day == null) throw new RuntimeException("Day is required");
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid day: " + day + ". Use MONDAY..SUNDAY.");
        }
    }

    private static LocalTime parseTime(String time) {
        if (time == null) throw new RuntimeException("Time is required");
        try {
            return LocalTime.parse(time.trim()); // expects HH:mm or HH:mm:ss
        } catch (Exception e) {
            throw new RuntimeException("Invalid time: " + time + ". Use HH:mm (e.g. 09:30).");
        }
    }

    private void apply(Timetable t, TimetableDTO dto) {

        if (dto.getSchoolClassId() == null) throw new RuntimeException("schoolClassId is required");
        if (dto.getTeacherId() == null) throw new RuntimeException("teacherId is required");
        if (dto.getSubject() == null || dto.getSubject().trim().isEmpty()) throw new RuntimeException("subject is required");
        if (dto.getDayOfWeek() == null) throw new RuntimeException("dayOfWeek is required");
        if (dto.getStartTime() == null || dto.getEndTime() == null) throw new RuntimeException("startTime and endTime are required");
        if (dto.getSession() == null || dto.getSession().trim().isEmpty()) throw new RuntimeException("session is required");
        if (dto.getTerm() == null) throw new RuntimeException("term is required");

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

        // allow UI to set active (optional)
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
                .findBySchoolClass_IdAndSessionAndTerm(classId, session, parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getTeacherTimetable(Long teacherId, String session, String term) {
        return timetableRepository
                .findByTeacher_IdAndSessionAndTerm(teacherId, session, parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDTO> getSchoolTimetable(String session, String term) {
        return timetableRepository
                .findBySessionAndTerm(session, parseTerm(term))
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long teacherId, String day, String startTime, String endTime, String session, String term) {

        boolean conflict =
                timetableRepository.existsByTeacher_IdAndSessionAndTermAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                        teacherId,
                        session,
                        parseTerm(term),
                        parseDay(day),
                        parseTime(endTime),
                        parseTime(startTime)
                );

        return !conflict;
    }
}