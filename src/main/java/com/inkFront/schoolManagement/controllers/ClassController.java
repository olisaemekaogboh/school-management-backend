package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ClassDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping
    public ResponseEntity<ClassDTO> createClass(@Valid @RequestBody ClassDTO classDTO) {
        SchoolClass schoolClass = classService.createClass(classDTO);
        return new ResponseEntity<>(ClassDTO.fromEntity(schoolClass), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassDTO> updateClass(@PathVariable Long id, @Valid @RequestBody ClassDTO classDTO) {
        SchoolClass schoolClass = classService.updateClass(id, classDTO);
        return ResponseEntity.ok(ClassDTO.fromEntity(schoolClass));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassDTO> getClass(@PathVariable Long id) {
        SchoolClass schoolClass = classService.getClassWithTeacher(id);
        return ResponseEntity.ok(ClassDTO.fromEntity(schoolClass));
    }

    @GetMapping("/name/{className}")
    public ResponseEntity<ClassDTO> getClassByName(@PathVariable String className) {
        SchoolClass schoolClass = classService.getClassByName(className);
        return ResponseEntity.ok(ClassDTO.fromEntity(schoolClass));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ClassDTO>> getAllClasses() {
        return ResponseEntity.ok(classService.getAllClasses());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ClassDTO>> getClassesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(classService.getClassesByCategory(category));
    }

    @PostMapping("/{classId}/assign-teacher/{teacherId}")
    public ResponseEntity<ClassDTO> assignClassTeacher(
            @PathVariable Long classId,
            @PathVariable Long teacherId) {
        SchoolClass schoolClass = classService.assignClassTeacher(classId, teacherId);
        return ResponseEntity.ok(ClassDTO.fromEntity(schoolClass));
    }

    @PostMapping("/{classId}/subjects")
    public ResponseEntity<ClassDTO> addSubject(
            @PathVariable Long classId,
            @RequestParam String subject) {
        SchoolClass schoolClass = classService.addSubject(classId, subject);
        return ResponseEntity.ok(ClassDTO.fromEntity(schoolClass));
    }

    @DeleteMapping("/{classId}/subjects")
    public ResponseEntity<ClassDTO> removeSubject(
            @PathVariable Long classId,
            @RequestParam String subject) {
        SchoolClass schoolClass = classService.removeSubject(classId, subject);
        return ResponseEntity.ok(ClassDTO.fromEntity(schoolClass));
    }

    @GetMapping("/{classId}/students")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsInClass(@PathVariable Long classId) {
        return ResponseEntity.ok(classService.getStudentsInClass(classId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getClassStatistics() {
        return ResponseEntity.ok(classService.getClassStatistics());
    }

    @GetMapping("/{classId}/export/pdf")
    public ResponseEntity<byte[]> exportClassListPdf(@PathVariable Long classId) throws Exception {
        byte[] pdfBytes = classService.generateClassListPdf(classId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=class_list.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/{classId}/export/excel")
    public ResponseEntity<byte[]> exportClassListExcel(@PathVariable Long classId) throws Exception {
        byte[] excelBytes = classService.generateClassListExcel(classId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=class_list.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}