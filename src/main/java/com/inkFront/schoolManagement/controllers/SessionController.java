// src/main/java/com/inkFront/schoolManagement/controllers/SessionController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.SessionRequestDTO;
import com.inkFront.schoolManagement.dto.SessionResponseDTO;
import com.inkFront.schoolManagement.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(
            @Valid @RequestBody SessionRequestDTO request) {
        return new ResponseEntity<>(sessionService.createSession(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponseDTO> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequestDTO request) {
        return ResponseEntity.ok(sessionService.updateSession(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SessionResponseDTO>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveSession() {
        SessionResponseDTO active = sessionService.getActiveSession();
        if (active == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(active);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<SessionResponseDTO> activateSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.activateSession(id));
    }
}