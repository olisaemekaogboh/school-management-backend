// src/main/java/com/inkFront/schoolManagement/dto/auth/ChangePasswordRequest.java
package com.inkFront.schoolManagement.dto.auth;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}