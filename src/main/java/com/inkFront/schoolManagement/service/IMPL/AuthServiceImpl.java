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
import com.inkFront.schoolManagement.service.AuthService;
import com.inkFront.schoolManagement.service.EmailService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final long ACCESS_TOKEN_EXPIRES_IN_MS = 86_400_000L;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;

    private final Map<String, String> passwordResetTokens = new HashMap<>();
    private final Map<String, String> emailVerificationTokens = new HashMap<>();

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsernameOrEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole() != null ? request.getRole() : User.Role.PARENT)
                .isActive(true)
                .isEmailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (request.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
            user.setTeacher(teacher);
        }

        if (request.getStudentId() != null) {
            Student student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            user.setStudent(student);
        }

        if (request.getParentId() != null) {
            Parent parent = parentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
            user.setParent(parent);
        }

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getUsername());
        return buildLoginResponse(savedUser);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BusinessException("Refresh token is required");
        }

        String refreshToken = request.getRefreshToken().trim();

        try {
            String usernameOrEmail = jwtService.extractUsername(refreshToken);

            if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
                throw new BusinessException("Invalid refresh token");
            }

            User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new BusinessException("Refresh token expired");
            }

            return buildLoginResponse(user);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Invalid refresh token");
        }
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            String usernameOrEmail = jwtService.extractUsername(token);
            log.info("Logout request received for user: {}", usernameOrEmail);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Logout called with invalid token: {}", ex.getMessage());
        }
    }

    @Override
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String resetToken = UUID.randomUUID().toString();
        passwordResetTokens.put(resetToken, email);

        emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String email = passwordResetTokens.get(token);
        if (email == null) {
            throw new BusinessException("Invalid or expired reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        passwordResetTokens.remove(token);

        log.info("Password reset successful for: {}", email);
    }

    @Override
    public void verifyEmail(String token) {
        String email = emailVerificationTokens.get(token);
        if (email == null) {
            throw new BusinessException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailVerificationTokens.remove(token);
        log.info("Email verified for: {}", email);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRES_IN_MS)
                .user(LoginResponse.UserResponse.fromUser(user))
                .build();
    }
}
