// src/main/java/com/inkFront/schoolManagement/dto/auth/LoginResponse.java
package com.inkFront.schoolManagement.dto.auth;

import com.inkFront.schoolManagement.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponse user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String phoneNumber;
        private String role;
        private String profilePictureUrl;
        private boolean active;
        private boolean emailVerified;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;

        public static UserResponse fromUser(User user) {
            if (user == null) return null;

            return UserResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .role(user.getRole() != null ? user.getRole().name() : null)
                    .profilePictureUrl(user.getProfilePictureUrl())
                    .active(user.isActive())
                    .emailVerified(user.isEmailVerified())
                    .lastLogin(user.getLastLogin())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }
}