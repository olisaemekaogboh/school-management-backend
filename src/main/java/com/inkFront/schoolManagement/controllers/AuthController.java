package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.auth.ChangePasswordRequest;
import com.inkFront.schoolManagement.dto.auth.LoginRequest;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.dto.auth.RefreshTokenRequest;
import com.inkFront.schoolManagement.dto.auth.RegisterRequest;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = {"https://localhost:3000", "https://127.0.0.1:3000"},
        allowCredentials = "true"
)
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_COOKIE_NAME = "accessToken";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return buildAuthResponse(response);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return buildAuthResponse(response, HttpStatus.CREATED);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String refreshToken = refreshTokenCookie;

        if ((refreshToken == null || refreshToken.isBlank()) && request != null) {
            refreshToken = request.getRefreshToken();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken(refreshToken);

        LoginResponse response = authService.refreshToken(refreshTokenRequest);

        return buildAuthResponse(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extractCookieValue(request, ACCESS_COOKIE_NAME);

        if (token == null || token.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse.UserResponse> getCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(LoginResponse.UserResponse.fromUser(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = securityUtils.getCurrentUser();
        authService.changePassword(user, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        authService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }

        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    private ResponseEntity<LoginResponse> buildAuthResponse(LoginResponse response) {
        return buildAuthResponse(response, HttpStatus.OK);
    }

    private ResponseEntity<LoginResponse> buildAuthResponse(LoginResponse response, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, buildAccessCookie(response.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(response.getRefreshToken()).toString())
                .body(response);
    }

    private ResponseCookie buildAccessCookie(String token) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    private ResponseCookie clearAccessCookie() {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}