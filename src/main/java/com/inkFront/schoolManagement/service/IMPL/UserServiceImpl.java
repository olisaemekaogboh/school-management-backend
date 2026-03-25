// src/main/java/com/inkFront/schoolManagement/service/IMPL/UserServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.UserDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        log.info("Creating new user via admin: {}", userDTO.getUsername());

        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new BusinessException("Password is required");
        }

        User user = User.builder()
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .phoneNumber(userDTO.getPhoneNumber())
                .role(userDTO.getRole() != null ? userDTO.getRole() : User.Role.PARENT)
                .profilePictureUrl(userDTO.getProfilePictureUrl())
                .isActive(userDTO.isActive())
                .isEmailVerified(userDTO.isEmailVerified())
                .lastLogin(userDTO.getLastLogin())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        return UserDTO.fromUser(savedUser);
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("Updating user with id: {}", id);

        User existingUser = getUserEntityById(id);

        if (userDTO.getUsername() != null && !userDTO.getUsername().equals(existingUser.getUsername())) {
            userRepository.findByUsername(userDTO.getUsername()).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new BusinessException("Username already exists");
                }
            });
        }

        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(existingUser.getEmail())) {
            userRepository.findByEmail(userDTO.getEmail()).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new BusinessException("Email already exists");
                }
            });
        }

        // Update only editable fields
        if (userDTO.getFirstName() != null) {
            existingUser.setFirstName(userDTO.getFirstName().trim());
        }

        if (userDTO.getLastName() != null) {
            existingUser.setLastName(userDTO.getLastName().trim());
        }

        if (userDTO.getUsername() != null) {
            existingUser.setUsername(userDTO.getUsername().trim());
        }

        if (userDTO.getEmail() != null) {
            existingUser.setEmail(userDTO.getEmail().trim());
        }

        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setProfilePictureUrl(userDTO.getProfilePictureUrl());

        // Preserve protected fields unless explicitly handled in admin workflows
        if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        if (userDTO.getRole() != null) {
            existingUser.setRole(userDTO.getRole());
        }

        existingUser.setActive(userDTO.isActive());
        existingUser.setEmailVerified(userDTO.isEmailVerified());
        existingUser.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(existingUser);
        log.info("User updated successfully with id: {}", updatedUser.getId());

        return UserDTO.fromUser(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return UserDTO.fromUser(getUserEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserDTO.fromUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserDTO.fromUser(user);
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = getUserEntityById(id);
        userRepository.delete(user);
        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsersPaginated(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        List<UserDTO> dtos = userPage.getContent().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, userPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String term) {
        String search = term == null ? "" : term.toLowerCase();

        return userRepository.findAll().stream()
                .filter(u ->
                        (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(search)) ||
                                (u.getLastName() != null && u.getLastName().toLowerCase().contains(search)) ||
                                (u.getUsername() != null && u.getUsername().toLowerCase().contains(search)) ||
                                (u.getEmail() != null && u.getEmail().toLowerCase().contains(search))
                )
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(User.Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO toggleUserStatus(Long id, boolean active) {
        User user = getUserEntityById(id);
        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("User {} status toggled to: {}", id, active);
        return UserDTO.fromUser(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(User::isActive).count();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", totalUsers - activeUsers);

        Map<User.Role, Long> roleCount = users.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        stats.put("roleCount", roleCount);

        return stats;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}