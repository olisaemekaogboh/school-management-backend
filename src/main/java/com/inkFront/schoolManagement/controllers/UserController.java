package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.UserDTO;
import com.inkFront.schoolManagement.dto.auth.LoginRequest;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.dto.auth.RegisterRequest;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AuthService;
import com.inkFront.schoolManagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    private User currentUser() {
        return securityUtils.getCurrentUser();
    }

    // ========== PUBLIC ENDPOINTS ==========

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }
    @PostMapping("/upload-signature")
    public ResponseEntity<?> uploadSignature(
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        try {
            String username = principal.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String fileName = "signature_" + user.getId() + ".png";

            Path uploadPath = Paths.get("uploads/signatures");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/signatures/" + fileName;

            user.setSignatureUrl(url);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Signature uploaded",
                    "signatureUrl", url
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Upload failed"
            ));
        }
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ========== PROTECTED ENDPOINTS ==========

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        return ResponseEntity.ok(UserDTO.fromUser(currentUser()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(currentUser().getId(), userDTO));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        userService.deleteUser(currentUser().getId());
        return ResponseEntity.noContent().build();
    }

    // ========== ADMIN ONLY ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<UserDTO>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getAllUsersPaginated(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        return new ResponseEntity<>(userService.createUser(userDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<UserDTO> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(userService.toggleUserStatus(id, active));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String term) {
        return ResponseEntity.ok(userService.searchUsers(term));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable User.Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Object> getUserStatistics() {
        return ResponseEntity.ok(userService.getUserStatistics());
    }
}