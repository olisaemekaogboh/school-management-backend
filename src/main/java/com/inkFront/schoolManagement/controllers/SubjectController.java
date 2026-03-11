package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<SubjectResponseDTO> createSubject(@Valid @RequestBody SubjectRequestDTO request) {
        return new ResponseEntity<>(subjectService.createSubject(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponseDTO> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequestDTO request
    ) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponseDTO>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @GetMapping("/active")
    public ResponseEntity<List<SubjectResponseDTO>> getActiveSubjects() {
        return ResponseEntity.ok(subjectService.getActiveSubjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponseDTO> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SubjectResponseDTO> toggleSubjectStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(subjectService.toggleSubjectStatus(id, active));
    }

    @PostMapping("/class-assignments")
    public ResponseEntity<ClassSubjectResponseDTO> assignSubjectToClass(
            @Valid @RequestBody ClassSubjectRequestDTO request
    ) {
        return new ResponseEntity<>(subjectService.assignSubjectToClass(request), HttpStatus.CREATED);
    }

    @GetMapping("/class-assignments")
    public ResponseEntity<List<ClassSubjectResponseDTO>> getSubjectsForClass(
            @RequestParam String className,
            @RequestParam String classArm
    ) {
        return ResponseEntity.ok(subjectService.getSubjectsForClass(className, classArm));
    }

    @DeleteMapping("/class-assignments")
    public ResponseEntity<Void> removeSubjectFromClass(
            @RequestParam String className,
            @RequestParam String classArm,
            @RequestParam Long subjectId
    ) {
        subjectService.removeSubjectFromClass(className, classArm, subjectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teacher-assignments")
    public ResponseEntity<TeacherSubjectResponseDTO> assignSubjectToTeacher(
            @Valid @RequestBody TeacherSubjectRequestDTO request
    ) {
        return new ResponseEntity<>(subjectService.assignSubjectToTeacher(request), HttpStatus.CREATED);
    }

    @GetMapping("/teacher-assignments/{teacherId}")
    public ResponseEntity<List<TeacherSubjectResponseDTO>> getTeacherSubjects(@PathVariable Long teacherId) {
        return ResponseEntity.ok(subjectService.getTeacherSubjects(teacherId));
    }

    @DeleteMapping("/teacher-assignments/{id}")
    public ResponseEntity<Void> removeTeacherSubject(@PathVariable Long id) {
        subjectService.removeTeacherSubject(id);
        return ResponseEntity.noContent().build();
    }
}