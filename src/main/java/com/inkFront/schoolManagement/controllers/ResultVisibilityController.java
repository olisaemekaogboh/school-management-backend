package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ResultVisibilityUpdateDTO;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.ResultVisibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/result-visibility")
@RequiredArgsConstructor
public class ResultVisibilityController {

    private static final Logger log = LoggerFactory.getLogger(ResultVisibilityController.class);

    private final ResultVisibilityService resultVisibilityService;
    private final AccessControlService accessControlService;
    private final SecurityUtils securityUtils;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    private String currentAdminDisplayName() {
        User user = currentUser();
        if (user == null) {
            return "System Admin";
        }

        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).replaceAll("\\s+", " ").trim();

        return fullName.isBlank() ? user.getUsername() : fullName;
    }

    private ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        log.error(message, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", message,
                        "error", e.getMessage()
                ));
    }

    @PatchMapping("/term/student/{studentId}")
    public ResponseEntity<?> updateTermVisibilityForStudent(
            @PathVariable Long studentId,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @Valid @RequestBody ResultVisibilityUpdateDTO request
    ) {
        try {
            accessControlService.requireAdmin(currentUser());

            return ResponseEntity.ok(
                    resultVisibilityService.updateTermVisibilityForStudent(
                            studentId,
                            session,
                            term,
                            request,
                            currentAdminDisplayName()
                    )
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to update term result visibility", e);
        }
    }

    @PatchMapping("/term/class/{classId}")
    public ResponseEntity<?> updateTermVisibilityForClass(
            @PathVariable Long classId,
            @RequestParam String session,
            @RequestParam Result.Term term,
            @Valid @RequestBody ResultVisibilityUpdateDTO request
    ) {
        try {
            accessControlService.requireAdmin(currentUser());

            return ResponseEntity.ok(
                    resultVisibilityService.updateTermVisibilityForClass(
                            classId,
                            session,
                            term,
                            request,
                            currentAdminDisplayName()
                    )
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to update class term result visibility", e);
        }
    }

    @PatchMapping("/session/student/{studentId}")
    public ResponseEntity<?> updateSessionVisibilityForStudent(
            @PathVariable Long studentId,
            @RequestParam String session,
            @Valid @RequestBody ResultVisibilityUpdateDTO request
    ) {
        try {
            accessControlService.requireAdmin(currentUser());

            return ResponseEntity.ok(
                    resultVisibilityService.updateSessionVisibilityForStudent(
                            studentId,
                            session,
                            request,
                            currentAdminDisplayName()
                    )
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to update session result visibility", e);
        }
    }

    @PatchMapping("/session/class/{classId}")
    public ResponseEntity<?> updateSessionVisibilityForClass(
            @PathVariable Long classId,
            @RequestParam String session,
            @Valid @RequestBody ResultVisibilityUpdateDTO request
    ) {
        try {
            accessControlService.requireAdmin(currentUser());

            return ResponseEntity.ok(
                    resultVisibilityService.updateSessionVisibilityForClass(
                            classId,
                            session,
                            request,
                            currentAdminDisplayName()
                    )
            );
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Unable to update class session result visibility", e);
        }
    }
}