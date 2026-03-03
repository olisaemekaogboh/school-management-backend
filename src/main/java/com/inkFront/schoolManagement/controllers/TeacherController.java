// src/main/java/com/inkFront/schoolManagement/controllers/TeacherController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.model.TeacherInvitation;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.JwtService;
import com.inkFront.schoolManagement.service.EmailService;
import com.inkFront.schoolManagement.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Slf4j

public class TeacherController {

    private final TeacherService teacherService;
    private final EmailService emailService;
    private final JwtService jwtService;

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Get all teachers
     */
    @GetMapping
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        log.info("GET /api/teachers - Fetching all teachers");
        List<TeacherDTO> teachers = teacherService.getAllTeachers();
        return ResponseEntity.ok(teachers);
    }

    /**
     * Get teachers with pagination
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<TeacherDTO>> getTeachersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("GET /api/teachers/paginated - Page: {}, Size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TeacherDTO> teachers = teacherService.getAllTeachersPaginated(pageable);
        return ResponseEntity.ok(teachers);
    }

    /**
     * Get teacher by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeacherDTO> getTeacher(@PathVariable Long id) {
        log.info("GET /api/teachers/{} - Fetching teacher", id);
        TeacherDTO teacher = teacherService.getTeacherDTO(id);
        return ResponseEntity.ok(teacher);
    }

    /**
     * Get teacher by teacher ID (e.g., TCH240001)
     */
    @GetMapping("/teacher-id/{teacherId}")
    public ResponseEntity<TeacherDTO> getTeacherByTeacherId(@PathVariable String teacherId) {
        log.info("GET /api/teachers/teacher-id/{} - Fetching teacher", teacherId);
        TeacherDTO teacher = teacherService.getTeacherByTeacherId(teacherId);
        return ResponseEntity.ok(teacher);
    }

    /**
     * Create a new teacher
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<TeacherDTO> createTeacher(
            @RequestPart("teacher") TeacherDTO teacherDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        log.info("POST /api/teachers - Creating new teacher: {}", teacherDTO.getEmail());
        TeacherDTO createdTeacher = teacherService.createTeacher(teacherDTO, profilePicture);
        return new ResponseEntity<>(createdTeacher, HttpStatus.CREATED);
    }

    /**
     * Update an existing teacher
     */
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<TeacherDTO> updateTeacher(
            @PathVariable Long id,
            @RequestPart("teacher") TeacherDTO teacherDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        log.info("PUT /api/teachers/{} - Updating teacher", id);
        TeacherDTO updatedTeacher = teacherService.updateTeacher(id, teacherDTO, profilePicture);
        return ResponseEntity.ok(updatedTeacher);
    }

    /**
     * Delete a teacher
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        log.info("DELETE /api/teachers/{} - Deleting teacher", id);
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }

    // ========== SEARCH AND FILTER OPERATIONS ==========

    /**
     * Search teachers by name, email, or teacher ID
     */
    @GetMapping("/search")
    public ResponseEntity<List<TeacherDTO>> searchTeachers(@RequestParam String term) {
        log.info("GET /api/teachers/search - Searching teachers with term: {}", term);
        List<TeacherDTO> teachers = teacherService.searchTeachers(term);
        return ResponseEntity.ok(teachers);
    }

    /**
     * Get teachers by status (ACTIVE, INACTIVE, ON_LEAVE)
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TeacherDTO>> getTeachersByStatus(@PathVariable String status) {
        log.info("GET /api/teachers/status/{} - Fetching teachers by status", status);
        List<TeacherDTO> teachers = teacherService.getTeachersByStatus(status);
        return ResponseEntity.ok(teachers);
    }

    /**
     * Get teachers by subject they teach
     */
    @GetMapping("/subject/{subject}")
    public ResponseEntity<List<TeacherDTO>> getTeachersBySubject(@PathVariable String subject) {
        log.info("GET /api/teachers/subject/{} - Fetching teachers by subject", subject);
        List<TeacherDTO> teachers = teacherService.getTeachersBySubject(subject);
        return ResponseEntity.ok(teachers);
    }

    /**
     * Get teachers by department
     */
    @GetMapping("/department/{department}")
    public ResponseEntity<List<TeacherDTO>> getTeachersByDepartment(@PathVariable String department) {
        log.info("GET /api/teachers/department/{} - Fetching teachers by department", department);
        List<TeacherDTO> teachers = teacherService.getTeachersByDepartment(department);
        return ResponseEntity.ok(teachers);
    }

    // ========== SUBJECT AND QUALIFICATION MANAGEMENT ==========

    /**
     * Add a subject to a teacher
     */
    @PostMapping("/{id}/subjects")
    public ResponseEntity<TeacherDTO> addSubject(@PathVariable Long id, @RequestParam String subject) {
        log.info("POST /api/teachers/{}/subjects - Adding subject: {}", id, subject);
        TeacherDTO updatedTeacher = teacherService.addSubject(id, subject);
        return ResponseEntity.ok(updatedTeacher);
    }

    /**
     * Remove a subject from a teacher
     */
    @DeleteMapping("/{id}/subjects")
    public ResponseEntity<TeacherDTO> removeSubject(@PathVariable Long id, @RequestParam String subject) {
        log.info("DELETE /api/teachers/{}/subjects - Removing subject: {}", id, subject);
        TeacherDTO updatedTeacher = teacherService.removeSubject(id, subject);
        return ResponseEntity.ok(updatedTeacher);
    }

    /**
     * Add a qualification to a teacher
     */
    @PostMapping("/{id}/qualifications")
    public ResponseEntity<TeacherDTO> addQualification(@PathVariable Long id, @RequestParam String qualification) {
        log.info("POST /api/teachers/{}/qualifications - Adding qualification: {}", id, qualification);
        TeacherDTO updatedTeacher = teacherService.addQualification(id, qualification);
        return ResponseEntity.ok(updatedTeacher);
    }

    /**
     * Update teacher's employment status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TeacherDTO> updateEmploymentStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("PATCH /api/teachers/{}/status - Updating status to: {}", id, status);
        TeacherDTO updatedTeacher = teacherService.updateEmploymentStatus(id, status);
        return ResponseEntity.ok(updatedTeacher);
    }

    // ========== STATISTICS AND UTILITIES ==========

    /**
     * Get teacher statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTeacherStatistics() {
        log.info("GET /api/teachers/statistics - Fetching teacher statistics");
        Map<String, Object> statistics = teacherService.getTeacherStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Generate a new teacher ID
     */
    @GetMapping("/generate-id")
    public ResponseEntity<Map<String, String>> generateTeacherId() {
        log.info("GET /api/teachers/generate-id - Generating teacher ID");
        String teacherId = teacherService.generateTeacherId();
        Map<String, String> response = new HashMap<>();
        response.put("teacherId", teacherId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if email already exists
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        log.info("GET /api/teachers/check-email - Checking email: {}", email);
        boolean exists = teacherService.checkEmailExists(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if teacher ID already exists
     */
    @GetMapping("/check-teacher-id")
    public ResponseEntity<Map<String, Boolean>> checkTeacherIdExists(@RequestParam String teacherId) {
        log.info("GET /api/teachers/check-teacher-id - Checking teacher ID: {}", teacherId);
        boolean exists = teacherService.checkTeacherIdExists(teacherId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Export teachers to PDF
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPDF() {
        log.info("GET /api/teachers/export/pdf - Exporting teachers to PDF");
        byte[] pdfBytes = teacherService.exportToPDF();
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=teachers.pdf")
                .body(pdfBytes);
    }

    /**
     * Export teachers to Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel() {
        log.info("GET /api/teachers/export/excel - Exporting teachers to Excel");
        byte[] excelBytes = teacherService.exportToExcel();
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.ms-excel")
                .header("Content-Disposition", "attachment; filename=teachers.xlsx")
                .body(excelBytes);
    }

    // ========== INVITATION METHODS ==========

    /**
     * Send an invitation to a teacher to complete registration
     */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse> inviteTeacher(@RequestBody TeacherInviteDTO inviteDTO) {
        log.info("POST /api/teachers/invite - Inviting teacher with email: {}", inviteDTO.getEmail());

        try {
            // Validate input
            if (inviteDTO.getEmail() == null || inviteDTO.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Email is required"));
            }
            if (inviteDTO.getFirstName() == null || inviteDTO.getFirstName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "First name is required"));
            }
            if (inviteDTO.getLastName() == null || inviteDTO.getLastName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Last name is required"));
            }

            // Generate unique token
            String token = UUID.randomUUID().toString();

            // Save invitation
            teacherService.createInvitation(inviteDTO, token);

            // Send email with registration link
            String registrationLink = "http://localhost:3000/complete-teacher-registration?token=" + token;
            emailService.sendTeacherInvitation(inviteDTO.getEmail(), registrationLink, inviteDTO.getFirstName());

            log.info("Invitation sent successfully to: {}", inviteDTO.getEmail());
            return ResponseEntity.ok(new ApiResponse(true, "Invitation sent successfully to " + inviteDTO.getEmail()));
        } catch (Exception e) {
            log.error("Error sending invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to send invitation: " + e.getMessage()));
        }
    }

    /**
     * Verify an invitation token
     */
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

    /**
     * Complete teacher registration (set username and password)
     */
    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeTeacherRegistration(@RequestBody CompleteRegistrationDTO completeDTO) {
        log.info("POST /api/teachers/complete-registration - Completing registration for token: {}", completeDTO.getToken());

        try {
            // Validate input
            if (completeDTO.getToken() == null || completeDTO.getToken().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Token is required"));
            }
            if (completeDTO.getUsername() == null || completeDTO.getUsername().length() < 3) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Username must be at least 3 characters"));
            }
            if (completeDTO.getPassword() == null || completeDTO.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Password must be at least 6 characters"));
            }

            // Complete registration
            User user = teacherService.completeRegistration(completeDTO);

            // Generate JWT token
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

    /**
     * Resend invitation email
     */
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

    /**
     * Get all pending invitations
     */
    @GetMapping("/invitations/pending")
    public ResponseEntity<List<TeacherInvitationDTO>> getPendingInvitations() {
        log.info("GET /api/teachers/invitations/pending - Fetching pending invitations");
        List<TeacherInvitationDTO> invitations = teacherService.getPendingInvitations();
        return ResponseEntity.ok(invitations);
    }

    /**
     * Cancel an invitation
     */
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
}