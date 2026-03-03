// src/main/java/com/inkFront/schoolManagement/controllers/StudentController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.StudentRequestDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:3000")
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);
    private final StudentService studentService;
    private final String UPLOAD_DIR = "uploads/profile-pictures/";

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // ==================== CREATE METHODS ====================

    /**
     * Create a new student with profile picture (multipart/form-data with JSON string)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDTO> registerStudentWithFile(
            @RequestParam("student") String studentJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        try {
            // Create ObjectMapper with JavaTimeModule
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Parse JSON string to DTO
            StudentRequestDTO studentRequest = mapper.readValue(studentJson, StudentRequestDTO.class);

            System.out.println("Received student: " + studentRequest.getFirstName());

            // Handle profile picture
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fileUrl = saveProfilePicture(profilePicture);
                studentRequest.setProfilePictureUrl(fileUrl);
                System.out.println("Set profile picture URL: " + fileUrl);
            }

            Student student = mapToEntity(studentRequest);

            // Log before saving
            System.out.println("Before saving - Student profile picture URL: " + student.getProfilePictureUrl());

            Student savedStudent = studentService.registerStudent(student);

            // Log after saving
            System.out.println("After saving - Student profile picture URL: " + savedStudent.getProfilePictureUrl());

            return new ResponseEntity<>(StudentResponseDTO.fromStudent(savedStudent), HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new student with profile picture (using @RequestPart - Spring handles JSON parsing automatically)
     */
    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDTO> registerStudentWithFilePart(
            @RequestPart("student") @Valid StudentRequestDTO studentRequest,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        try {
            System.out.println("========== RECEIVED WITH FILE PART ==========");
            System.out.println("Student: " + studentRequest.getFirstName() + " " + studentRequest.getLastName());
            System.out.println("File present: " + (profilePicture != null));

            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fileUrl = saveProfilePicture(profilePicture);
                studentRequest.setProfilePictureUrl(fileUrl);
            }

            Student student = mapToEntity(studentRequest);
            Student savedStudent = studentService.registerStudent(student);

            return new ResponseEntity<>(StudentResponseDTO.fromStudent(savedStudent), HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new student without profile picture (JSON only)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentResponseDTO> registerStudentJson(@Valid @RequestBody StudentRequestDTO studentRequest) {
        System.out.println("========== RECEIVED JSON ONLY ==========");
        System.out.println("Student: " + studentRequest.getFirstName() + " " + studentRequest.getLastName());

        Student student = mapToEntity(studentRequest);
        Student savedStudent = studentService.registerStudent(student);

        return new ResponseEntity<>(StudentResponseDTO.fromStudent(savedStudent), HttpStatus.CREATED);
    }

    // ==================== UPDATE METHODS ====================

    /**
     * Update student with profile picture (multipart/form-data)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDTO> updateStudentWithFile(
            @PathVariable Long id,
            @RequestParam("student") String studentJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            StudentRequestDTO studentRequest = mapper.readValue(studentJson, StudentRequestDTO.class);

            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fileUrl = saveProfilePicture(profilePicture);
                studentRequest.setProfilePictureUrl(fileUrl);
            }

            Student student = mapToEntity(studentRequest);
            Student updatedStudent = studentService.updateStudent(id, student);

            return ResponseEntity.ok(StudentResponseDTO.fromStudent(updatedStudent));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update student without profile picture (JSON only)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentResponseDTO> updateStudentJson(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO studentRequest) {
        Student student = mapToEntity(studentRequest);
        Student updatedStudent = studentService.updateStudent(id, student);
        return ResponseEntity.ok(StudentResponseDTO.fromStudent(updatedStudent));
    }

    // ==================== READ METHODS ====================

    /**
     * Get all students
     */
    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        log.debug("GET /api/students - Fetching all students");
        List<Student> students = studentService.getAllStudents();
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get students with pagination
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<StudentResponseDTO>> getAllStudentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("GET /api/students/paginated - Page: {}, Size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Student> studentPage = studentService.getAllStudentsPaginated(pageable);
        Page<StudentResponseDTO> responsePage = studentPage.map(StudentResponseDTO::fromStudent);

        return ResponseEntity.ok(responsePage);
    }

    /**
     * Get student by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable Long id) {
        log.debug("GET /api/students/{} - Fetching student by ID", id);
        return studentService.getStudentById(id)
                .map(student -> ResponseEntity.ok(StudentResponseDTO.fromStudent(student)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get student by admission number
     */
    @GetMapping("/admission/{admissionNumber}")
    public ResponseEntity<StudentResponseDTO> getStudentByAdmissionNumber(@PathVariable String admissionNumber) {
        log.debug("GET /api/students/admission/{} - Fetching student by admission number", admissionNumber);
        return studentService.getStudentByAdmissionNumber(admissionNumber)
                .map(student -> ResponseEntity.ok(StudentResponseDTO.fromStudent(student)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search students by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<StudentResponseDTO>> searchStudents(@RequestParam String term) {
        log.debug("GET /api/students/search - Searching students with term: {}", term);
        List<Student> students = studentService.searchStudents(term);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get students by class
     */
    @GetMapping("/class/{className}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByClass(@PathVariable String className) {
        log.debug("GET /api/students/class/{} - Fetching students by class", className);
        List<Student> students = studentService.getStudentsByClass(className);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get students by class and arm
     */
    @GetMapping("/class/{className}/arm/{arm}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByClassAndArm(
            @PathVariable String className,
            @PathVariable String arm) {
        log.debug("GET /api/students/class/{}/arm/{} - Fetching students by class and arm", className, arm);
        List<Student> students = studentService.getStudentsByClassAndArm(className, arm);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get students by state of origin
     */
    @GetMapping("/state/{state}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByState(@PathVariable String state) {
        log.debug("GET /api/students/state/{} - Fetching students by state", state);
        List<Student> students = studentService.getStudentsByState(state);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get students by LGA
     */
    @GetMapping("/lga/{lga}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByLGA(@PathVariable String lga) {
        log.debug("GET /api/students/lga/{} - Fetching students by LGA", lga);
        List<Student> students = studentService.getStudentsByLGA(lga);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get active students
     */
    @GetMapping("/active")
    public ResponseEntity<List<StudentResponseDTO>> getActiveStudents() {
        log.debug("GET /api/students/active - Fetching active students");
        List<Student> students = studentService.getActiveStudents();
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get students by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByStatus(
            @PathVariable Student.StudentStatus status) {
        log.debug("GET /api/students/status/{} - Fetching students by status", status);
        List<Student> students = studentService.getStudentsByStatus(status);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get student statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStudentStatistics() {
        log.debug("GET /api/students/statistics - Fetching student statistics");
        Map<String, Object> statistics = Map.of(
                "totalStudents", studentService.getTotalStudentCount(),
                "activeStudents", studentService.getActiveStudentCount(),
                "studentsByClass", studentService.getStudentCountByClass(),
                "recentAdmissions", studentService.getRecentAdmissions(30).stream()
                        .map(StudentResponseDTO::fromStudent)
                        .collect(Collectors.toList())
        );
        return ResponseEntity.ok(statistics);
    }

    // ==================== DELETE METHODS ====================

    /**
     * Delete student by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        log.info("DELETE /api/students/{} - Deleting student", id);
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete student by admission number
     */
    @DeleteMapping("/admission/{admissionNumber}")
    public ResponseEntity<Void> deleteStudentByAdmissionNumber(@PathVariable String admissionNumber) {
        log.info("DELETE /api/students/admission/{} - Deleting student", admissionNumber);
        studentService.deleteStudentByAdmissionNumber(admissionNumber);
        return ResponseEntity.noContent().build();
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Register multiple students in bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<StudentResponseDTO>> registerBulkStudents(
            @Valid @RequestBody List<StudentRequestDTO> studentRequests) {
        log.info("POST /api/students/bulk - Registering {} students in bulk", studentRequests.size());
        List<Student> students = studentRequests.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());

        List<Student> savedStudents = studentService.registerBulkStudents(students);
        List<StudentResponseDTO> response = savedStudents.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Bulk update student classes
     */
    @PatchMapping("/bulk/class")
    public ResponseEntity<Void> bulkUpdateClass(
            @RequestParam String newClass,
            @RequestBody List<Long> studentIds) {
        log.info("PATCH /api/students/bulk/class - Updating {} students to class {}", studentIds.size(), newClass);
        studentService.updateBulkStudentClass(studentIds, newClass);
        return ResponseEntity.ok().build();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Generate admission number
     */
    @GetMapping("/generate-admission")
    public ResponseEntity<Map<String, String>> generateAdmissionNumber() {
        log.debug("GET /api/students/generate-admission - Generating admission number");
        String admissionNumber = studentService.generateAdmissionNumber();
        return ResponseEntity.ok(Map.of("admissionNumber", admissionNumber));
    }

    /**
     * Check if admission number exists
     */
    @GetMapping("/check-admission/{admissionNumber}")
    public ResponseEntity<Map<String, Boolean>> checkAdmissionNumber(
            @PathVariable String admissionNumber) {
        log.debug("GET /api/students/check-admission/{} - Checking admission number", admissionNumber);
        boolean exists = !studentService.isAdmissionNumberUnique(admissionNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ==================== REPORT METHODS ====================

    /**
     * Generate student report
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> generateStudentReport(@PathVariable Long id) {
        log.info("GET /api/students/{}/report - Generating student report", id);
        byte[] report = studentService.generateStudentReport(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=student-report-" + id + ".pdf")
                .body(report);
    }

    /**
     * Generate class report
     */
    @GetMapping("/class/{className}/report")
    public ResponseEntity<byte[]> generateClassReport(@PathVariable String className) {
        log.info("GET /api/students/class/{}/report - Generating class report", className);
        byte[] report = studentService.generateClassReport(className);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=class-report-" + className + ".pdf")
                .body(report);
    }

    // ==================== PROMOTION METHODS ====================

    /**
     * Get promotion preview
     */
    @GetMapping("/promote/preview")
    public ResponseEntity<Map<String, Object>> getPromotionPreview() {
        log.info("GET /api/students/promote/preview - Getting promotion preview");
        Map<String, Object> preview = studentService.getPromotionPreview();
        return ResponseEntity.ok(preview);
    }

    /**
     * Get excluded students
     */
    @GetMapping("/excluded")
    public ResponseEntity<List<StudentResponseDTO>> getExcludedStudents() {
        log.info("GET /api/students/excluded - Fetching excluded students");
        List<Student> excludedStudents = studentService.getExcludedStudents();
        List<StudentResponseDTO> response = excludedStudents.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Promote all eligible students
     */
    @PostMapping("/promote/all")
    public ResponseEntity<Map<String, Object>> promoteAllStudents() {
        log.info("POST /api/students/promote/all - Promoting all eligible students");
        Map<String, Object> result = studentService.promoteAllStudents();
        return ResponseEntity.ok(result);
    }

    /**
     * Promote selected students
     */
    @PostMapping("/promote/selected")
    public ResponseEntity<Map<String, Object>> promoteSelectedStudents(@RequestBody List<Long> studentIds) {
        log.info("POST /api/students/promote/selected - Promoting {} selected students", studentIds.size());
        Map<String, Object> result = studentService.promoteSelectedStudents(studentIds);
        return ResponseEntity.ok(result);
    }

    /**
     * Toggle promotion exclusion for a student
     */
    @PostMapping("/{id}/toggle-exclusion")
    public ResponseEntity<StudentResponseDTO> togglePromotionExclusion(
            @PathVariable Long id,
            @RequestParam boolean exclude,
            @RequestParam(required = false) String reason) {
        log.info("POST /api/students/{}/toggle-exclusion - exclude={}, reason={}", id, exclude, reason);
        Student student = studentService.togglePromotionExclusion(id, exclude, reason);
        return ResponseEntity.ok(StudentResponseDTO.fromStudent(student));
    }

    /**
     * Promote students in a specific class
     */
    @PostMapping("/promote/class/{className}")
    public ResponseEntity<Map<String, Object>> promoteClass(
            @PathVariable String className,
            @RequestParam(required = false) String arm) {
        log.info("POST /api/students/promote/class/{} - Promoting class", className);

        List<Student> students;
        if (arm != null && !arm.isEmpty()) {
            students = studentService.getStudentsByClassAndArm(className, arm);
        } else {
            students = studentService.getStudentsByClass(className);
        }

        List<Long> studentIds = students.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        Map<String, Object> result = studentService.promoteSelectedStudents(studentIds);
        result.put("className", className);
        result.put("arm", arm);

        return ResponseEntity.ok(result);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Save profile picture to disk and return URL
     */
    private String saveProfilePicture(MultipartFile file) throws IOException {
        // Check file size (5MB max)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName != null ?
                originalFileName.substring(originalFileName.lastIndexOf(".")) : ".jpg";
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("File saved: " + filePath.toAbsolutePath());
        System.out.println("File size: " + (file.getSize() / (1024 * 1024)) + "MB");

        // Return URL that can be accessed by frontend
        return "/uploads/profile-pictures/" + fileName;
    }

    /**
     * Map DTO to Entity
     */
    private Student mapToEntity(StudentRequestDTO dto) {
        Student student = new Student();
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());
        student.setStudentClass(dto.getStudentClass());
        student.setClassArm(dto.getClassArm());
        student.setGender(dto.getGender());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setParentName(dto.getParentName());
        student.setParentPhone(dto.getParentPhone());
        student.setParentEmail(dto.getParentEmail());
        student.setAddress(dto.getAddress());
        student.setLocalGovtArea(dto.getLocalGovtArea());
        student.setStateOfOrigin(dto.getStateOfOrigin());
        student.setNationality(dto.getNationality());
        student.setReligion(dto.getReligion());
        student.setEmergencyContactName(dto.getEmergencyContactName());
        student.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        student.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        student.setStatus(dto.getStatus());
        student.setPreviousSchool(dto.getPreviousSchool());

        // Handle excludeFromPromotion - use getExcludeFromPromotion() not isExcludeFromPromotion()
        if (dto.getExcludeFromPromotion() != null) {
            student.setExcludeFromPromotion(dto.getExcludeFromPromotion());
        } else {
            student.setExcludeFromPromotion(false);
        }

        student.setPromotionHoldReason(dto.getPromotionHoldReason());
        student.setProfilePictureUrl(dto.getProfilePictureUrl());
        return student;
    }
}