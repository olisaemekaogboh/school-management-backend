// src/main/java/com/inkFront/schoolManagement/dto/auth/LoginRequest.java
package com.inkFront.schoolManagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}