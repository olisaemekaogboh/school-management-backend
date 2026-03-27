package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkFront.schoolManagement.dto.StudentRequestDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);
    private static final String UPLOAD_DIR = "uploads/profile-pictures/";

    private final StudentService studentService;
    private final SecurityUtils securityUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDTO> registerStudentWithFile(
            @RequestPart(value = "student", required = false) String studentJson,
            @RequestPart(value = "studentJson", required = false) String legacyStudentJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            StudentRequestDTO studentRequest = parseStudentRequest(studentJson, legacyStudentJson);
            MultipartFile imageFile = resolveMultipartFile(profilePicture, file);

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileUrl = saveProfilePicture(imageFile);
                studentRequest.setProfilePictureUrl(fileUrl);
            }

            Student student = mapToEntity(studentRequest);
            Student savedStudent = studentService.registerStudent(student);

            return new ResponseEntity<>(StudentResponseDTO.fromStudent(savedStudent), HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Error registering student with file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDTO> registerStudentWithFilePart(
            @RequestPart(value = "student", required = false) StudentRequestDTO studentRequest,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            if (studentRequest == null) {
                return ResponseEntity.badRequest().build();
            }

            MultipartFile imageFile = resolveMultipartFile(profilePicture, file);

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileUrl = saveProfilePicture(imageFile);
                studentRequest.setProfilePictureUrl(fileUrl);
            }

            Student student = mapToEntity(studentRequest);
            Student savedStudent = studentService.registerStudent(student);

            return new ResponseEntity<>(StudentResponseDTO.fromStudent(savedStudent), HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Error registering student with @RequestPart", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentResponseDTO> registerStudentJson(
            @Valid @RequestBody StudentRequestDTO studentRequest) {

        Student student = mapToEntity(studentRequest);
        Student savedStudent = studentService.registerStudent(student);

        return new ResponseEntity<>(StudentResponseDTO.fromStudent(savedStudent), HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDTO> updateStudentWithFile(
            @PathVariable Long id,
            @RequestPart(value = "student", required = false) String studentJson,
            @RequestPart(value = "studentJson", required = false) String legacyStudentJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            StudentRequestDTO studentRequest = parseStudentRequest(studentJson, legacyStudentJson);
            MultipartFile imageFile = resolveMultipartFile(profilePicture, file);

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileUrl = saveProfilePicture(imageFile);
                studentRequest.setProfilePictureUrl(fileUrl);
            }

            Student student = mapToEntity(studentRequest);
            Student updatedStudent = studentService.updateStudent(id, student);

            return ResponseEntity.ok(StudentResponseDTO.fromStudent(updatedStudent));

        } catch (Exception e) {
            log.error("Error updating student with file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentResponseDTO> updateStudentJson(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO studentRequest) {

        Student student = mapToEntity(studentRequest);
        Student updatedStudent = studentService.updateStudent(id, student);
        return ResponseEntity.ok(StudentResponseDTO.fromStudent(updatedStudent));
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<StudentResponseDTO>> getAllStudentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Student> studentPage = studentService.getAllStudentsPaginated(pageable);
        Page<StudentResponseDTO> responsePage = studentPage.map(StudentResponseDTO::fromStudent);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id)
                .map(student -> ResponseEntity.ok(StudentResponseDTO.fromStudent(student)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admission/{admissionNumber}")
    public ResponseEntity<StudentResponseDTO> getStudentByAdmissionNumber(@PathVariable String admissionNumber) {
        return studentService.getStudentByAdmissionNumber(admissionNumber)
                .map(student -> ResponseEntity.ok(StudentResponseDTO.fromStudent(student)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<StudentResponseDTO>> searchStudents(@RequestParam String term) {
        List<Student> students = studentService.searchStudents(term);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class/{className}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByClass(@PathVariable String className) {
        List<Student> students = studentService.getStudentsByClass(className);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class/{className}/arm/{arm}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByClassAndArm(
            @PathVariable String className,
            @PathVariable String arm) {

        List<Student> students = studentService.getStudentsByClassAndArm(className, arm);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/state/{state}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByState(@PathVariable String state) {
        List<Student> students = studentService.getStudentsByState(state);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lga/{lga}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByLGA(@PathVariable String lga) {
        List<Student> students = studentService.getStudentsByLGA(lga);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<StudentResponseDTO>> getActiveStudents() {
        List<Student> students = studentService.getActiveStudents();
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<StudentResponseDTO>> getStudentsByStatus(
            @PathVariable Student.StudentStatus status) {
        List<Student> students = studentService.getStudentsByStatus(status);
        List<StudentResponseDTO> response = students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStudentStatistics() {
        Map<String, Object> statistics = new java.util.HashMap<>();
        statistics.put("totalStudents", studentService.getTotalStudentCount());
        statistics.put("activeStudents", studentService.getActiveStudentCount());
        statistics.put("studentsByClass", studentService.getStudentCountByClass());
        statistics.put(
                "recentAdmissions",
                studentService.getRecentAdmissions(30).stream()
                        .map(StudentResponseDTO::fromStudent)
                        .collect(Collectors.toList())
        );

        return ResponseEntity.ok(statistics);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admission/{admissionNumber}")
    public ResponseEntity<Void> deleteStudentByAdmissionNumber(@PathVariable String admissionNumber) {
        studentService.deleteStudentByAdmissionNumber(admissionNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<StudentResponseDTO>> registerBulkStudents(
            @Valid @RequestBody List<StudentRequestDTO> studentRequests) {

        List<Student> students = studentRequests.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());

        List<Student> savedStudents = studentService.registerBulkStudents(students);
        List<StudentResponseDTO> response = savedStudents.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/bulk/class")
    public ResponseEntity<Void> bulkUpdateClass(
            @RequestParam String newClass,
            @RequestBody List<Long> studentIds) {

        studentService.updateBulkStudentClass(studentIds, newClass);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/generate-admission")
    public ResponseEntity<Map<String, String>> generateAdmissionNumber() {
        String admissionNumber = studentService.generateAdmissionNumber();
        return ResponseEntity.ok(Map.of("admissionNumber", admissionNumber));
    }

    @GetMapping("/check-admission/{admissionNumber}")
    public ResponseEntity<Map<String, Boolean>> checkAdmissionNumber(@PathVariable String admissionNumber) {
        boolean exists = !studentService.isAdmissionNumberUnique(admissionNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> generateStudentReport(@PathVariable Long id) {
        byte[] report = studentService.generateStudentReport(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=student-report-" + id + ".pdf")
                .body(report);
    }

    @GetMapping("/class/{className}/report")
    public ResponseEntity<byte[]> generateClassReport(@PathVariable String className) {
        byte[] report = studentService.generateClassReport(className);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=class-report-" + className + ".pdf")
                .body(report);
    }

    @GetMapping("/promote/preview")
    public ResponseEntity<Map<String, Object>> getPromotionPreview() {
        Map<String, Object> preview = studentService.getPromotionPreview();
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/excluded")
    public ResponseEntity<List<StudentResponseDTO>> getExcludedStudents() {
        List<Student> excludedStudents = studentService.getExcludedStudents();
        List<StudentResponseDTO> response = excludedStudents.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/promote/all")
    public ResponseEntity<Map<String, Object>> promoteAllStudents() {
        Map<String, Object> result = studentService.promoteAllStudents();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/promote/selected")
    public ResponseEntity<Map<String, Object>> promoteSelectedStudents(@RequestBody List<Long> studentIds) {
        Map<String, Object> result = studentService.promoteSelectedStudents(studentIds);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/toggle-exclusion")
    public ResponseEntity<StudentResponseDTO> togglePromotionExclusion(
            @PathVariable Long id,
            @RequestParam boolean exclude,
            @RequestParam(required = false) String reason) {

        Student student = studentService.togglePromotionExclusion(id, exclude, reason);
        return ResponseEntity.ok(StudentResponseDTO.fromStudent(student));
    }

    @PostMapping("/promote/class/{className}")
    public ResponseEntity<Map<String, Object>> promoteClass(
            @PathVariable String className,
            @RequestParam(required = false) String arm) {

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

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        var currentUser = securityUtils.getCurrentUser();

        if (currentUser.getStudent() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "This account is not linked to a student"));
        }

        return ResponseEntity.ok(StudentResponseDTO.fromStudent(currentUser.getStudent()));
    }

    private StudentRequestDTO parseStudentRequest(String studentJson, String legacyStudentJson) throws IOException {
        String payload = StringUtils.hasText(studentJson) ? studentJson : legacyStudentJson;

        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("Student payload is required");
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper.readValue(payload, StudentRequestDTO.class);
    }

    private MultipartFile resolveMultipartFile(MultipartFile profilePicture, MultipartFile file) {
        if (profilePicture != null && !profilePicture.isEmpty()) {
            return profilePicture;
        }
        if (file != null && !file.isEmpty()) {
            return file;
        }
        return null;
    }

    private String saveProfilePicture(MultipartFile file) throws IOException {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = (originalFileName != null && originalFileName.contains("."))
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";

        String fileName = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/profile-pictures/" + fileName;
    }

    private Student mapToEntity(StudentRequestDTO dto) {
        Student student = new Student();
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(dto.getClassId());
        student.setSchoolClass(schoolClass);

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
        student.setExcludeFromPromotion(dto.getExcludeFromPromotion() != null && dto.getExcludeFromPromotion());
        student.setPromotionHoldReason(dto.getPromotionHoldReason());
        student.setProfilePictureUrl(dto.getProfilePictureUrl());

        return student;
    }
}