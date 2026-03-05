package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.TimetableDTO;

import java.util.List;

public interface TimetableService {

    TimetableDTO createEntry(TimetableDTO dto);

    TimetableDTO updateEntry(Long id, TimetableDTO dto);

    TimetableDTO getEntry(Long id);

    void deleteEntry(Long id);

    List<TimetableDTO> getClassTimetable(Long classId, String session, String term);

    List<TimetableDTO> getTeacherTimetable(Long teacherId, String session, String term);

    List<TimetableDTO> getSchoolTimetable(String session, String term);

    boolean checkAvailability(Long teacherId, String day, String startTime, String endTime, String session, String term);
}