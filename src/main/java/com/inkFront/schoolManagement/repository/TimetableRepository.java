package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    List<Timetable> findBySchoolClass_IdAndSessionAndTerm(Long classId, String session, Timetable.Term term);

    List<Timetable> findByTeacher_IdAndSessionAndTerm(Long teacherId, String session, Timetable.Term term);

    List<Timetable> findBySessionAndTerm(String session, Timetable.Term term);

    boolean existsByTeacher_IdAndSessionAndTermAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
            Long teacherId,
            String session,
            Timetable.Term term,
            DayOfWeek dayOfWeek,
            LocalTime endTime,
            LocalTime startTime
    );
}