package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AttendanceService;
import com.inkFront.schoolManagement.service.ParentService;
import com.inkFront.schoolManagement.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
@Slf4j
public class ParentController {

    private final ParentService parentService;
    private final StudentRepository studentRepository;
    private final SecurityUtils securityUtils;
    private final ResultService resultService;
    private final AttendanceService attendanceService;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    private void validateTeacherOrAdmin() {
        User user = currentUser();
        if (!(user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.TEACHER)) {
            throw new AccessDeniedException("Only admin or teacher can perform this action");
        }
    }

    private void validateWardAccess(Long studentId) {
        User currentUser = securityUtils.getCurrentUser();

        if (currentUser.getParent() == null) {
            throw new RuntimeException("This account is not linked to a parent");
        }

        boolean allowed = studentRepository.findById(studentId)
                .map(student -> student.getParent() != null &&
                        student.getParent().getId().equals(currentUser.getParent().getId()))
                .orElse(false);

        if (!allowed) {
            throw new RuntimeException("You are not allowed to access this student's record");
        }
    }

    @PostMapping
    public ResponseEntity<?> createParent(@RequestBody ParentDTO parentDTO) {
        validateTeacherOrAdmin();
        try {
            log.info("POST /api/parents - Creating parent with email: {}", parentDTO.getEmail());
            ParentDTO createdParent = parentService.createParent(parentDTO);
            return new ResponseEntity<>(createdParent, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating parent: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<ParentDTO>> getAllParents() {
        validateTeacherOrAdmin();
        log.info("GET /api/parents - Fetching all parents");
        List<ParentDTO> parents = parentService.getAllParents();
        return ResponseEntity.ok(parents);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<ParentDTO>> getParentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        validateTeacherOrAdmin();

        log.info("GET /api/parents/paginated - Page: {}, Size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ParentDTO> parents = parentService.getAllParentsPaginated(pageable);
        return ResponseEntity.ok(parents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getParentById(@PathVariable Long id) {
        User user = currentUser();

        if (user.getRole() == User.Role.PARENT) {
            if (user.getParent() == null || !user.getParent().getId().equals(id)) {
                throw new AccessDeniedException("You can only access your own record");
            }
        } else if (!(user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.TEACHER)) {
            throw new AccessDeniedException("Access denied");
        }

        log.info("GET /api/parents/{} - Fetching parent by ID", id);
        return parentService.getParentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        User currentUser = securityUtils.getCurrentUser();

        if (currentUser.getParent() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "This account is not linked to a parent"));
        }

        return ResponseEntity.ok(currentUser.getParent());
    }

    @GetMapping("/me/wards")
    public ResponseEntity<?> getMyWards() {
        User currentUser = securityUtils.getCurrentUser();

        if (currentUser.getParent() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "This account is not linked to a parent"));
        }

        List<Student> wards = studentRepository.findAll().stream()
                .filter(student -> student.getParent() != null &&
                        student.getParent().getId().equals(currentUser.getParent().getId()))
                .toList();

        return ResponseEntity.ok(wards);
    }

    @GetMapping("/by-email")
    public ResponseEntity<?> getParentByEmail(@RequestParam String email) {
        validateTeacherOrAdmin();
        log.info("GET /api/parents/by-email - Fetching parent with email: {}", email);

        return parentService.getParentByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyParentEmail(@RequestParam String email) {
        log.info("GET /api/parents/verify - Verifying email: {}", email);

        boolean exists = parentService.verifyParentEmail(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", exists);
        response.put("message", exists
                ? "Parent found with email: " + email
                : "Parent not found with email: " + email);
        response.put("email", email);

        return exists
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateParent(
            @PathVariable Long id,
            @RequestBody ParentDTO parentDTO) {

        User user = currentUser();

        if (user.getRole() == User.Role.PARENT) {
            if (user.getParent() == null || !user.getParent().getId().equals(id)) {
                throw new AccessDeniedException("You can only update your own record");
            }
        } else if (!(user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.TEACHER)) {
            throw new AccessDeniedException("Access denied");
        }

        try {
            log.info("PUT /api/parents/{} - Updating parent", id);
            ParentDTO updatedParent = parentService.updateParent(id, parentDTO);
            return ResponseEntity.ok(updatedParent);
        } catch (Exception e) {
            log.error("Error updating parent: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteParent(@PathVariable Long id) {
        validateTeacherOrAdmin();
        try {
            log.info("DELETE /api/parents/{} - Deleting parent", id);
            parentService.deleteParent(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Parent deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting parent: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ParentDTO>> searchParents(@RequestParam String q) {
        validateTeacherOrAdmin();
        log.info("GET /api/parents/search - Searching parents with query: {}", q);
        List<ParentDTO> parents = parentService.searchParents(q);
        return ResponseEntity.ok(parents);
    }

    @PostMapping("/{parentId}/wards/{studentId}")
    public ResponseEntity<?> addWardToParent(
            @PathVariable Long parentId,
            @PathVariable Long studentId) {

        validateTeacherOrAdmin();

        try {
            log.info("POST /api/parents/{}/wards/{} - Adding ward to parent", parentId, studentId);
            ParentDTO updatedParent = parentService.addWardToParent(parentId, studentId);
            return ResponseEntity.ok(updatedParent);
        } catch (Exception e) {
            log.error("Error adding ward to parent: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{parentId}/wards/{studentId}")
    public ResponseEntity<?> removeWardFromParent(
            @PathVariable Long parentId,
            @PathVariable Long studentId) {

        validateTeacherOrAdmin();

        try {
            log.info("DELETE /api/parents/{}/wards/{} - Removing ward from parent", parentId, studentId);
            ParentDTO updatedParent = parentService.removeWardFromParent(parentId, studentId);
            return ResponseEntity.ok(updatedParent);
        } catch (Exception e) {
            log.error("Error removing ward from parent: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/no-wards")
    public ResponseEntity<List<ParentDTO>> getParentsWithNoWards() {
        validateTeacherOrAdmin();
        log.info("GET /api/parents/no-wards - Fetching parents with no wards");
        List<ParentDTO> parents = parentService.getParentsWithNoWards();
        return ResponseEntity.ok(parents);
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createMultipleParents(@RequestBody List<ParentDTO> parentDTOs) {
        validateTeacherOrAdmin();

        try {
            log.info("POST /api/parents/bulk - Creating {} parents", parentDTOs.size());
            List<ParentDTO> createdParents = parentService.createMultipleParents(parentDTOs);
            return new ResponseEntity<>(createdParents, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating multiple parents: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getParentStats() {
        validateTeacherOrAdmin();

        log.info("GET /api/parents/stats - Fetching parent statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalParents", parentService.getTotalParentCount());
        stats.put("parentsWithNoWards", parentService.getParentsWithNoWards().size());

        return ResponseEntity.ok(stats);
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsWithId() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/wards/{studentId}/results/term")
    public ResponseEntity<?> getWardTermResult(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam com.inkFront.schoolManagement.model.Result.Term term) {

        validateWardAccess(studentId);
        return ResponseEntity.ok(resultService.generateResultSheet(studentId, session, term));
    }

    @GetMapping("/me/wards/{studentId}/results/session")
    public ResponseEntity<?> getWardSessionResult(
            @PathVariable Long studentId,
            @RequestParam String session) {

        validateWardAccess(studentId);
        return ResponseEntity.ok(resultService.generateAnnualResultSheet(studentId, session));
    }

    @GetMapping("/me/wards/{studentId}/attendance")
    public ResponseEntity<?> getWardAttendance(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam com.inkFront.schoolManagement.model.Result.Term term) {

        validateWardAccess(studentId);
        return ResponseEntity.ok(attendanceService.getStudentTermSummary(studentId, session, term));
    }
}