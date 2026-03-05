// src/main/java/com/inkFront/schoolManagement/controllers/PublicController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.ParentDTO;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.ParentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final ParentService parentService;
    private final StudentRepository studentRepository;

    @GetMapping("/verify-parent/email")
    public ResponseEntity<?> verifyParentByEmail(@RequestParam String email) {
        log.info("Verifying parent with email: {}", email);

        Optional<ParentDTO> parentOpt = parentService.getParentByEmail(email);

        Map<String, Object> response = new HashMap<>();

        if (parentOpt.isPresent()) {
            ParentDTO parent = parentOpt.get();
            response.put("success", true);
            response.put("message", "Parent found with email: " + email);
            response.put("email", email);
            response.put("parent", parent);
            return ResponseEntity.ok(response);
        } else {
            log.warn("Parent not found with email: {}", email);
            response.put("success", false);
            response.put("message", "Parent not found with email: " + email);
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/verify-parent/phone")
    public ResponseEntity<?> verifyParentByPhone(@RequestParam String phone) {
        log.info("Verifying parent with phone: {}", phone);

        // You'll need to add this method to your service
        // Optional<ParentDTO> parentOpt = parentService.getParentByPhone(phone);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Phone verification not implemented yet");
        response.put("phone", phone);

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    @GetMapping("/verify-student")
    public ResponseEntity<?> verifyStudent(@RequestParam String admissionNumber) {

        Student s = studentRepository.findByAdmissionNumber(admissionNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // return only safe fields (public verification)
        return ResponseEntity.ok(Map.of(
                "id", s.getId(),
                "admissionNumber", s.getAdmissionNumber(),
                "firstName", s.getFirstName(),
                "lastName", s.getLastName(),

                // ✅ extra safe fields for UI
                "className", s.getStudentClass(),
                "classArm", s.getClassArm(),
                "status", s.getStatus() != null ? s.getStatus().name() : null
        ));
    }
}