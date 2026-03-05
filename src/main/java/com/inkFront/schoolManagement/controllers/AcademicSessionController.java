// src/main/java/com/inkFront/schoolManagement/controllers/AcademicSessionController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.AcademicSessionRequest;
import com.inkFront.schoolManagement.dto.AcademicSessionResponse;
import com.inkFront.schoolManagement.service.AcademicSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class AcademicSessionController {

    private final AcademicSessionService service;

    public AcademicSessionController(AcademicSessionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AcademicSessionResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // ✅ Do NOT throw 404 when there is no session yet
    @GetMapping("/active")
    public ResponseEntity<AcademicSessionResponse> getActive() {
        // Option 1: 200 OK with null
        return ResponseEntity.ok(service.getActiveOrNull());

        // Option 2: 204 No Content (if you prefer)
        // var active = service.getActiveOptional();
        // return active.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<AcademicSessionResponse> create(@RequestBody AcademicSessionRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcademicSessionResponse> update(
            @PathVariable Long id,
            @RequestBody AcademicSessionRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AcademicSessionResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}