// src/main/java/com/inkFront/schoolManagement/dto/UserDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phoneNumber;

    private User.Role role;

    private String profilePictureUrl;

    private boolean isActive;

    private boolean isEmailVerified;

    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Linked entities
    private Long teacherId;
    private Long studentId;
    private Long parentId;

    public static UserDTO fromUser(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isActive(user.isActive())
                .isEmailVerified(user.isEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .teacherId(user.getTeacher() != null ? user.getTeacher().getId() : null)
                .studentId(user.getStudent() != null ? user.getStudent().getId() : null)
                .parentId(user.getParent() != null ? user.getParent().getId() : null)
                .build();
    }
}