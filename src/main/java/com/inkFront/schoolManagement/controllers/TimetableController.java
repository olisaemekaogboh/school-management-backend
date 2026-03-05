package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.TimetableDTO;
import com.inkFront.schoolManagement.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping
    public TimetableDTO create(@Valid @RequestBody TimetableDTO dto) {
        return timetableService.createEntry(dto);
    }

    @PutMapping("/{id}")
    public TimetableDTO update(@PathVariable Long id, @Valid @RequestBody TimetableDTO dto) {
        return timetableService.updateEntry(id, dto);
    }

    @GetMapping("/{id}")
    public TimetableDTO get(@PathVariable Long id) {
        return timetableService.getEntry(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        timetableService.deleteEntry(id);
    }

    @GetMapping("/class/{classId}")
    public List<TimetableDTO> classTimetable(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam String term
    ) {
        return timetableService.getClassTimetable(classId, session, term);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<TimetableDTO> teacherTimetable(
            @PathVariable Long teacherId,
            @RequestParam String session,
            @RequestParam String term
    ) {
        return timetableService.getTeacherTimetable(teacherId, session, term);
    }

    @GetMapping("/school")
    public List<TimetableDTO> school(@RequestParam String session, @RequestParam String term) {
        return timetableService.getSchoolTimetable(session, term);
    }

    // ✅ UPDATED: include session + term so conflicts are checked correctly
    @GetMapping("/check-availability")
    public boolean check(
            @RequestParam Long teacherId,
            @RequestParam String day,
            @RequestParam String session,
            @RequestParam String term,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        return timetableService.checkAvailability(teacherId, day, startTime, endTime, session, term);
    }
}