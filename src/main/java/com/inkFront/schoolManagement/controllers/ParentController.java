// src/main/java/com/inkFront/schoolManagement/controllers/ParentController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.service.ParentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Create parent
    @PostMapping
    public ResponseEntity<?> createParent(@RequestBody ParentDTO parentDTO) {
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

    // Get all parents
    @GetMapping
    public ResponseEntity<List<ParentDTO>> getAllParents() {
        log.info("GET /api/parents - Fetching all parents");
        List<ParentDTO> parents = parentService.getAllParents();
        return ResponseEntity.ok(parents);
    }

    // Get parents with pagination
    @GetMapping("/paginated")
    public ResponseEntity<Page<ParentDTO>> getParentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("GET /api/parents/paginated - Page: {}, Size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ParentDTO> parents = parentService.getAllParentsPaginated(pageable);
        return ResponseEntity.ok(parents);
    }

    // Get parent by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getParentById(@PathVariable Long id) {
        log.info("GET /api/parents/{} - Fetching parent by ID", id);

        return parentService.getParentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get parent by email
    @GetMapping("/by-email")
    public ResponseEntity<?> getParentByEmail(@RequestParam String email) {
        log.info("GET /api/parents/by-email - Fetching parent with email: {}", email);

        return parentService.getParentByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Verify parent email (public endpoint)
    @GetMapping("/verify")
    public ResponseEntity<?> verifyParentEmail(@RequestParam String email) {
        log.info("GET /api/parents/verify - Verifying email: {}", email);

        boolean exists = parentService.verifyParentEmail(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", exists);
        response.put("message", exists ?
                "Parent found with email: " + email :
                "Parent not found with email: " + email);
        response.put("email", email);

        if (exists) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Update parent
    @PutMapping("/{id}")
    public ResponseEntity<?> updateParent(@PathVariable Long id, @RequestBody ParentDTO parentDTO) {
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

    // Delete parent
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteParent(@PathVariable Long id) {
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

    // Search parents
    @GetMapping("/search")
    public ResponseEntity<List<ParentDTO>> searchParents(@RequestParam String q) {
        log.info("GET /api/parents/search - Searching parents with query: {}", q);
        List<ParentDTO> parents = parentService.searchParents(q);
        return ResponseEntity.ok(parents);
    }

    // Add ward to parent
    @PostMapping("/{parentId}/wards/{studentId}")
    public ResponseEntity<?> addWardToParent(
            @PathVariable Long parentId,
            @PathVariable Long studentId) {
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

    // Remove ward from parent
    @DeleteMapping("/{parentId}/wards/{studentId}")
    public ResponseEntity<?> removeWardFromParent(
            @PathVariable Long parentId,
            @PathVariable Long studentId) {
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

    // Get parents with no wards
    @GetMapping("/no-wards")
    public ResponseEntity<List<ParentDTO>> getParentsWithNoWards() {
        log.info("GET /api/parents/no-wards - Fetching parents with no wards");
        List<ParentDTO> parents = parentService.getParentsWithNoWards();
        return ResponseEntity.ok(parents);
    }

    // Bulk create parents
    @PostMapping("/bulk")
    public ResponseEntity<?> createMultipleParents(@RequestBody List<ParentDTO> parentDTOs) {
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

    // Get parent statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getParentStats() {
        log.info("GET /api/parents/stats - Fetching parent statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalParents", parentService.getTotalParentCount());
        stats.put("parentsWithNoWards", parentService.getParentsWithNoWards().size());

        return ResponseEntity.ok(stats);
    }
    // Handle OPTIONS requests for CORS preflight
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsWithId() {
        return ResponseEntity.ok().build();
    }
}