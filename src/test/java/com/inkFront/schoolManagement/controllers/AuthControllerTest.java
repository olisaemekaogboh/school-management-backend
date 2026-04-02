package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.auth.ChangePasswordRequest;
import com.inkFront.schoolManagement.dto.auth.LoginRequest;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.dto.auth.RefreshTokenRequest;
import com.inkFront.schoolManagement.dto.auth.RegisterRequest;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private LoginResponse loginResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");
        userResponse.setRole("USER");

        loginResponse = new LoginResponse();
        loginResponse.setAccessToken("access-token-123");
        loginResponse.setRefreshToken("refresh-token-456");
        loginResponse.setUser(userResponse);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }
    @Test
    void login_ShouldReturnTokensAndSetCookies() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void register_ShouldCreateUserAndReturnTokens() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void refreshToken_WithCookie_ShouldReturnNewTokens() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", "valid-refresh-token");
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void refreshToken_WithRequestBody_ShouldReturnNewTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void refreshToken_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh-token"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).refreshToken(any());
    }

    @Test
    void logout_ShouldClearCookiesAndInvalidateToken() throws Exception {
        Cookie accessCookie = new Cookie("accessToken", "valid-token");
        doNothing().when(authService).logout("valid-token");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(authService, times(1)).logout("valid-token");
    }

    @Test
    void logout_WithAuthorizationHeader_ShouldInvalidateToken() throws Exception {
        doNothing().when(authService).logout("bearer-token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer bearer-token"))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout("bearer-token");
    }

    @Test
    void getCurrentUser_ShouldReturnUserDetails() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(securityUtils, times(1)).getCurrentUser();
    }

    @Test
    void changePassword_ShouldUpdatePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        doNothing().when(authService).changePassword(eq(testUser), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService, times(1)).changePassword(eq(testUser), any(ChangePasswordRequest.class));
    }

    @Test
    void forgotPassword_ShouldSendResetEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "user@example.com");

        doNothing().when(authService).forgotPassword("user@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent successfully"));

        verify(authService, times(1)).forgotPassword("user@example.com");
    }

    @Test
    void forgotPassword_WithEmptyEmail_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is required"));

        verify(authService, never()).forgotPassword(anyString());
    }

    @Test
    void resetPassword_ShouldResetPassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "reset-token-123");
        request.put("newPassword", "newPassword123");

        doNothing().when(authService).resetPassword("reset-token-123", "newPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));

        verify(authService, times(1)).resetPassword("reset-token-123", "newPassword123");
    }

    @Test
    void resetPassword_WithoutToken_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("newPassword", "newPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token is required"));

        verify(authService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_WithoutNewPassword_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "reset-token-123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New password is required"));

        verify(authService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void verifyEmail_ShouldVerifyEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "verification-token-123");

        doNothing().when(authService).verifyEmail("verification-token-123");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(authService, times(1)).verifyEmail("verification-token-123");
    }

    @Test
    void verifyEmail_WithoutToken_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token is required"));

        verify(authService, never()).verifyEmail(anyString());
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void register_WithExistingUsername_ShouldReturnConflict() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }
}