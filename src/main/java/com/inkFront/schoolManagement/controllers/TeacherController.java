package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ApiResponse;
import com.inkFront.schoolManagement.dto.CompleteRegistrationDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.dto.TeacherClassAttendanceRequest;
import com.inkFront.schoolManagement.dto.TeacherDTO;
import com.inkFront.schoolManagement.dto.TeacherInvitationDTO;
import com.inkFront.schoolManagement.dto.TeacherInviteDTO;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.TeacherInvitation;
import com.inkFront.schoolManagement.model.TeacherSubject;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.TeacherSubjectRepository;
import com.inkFront.schoolManagement.security.JwtService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AttendanceService;
import com.inkFront.schoolManagement.service.EmailService;
import com.inkFront.schoolManagement.service.ResultService;
import com.inkFront.schoolManagement.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final TeacherService teacherService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final SecurityUtils securityUtils;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final AttendanceService attendanceService;
    private final ResultService resultService;
    private final TeacherRepository teacherRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String classKey(String className, String arm) {
        return normalize(className) + "::" + normalize(arm);
    }

    private Long getCurrentTeacherId() {
        var currentUser = securityUtils.getCurrentUser();

        if (currentUser == null || currentUser.getTeacher() == null) {
            throw new RuntimeException("This account is not linked to a teacher");
        }

        return currentUser.getTeacher().getId();
    }

    private SchoolClass validateTeacherCanAccessClass(Long classId) {
        Long teacherId = getCurrentTeacherId();

        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        boolean isFormTeacher =
                schoolClass.getClassTeacher() != null &&
                        schoolClass.getClassTeacher().getId().equals(teacherId);

        boolean teachesClass = teacherSubjectRepository
                .findByTeacher_IdOrderByClassNameAscClassArmAsc(teacherId)
                .stream()
                .anyMatch(ts ->
                        normalize(ts.getClassName()).equals(normalize(schoolClass.getClassName())) &&
                                normalize(ts.getClassArm()).equals(normalize(schoolClass.getArm()))
                );

        if (!isFormTeacher && !teachesClass) {
            throw new RuntimeException("You are not assigned to this class");
        }

        return schoolClass;
    }

    private SchoolClass validateTeacherOwnsFormClass(Long classId) {
        Long teacherId = getCurrentTeacherId();

        SchoolClass schoolClass = classRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (schoolClass.getClassTeacher() == null
                || !schoolClass.getClassTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Only the form teacher of this class can view class results");
        }

        return schoolClass;
    }

    private Map<String, Object> buildTeacherClassResponse(
            SchoolClass schoolClass,
            Long currentTeacherId,
            List<TeacherSubject> teacherSubjects
    ) {
        boolean isFormTeacher =
                schoolClass.getClassTeacher() != null &&
                        schoolClass.getClassTeacher().getId().equals(currentTeacherId);

        List<String> subjects = teacherSubjects.stream()
                .filter(ts ->
                        normalize(ts.getClassName()).equals(normalize(schoolClass.getClassName())) &&
                                normalize(ts.getClassArm()).equals(normalize(schoolClass.getArm())) &&
                                ts.getSubject() != null &&
                                ts.getSubject().getName() != null
                )
                .map(ts -> ts.getSubject().getName().trim())
                .distinct()
                .sorted()
                .toList();

        int studentCount = studentRepository
                .findBySchoolClassIdOrderByLastNameAscFirstNameAsc(schoolClass.getId())
                .size();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", schoolClass.getId());
        item.put("className", schoolClass.getClassName());
        item.put("arm", schoolClass.getArm());
        item.put("classCode", schoolClass.getClassCode());
        item.put("category", schoolClass.getCategory());
        item.put("description", schoolClass.getDescription());
        item.put("capacity", schoolClass.getCapacity());
        item.put("studentCount", studentCount);
        item.put("subjects", subjects);
        item.put("isFormTeacher", isFormTeacher);

        if (schoolClass.getClassTeacher() != null) {
            item.put("classTeacherId", schoolClass.getClassTeacher().getId());
            item.put(
                    "classTeacherName",
                    (schoolClass.getClassTeacher().getFirstName() == null ? "" : schoolClass.getClassTeacher().getFirstName()) +
                            " " +
                            (schoolClass.getClassTeacher().getLastName() == null ? "" : schoolClass.getClassTeacher().getLastName())
            );
        } else {
            item.put("classTeacherId", null);
            item.put("classTeacherName", null);
        }

        return item;
    }

    @GetMapping
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        log.info("GET /api/teachers - Fetching all teachers");
        List<TeacherDTO> teachers = teacherService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<TeacherDTO>> getTeachersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("GET /api/teachers/paginated - Page: {}, Size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TeacherDTO> teachers = teacherService.getAllTeachersPaginated(pageable);
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherDTO> getTeacher(@PathVariable Long id) {
        log.info("GET /api/teachers/{} - Fetching teacher", id);
        TeacherDTO teacher = teacherService.getTeacherDTO(id);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping("/teacher-id/{teacherId}")
    public ResponseEntity<TeacherDTO> getTeacherByTeacherId(@PathVariable String teacherId) {
        log.info("GET /api/teachers/teacher-id/{} - Fetching teacher", teacherId);
        TeacherDTO teacher = teacherService.getTeacherByTeacherId(teacherId);
        return ResponseEntity.ok(teacher);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<TeacherDTO> createTeacher(
            @RequestPart("teacher") TeacherDTO teacherDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        log.info("POST /api/teachers - Creating new teacher: {}", teacherDTO.getEmail());
        TeacherDTO createdTeacher = teacherService.createTeacher(teacherDTO, profilePicture);
        return new ResponseEntity<>(createdTeacher, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<TeacherDTO> updateTeacher(
            @PathVariable Long id,
            @RequestPart("teacher") TeacherDTO teacherDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        log.info("PUT /api/teachers/{} - Updating teacher", id);
        TeacherDTO updatedTeacher = teacherService.updateTeacher(id, teacherDTO, profilePicture);
        return ResponseEntity.ok(updatedTeacher);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        log.info("DELETE /api/teachers/{} - Deleting teacher", id);
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TeacherDTO>> searchTeachers(@RequestParam String term) {
        log.info("GET /api/teachers/search - Searching teachers with term: {}", term);
        List<TeacherDTO> teachers = teacherService.searchTeachers(term);
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TeacherDTO>> getTeachersByStatus(@PathVariable String status) {
        log.info("GET /api/teachers/status/{} - Fetching teachers by status", status);
        List<TeacherDTO> teachers = teacherService.getTeachersByStatus(status);
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/subject/{subject}")
    public ResponseEntity<List<TeacherDTO>> getTeachersBySubject(@PathVariable String subject) {
        log.info("GET /api/teachers/subject/{} - Fetching teachers by subject", subject);
        List<TeacherDTO> teachers = teacherService.getTeachersBySubject(subject);
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<TeacherDTO>> getTeachersByDepartment(@PathVariable String department) {
        log.info("GET /api/teachers/department/{} - Fetching teachers by department", department);
        List<TeacherDTO> teachers = teacherService.getTeachersByDepartment(department);
        return ResponseEntity.ok(teachers);
    }

    @PostMapping("/{id}/subjects")
    public ResponseEntity<TeacherDTO> addSubject(@PathVariable Long id, @RequestParam String subject) {
        log.info("POST /api/teachers/{}/subjects - Adding subject: {}", id, subject);
        TeacherDTO updatedTeacher = teacherService.addSubject(id, subject);
        return ResponseEntity.ok(updatedTeacher);
    }

    @DeleteMapping("/{id}/subjects")
    public ResponseEntity<TeacherDTO> removeSubject(@PathVariable Long id, @RequestParam String subject) {
        log.info("DELETE /api/teachers/{}/subjects - Removing subject: {}", id, subject);
        TeacherDTO updatedTeacher = teacherService.removeSubject(id, subject);
        return ResponseEntity.ok(updatedTeacher);
    }

    @PostMapping("/{id}/qualifications")
    public ResponseEntity<TeacherDTO> addQualification(@PathVariable Long id, @RequestParam String qualification) {
        log.info("POST /api/teachers/{}/qualifications - Adding qualification: {}", id, qualification);
        TeacherDTO updatedTeacher = teacherService.addQualification(id, qualification);
        return ResponseEntity.ok(updatedTeacher);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TeacherDTO> updateEmploymentStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("PATCH /api/teachers/{}/status - Updating status to: {}", id, status);
        TeacherDTO updatedTeacher = teacherService.updateEmploymentStatus(id, status);
        return ResponseEntity.ok(updatedTeacher);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTeacherStatistics() {
        log.info("GET /api/teachers/statistics - Fetching teacher statistics");
        Map<String, Object> statistics = teacherService.getTeacherStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/generate-id")
    public ResponseEntity<Map<String, String>> generateTeacherId() {
        log.info("GET /api/teachers/generate-id - Generating teacher ID");
        String teacherId = teacherService.generateTeacherId();
        Map<String, String> response = new HashMap<>();
        response.put("teacherId", teacherId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        log.info("GET /api/teachers/check-email - Checking email: {}", email);
        boolean exists = teacherService.checkEmailExists(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-teacher-id")
    public ResponseEntity<Map<String, Boolean>> checkTeacherIdExists(@RequestParam String teacherId) {
        log.info("GET /api/teachers/check-teacher-id - Checking teacher ID: {}", teacherId);
        boolean exists = teacherService.checkTeacherIdExists(teacherId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPDF() {
        log.info("GET /api/teachers/export/pdf - Exporting teachers to PDF");
        byte[] pdfBytes = teacherService.exportToPDF();
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=teachers.pdf")
                .body(pdfBytes);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel() {
        log.info("GET /api/teachers/export/excel - Exporting teachers to Excel");
        byte[] excelBytes = teacherService.exportToExcel();
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.ms-excel")
                .header("Content-Disposition", "attachment; filename=teachers.xlsx")
                .body(excelBytes);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse> inviteTeacher(@RequestBody TeacherInviteDTO inviteDTO) {
        log.info("POST /api/teachers/invite - Inviting teacher with email: {}", inviteDTO.getEmail());

        try {
            if (inviteDTO.getEmail() == null || inviteDTO.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Email is required"));
            }
            if (inviteDTO.getFirstName() == null || inviteDTO.getFirstName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "First name is required"));
            }
            if (inviteDTO.getLastName() == null || inviteDTO.getLastName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Last name is required"));
            }

            String token = UUID.randomUUID().toString();
            teacherService.createInvitation(inviteDTO, token);

            String registrationLink = "http://localhost:3000/complete-teacher-registration?token=" + token;
            emailService.sendTeacherInvitation(
                    inviteDTO.getEmail(),
                    registrationLink,
                    inviteDTO.getFirstName()
            );

            log.info("Invitation sent successfully to: {}", inviteDTO.getEmail());
            return ResponseEntity.ok(
                    new ApiResponse(true, "Invitation sent successfully to " + inviteDTO.getEmail())
            );
        } catch (Exception e) {
            log.error("Error sending invitation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to send invitation: " + e.getMessage()));
        }
    }

    @GetMapping("/verify-invitation")
    public ResponseEntity<?> verifyInvitationToken(@RequestParam String token) {
        log.info("GET /api/teachers/verify-invitation - Verifying invitation token: {}", token);

        try {
            TeacherInvitation invitation = teacherService.verifyInvitationToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("firstName", invitation.getFirstName());
            response.put("lastName", invitation.getLastName());
            response.put("email", invitation.getEmail());
            response.put("phoneNumber", invitation.getPhoneNumber());
            response.put("expiryDate", invitation.getExpiryDate());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeTeacherRegistration(@RequestBody CompleteRegistrationDTO completeDTO) {
        log.info("POST /api/teachers/complete-registration - Completing registration for token: {}",
                completeDTO.getToken());

        try {
            if (completeDTO.getToken() == null || completeDTO.getToken().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Token is required"));
            }
            if (completeDTO.getUsername() == null || completeDTO.getUsername().length() < 3) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Username must be at least 3 characters"));
            }
            if (completeDTO.getPassword() == null || completeDTO.getPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Password must be at least 6 characters"));
            }

            User user = teacherService.completeRegistration(completeDTO);

            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            LoginResponse response = LoginResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(LoginResponse.UserResponse.fromUser(user))
                    .build();

            log.info("Teacher registration completed successfully for: {}", user.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error completing registration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/resend-invitation")
    public ResponseEntity<ApiResponse> resendInvitation(@RequestParam String email) {
        log.info("POST /api/teachers/resend-invitation - Resending invitation to: {}", email);

        try {
            teacherService.resendInvitation(email);
            return ResponseEntity.ok(new ApiResponse(true, "Invitation resent successfully to " + email));
        } catch (Exception e) {
            log.error("Error resending invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<TeacherInvitationDTO>> getPendingInvitations() {
        log.info("GET /api/teachers/invitations/pending - Fetching pending invitations");
        List<TeacherInvitationDTO> invitations = teacherService.getPendingInvitations();
        return ResponseEntity.ok(invitations);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<ApiResponse> cancelInvitation(@PathVariable Long invitationId) {
        log.info("DELETE /api/teachers/invitations/{} - Cancelling invitation", invitationId);

        try {
            teacherService.cancelInvitation(invitationId);
            return ResponseEntity.ok(new ApiResponse(true, "Invitation cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyTeacherProfile() {
        var currentUser = securityUtils.getCurrentUser();

        if (currentUser.getTeacher() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "This account is not linked to a teacher"));
        }

        Teacher teacher = teacherRepository.findByUserIdWithDetails(currentUser.getId())
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found with user ID: " + currentUser.getId()));

        return ResponseEntity.ok(TeacherDTO.fromEntity(teacher));
    }

    @GetMapping("/me/subject-assignments")
    public ResponseEntity<?> getMySubjectAssignments() {
        Long teacherId = getCurrentTeacherId();

        List<Map<String, Object>> assignments = teacherSubjectRepository
                .findByTeacher_IdOrderByClassNameAscClassArmAsc(teacherId)
                .stream()
                .map(ts -> {
                    Map<String, Object> item = new HashMap<>();

                    SchoolClass schoolClass = null;
                    if (ts.getClassName() != null && ts.getClassArm() != null) {
                        schoolClass = classRepository
                                .findByClassNameAndArmNormalized(ts.getClassName(), ts.getClassArm())
                                .orElse(null);
                    }

                    item.put("id", ts.getId());
                    item.put("classId", schoolClass != null ? schoolClass.getId() : null);
                    item.put("subjectId", ts.getSubject() != null ? ts.getSubject().getId() : null);
                    item.put("subjectName", ts.getSubject() != null ? ts.getSubject().getName() : null);
                    item.put("className", ts.getClassName());
                    item.put("classArm", ts.getClassArm());
                    return item;
                })
                .toList();

        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/me/classes")
    public ResponseEntity<?> getMyClasses() {
        Long teacherId = getCurrentTeacherId();

        List<SchoolClass> formClasses = classRepository.findByClassTeacherIdWithTeacher(teacherId);
        List<TeacherSubject> subjectAssignments =
                teacherSubjectRepository.findByTeacher_IdOrderByClassNameAscClassArmAsc(teacherId);

        Map<String, SchoolClass> merged = new LinkedHashMap<>();

        for (SchoolClass schoolClass : formClasses) {
            merged.put(classKey(schoolClass.getClassName(), schoolClass.getArm()), schoolClass);
        }

        for (TeacherSubject ts : subjectAssignments) {
            if (ts.getClassName() == null || ts.getClassArm() == null) {
                continue;
            }

            classRepository.findByClassNameAndArmNormalized(ts.getClassName(), ts.getClassArm())
                    .ifPresent(schoolClass ->
                            merged.putIfAbsent(
                                    classKey(schoolClass.getClassName(), schoolClass.getArm()),
                                    schoolClass
                            )
                    );
        }

        List<Map<String, Object>> classes = merged.values().stream()
                .sorted(Comparator
                        .comparing((SchoolClass c) -> normalize(c.getClassName()))
                        .thenComparing(c -> normalize(c.getArm())))
                .map(schoolClass -> buildTeacherClassResponse(schoolClass, teacherId, subjectAssignments))
                .collect(Collectors.toList());

        return ResponseEntity.ok(classes);
    }

    @GetMapping("/me/classes/{classId}/students")
    public ResponseEntity<?> getMyClassStudents(@PathVariable Long classId) {
        SchoolClass schoolClass = validateTeacherCanAccessClass(classId);

        List<StudentResponseDTO> students = studentRepository
                .findBySchoolClassIdOrderByLastNameAscFirstNameAsc(schoolClass.getId())
                .stream()
                .map(StudentResponseDTO::fromStudent)
                .toList();

        return ResponseEntity.ok(students);
    }

    @GetMapping("/me/classes/{classId}/results")
    public ResponseEntity<?> getMyClassResults(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        SchoolClass schoolClass = validateTeacherOwnsFormClass(classId);

        return ResponseEntity.ok(
                resultService.getClassRankings(
                        schoolClass.getClassName(),
                        schoolClass.getArm(),
                        session,
                        term
                )
        );
    }

    @GetMapping("/me/classes/{classId}/attendance")
    public ResponseEntity<?> getMyClassAttendance(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String session,
            @RequestParam Result.Term term) {

        SchoolClass schoolClass = validateTeacherCanAccessClass(classId);

        return ResponseEntity.ok(
                attendanceService.getClassAttendance(
                        schoolClass.getClassName(),
                        schoolClass.getArm(),
                        date,
                        session,
                        term
                )
        );
    }

    @PostMapping("/me/classes/{classId}/attendance")
    public ResponseEntity<?> markMyClassAttendance(
            @PathVariable Long classId,
            @RequestBody TeacherClassAttendanceRequest request) {

        SchoolClass schoolClass = validateTeacherCanAccessClass(classId);

        List<Student> classStudents = studentRepository
                .findBySchoolClassIdOrderByLastNameAscFirstNameAsc(schoolClass.getId());

        List<Long> allowedStudentIds = classStudents.stream()
                .map(Student::getId)
                .toList();

        boolean hasInvalidStudent = request.getStudentIds().stream()
                .anyMatch(id -> !allowedStudentIds.contains(id));

        if (hasInvalidStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "One or more students do not belong to your class arm"));
        }

        return ResponseEntity.ok(
                attendanceService.markBulkAttendance(
                        request.getStudentIds(),
                        request.getDate(),
                        request.getSession(),
                        request.getTerm(),
                        request.getStatus()
                )
        );
    }
}