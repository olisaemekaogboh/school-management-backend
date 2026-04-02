package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.auth.ChangePasswordRequest;
import com.inkFront.schoolManagement.dto.auth.LoginRequest;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.dto.auth.RefreshTokenRequest;
import com.inkFront.schoolManagement.dto.auth.RegisterRequest;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ParentRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.security.JwtService;
import com.inkFront.schoolManagement.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ParentRepository parentRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(User.Role.PARENT);
        testUser.setActive(true);
        testUser.setEmailVerified(true);

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setRole(User.Role.PARENT);

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("oldPassword");
        changePasswordRequest.setNewPassword("newPassword123");
        changePasswordRequest.setConfirmPassword("newPassword123");

        ReflectionTestUtils.setField(authService, "refreshTokens", new HashMap<String, String>());
        ReflectionTestUtils.setField(authService, "passwordResetTokens", new HashMap<String, String>());
        ReflectionTestUtils.setField(authService, "emailVerificationTokens", new HashMap<String, String>());
        ReflectionTestUtils.setField(authService, "blacklistedTokens", new HashMap<String, Boolean>());
    }

    @Test
    void login_ShouldReturnLoginResponse() {
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh_token");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals("testuser", response.getUser().getUsername());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void login_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void register_ShouldCreateUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        LoginResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_WithExistingUsername_ShouldThrowException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
    }

    @Test
    void register_WithTeacherId_ShouldLinkTeacher() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        registerRequest.setTeacherId(1L);
        registerRequest.setRole(User.Role.TEACHER);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        LoginResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(teacherRepository, times(1)).findById(1L);
    }

    @Test
    void register_WithStudentId_ShouldLinkStudent() {
        Student student = new Student();
        student.setId(1L);
        registerRequest.setStudentId(1L);
        registerRequest.setRole(User.Role.STUDENT);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        LoginResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(studentRepository, times(1)).findById(1L);
    }

    @Test
    void register_WithParentId_ShouldLinkParent() {
        Parent parent = new Parent();
        parent.setId(1L);
        registerRequest.setParentId(1L);
        registerRequest.setRole(User.Role.PARENT);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        LoginResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(parentRepository, times(1)).findById(1L);
    }

    @Test
    void refreshToken_ShouldReturnNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        @SuppressWarnings("unchecked")
        Map<String, String> refreshTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "refreshTokens");
        refreshTokens.put("valid_refresh_token", "testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid("valid_refresh_token", testUser)).thenReturn(true);
        when(jwtService.generateToken(testUser)).thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new_refresh_token");

        LoginResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());

        assertFalse(refreshTokens.containsKey("valid_refresh_token"));
        assertEquals("testuser", refreshTokens.get("new_refresh_token"));
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldThrowException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_token");

        assertThrows(BusinessException.class, () -> authService.refreshToken(request));
    }

    @Test
    void logout_ShouldBlacklistToken() {
        authService.logout("token_to_blacklist");

        @SuppressWarnings("unchecked")
        Map<String, Boolean> blacklistedTokens =
                (Map<String, Boolean>) ReflectionTestUtils.getField(authService, "blacklistedTokens");

        assertTrue(Boolean.TRUE.equals(blacklistedTokens.get("token_to_blacklist")));
    }

    @Test
    void changePassword_ShouldUpdatePassword() {
        when(passwordEncoder.matches("oldPassword", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new_encoded_password");

        authService.changePassword(testUser, changePasswordRequest);

        verify(userRepository, times(1)).save(testUser);
        assertEquals("new_encoded_password", testUser.getPassword());
    }

    @Test
    void changePassword_WithMismatchedPasswords_ShouldThrowException() {
        changePasswordRequest.setConfirmPassword("different");

        assertThrows(BusinessException.class, () -> authService.changePassword(testUser, changePasswordRequest));
    }

    @Test
    void changePassword_WithIncorrectCurrentPassword_ShouldThrowException() {
        when(passwordEncoder.matches("oldPassword", "encoded_password")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.changePassword(testUser, changePasswordRequest));
    }

    @Test
    void forgotPassword_ShouldSendResetEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        authService.forgotPassword("test@example.com");

        verify(emailService, times(1)).sendPasswordResetEmail(eq("test@example.com"), anyString());

        @SuppressWarnings("unchecked")
        Map<String, String> passwordResetTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "passwordResetTokens");

        assertFalse(passwordResetTokens.isEmpty());
        assertTrue(passwordResetTokens.containsValue("test@example.com"));
    }

    @Test
    void forgotPassword_WithNonExistentEmail_ShouldThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword("nonexistent@example.com"));
    }

    @Test
    void resetPassword_ShouldUpdatePassword() {
        String token = "reset_token";
        String newPassword = "newPassword123";

        @SuppressWarnings("unchecked")
        Map<String, String> passwordResetTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "passwordResetTokens");
        passwordResetTokens.put(token, "test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");

        authService.resetPassword(token, newPassword);

        verify(userRepository, times(1)).save(testUser);
        assertEquals("encoded_new_password", testUser.getPassword());
        assertFalse(passwordResetTokens.containsKey(token));
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldThrowException() {
        assertThrows(BusinessException.class, () ->
                authService.resetPassword("invalid_token", "newPassword"));
    }

    @Test
    void verifyEmail_ShouldVerifyUser() {
        String token = "verify_token";
        testUser.setEmailVerified(false);

        @SuppressWarnings("unchecked")
        Map<String, String> emailVerificationTokens =
                (Map<String, String>) ReflectionTestUtils.getField(authService, "emailVerificationTokens");
        emailVerificationTokens.put(token, "test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.verifyEmail(token);

        assertTrue(testUser.isEmailVerified());
        verify(userRepository, times(1)).save(testUser);
        assertFalse(emailVerificationTokens.containsKey(token));
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldThrowException() {
        assertThrows(BusinessException.class, () -> authService.verifyEmail("invalid_token"));
    }
}