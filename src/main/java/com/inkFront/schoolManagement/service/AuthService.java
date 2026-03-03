// src/main/java/com/inkFront/schoolManagement/service/AuthService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.auth.*;
import com.inkFront.schoolManagement.model.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    LoginResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
    void changePassword(User user, ChangePasswordRequest request);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void verifyEmail(String token);
}