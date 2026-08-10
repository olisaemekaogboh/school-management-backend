package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.GeneratedResultCheckerPinDTO;
import com.inkFront.schoolManagement.dto.ResultCheckerPinCreateDTO;
import com.inkFront.schoolManagement.dto.ResultPinVerificationRequestDTO;
import com.inkFront.schoolManagement.security.AccessControlService;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.ResultCheckerPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/result-checker-pins")
@RequiredArgsConstructor
public class ResultCheckerPinController {

    private final ResultCheckerPinService resultCheckerPinService;
    private final AccessControlService accessControlService;
    private final SecurityUtils securityUtils;

    private ResponseEntity<Map<String, Object>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }

    private String currentUserDisplayName() {
        var user = securityUtils.getCurrentUser();

        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";

        String fullName = (firstName + " " + lastName)
                .replaceAll("\\s+", " ")
                .trim();

        return fullName.isBlank() ? user.getUsername() : fullName;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePins(@Valid @RequestBody ResultCheckerPinCreateDTO request) {
        try {
            accessControlService.requireAdmin(securityUtils.getCurrentUser());

            List<GeneratedResultCheckerPinDTO> generated = resultCheckerPinService.generatePins(
                    request,
                    currentUserDisplayName()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(generated);
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllPins() {
        try {
            accessControlService.requireAdmin(securityUtils.getCurrentUser());
            return ResponseEntity.ok(resultCheckerPinService.getAllPins());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PatchMapping("/{pinId}/deactivate")
    public ResponseEntity<?> deactivatePin(@PathVariable Long pinId) {
        try {
            accessControlService.requireAdmin(securityUtils.getCurrentUser());
            return ResponseEntity.ok(resultCheckerPinService.deactivatePin(pinId));
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/verify/term")
    public ResponseEntity<?> verifyTermPin(@Valid @RequestBody ResultPinVerificationRequestDTO request) {
        try {
            return ResponseEntity.ok(
                    resultCheckerPinService.verifyTermPin(
                            request.getStudentId(),
                            request.getSession(),
                            request.getTerm(),
                            request.getPin()
                    )
            );
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }

    @PostMapping("/verify/session")
    public ResponseEntity<?> verifySessionPin(@Valid @RequestBody ResultPinVerificationRequestDTO request) {
        try {
            return ResponseEntity.ok(
                    resultCheckerPinService.verifySessionPin(
                            request.getStudentId(),
                            request.getSession(),
                            request.getPin()
                    )
            );
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccessDeniedException e) {
            return forbidden(e.getMessage());
        }
    }
}