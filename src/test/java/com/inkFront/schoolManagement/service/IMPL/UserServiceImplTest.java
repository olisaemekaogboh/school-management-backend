// src/test/java/com/inkFront/schoolManagement/service/IMPL/UserServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.UserDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.SupportMessageRepository;
import com.inkFront.schoolManagement.repository.SupportTicketRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private SupportMessageRepository supportMessageRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(User.Role.PARENT);
        testUser.setActive(true);
        testUser.setEmailVerified(true);

        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.PARENT)
                .isActive(true)
                .isEmailVerified(true)
                .password("password123")
                .build();
    }

    @Test
    void createUser_ShouldCreateUser() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.createUser(testUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(testUserDTO));
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(testUserDTO));
    }

    @Test
    void createUser_WithoutPassword_ShouldThrowException() {
        testUserDTO.setPassword(null);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.createUser(testUserDTO));
    }

    @Test
    void updateUser_ShouldUpdateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.updateUser(1L, testUserDTO);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WithNonExistentId_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(999L, testUserDTO));
    }

    @Test
    void updateUser_WithUsernameConflict_ShouldThrowException() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        UserDTO conflictDto = UserDTO.builder()
                .id(1L)
                .username("otheruser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.PARENT)
                .isActive(true)
                .isEmailVerified(true)
                .password("password123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));

        assertThrows(BusinessException.class, () -> userService.updateUser(1L, conflictDto));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserByUsername_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void deleteUser_ShouldDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findByCreatedBy_Id(1L)).thenReturn(Arrays.asList());

        userService.deleteUser(1L);

        verify(userRepository).delete(testUser);
    }
    @Test
    void getAllUsers_ShouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllUsersPaginated_ShouldReturnPage() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserDTO> result = userService.getAllUsersPaginated(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchUsers_ShouldReturnMatches() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.searchUsers("john");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getUsersByRole_ShouldReturnMatches() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.getUsersByRole(User.Role.PARENT);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void toggleUserStatus_ShouldUpdateStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.toggleUserStatus(1L, false);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserStatistics_ShouldReturnStats() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        Map<String, Object> stats = userService.getUserStatistics();

        assertNotNull(stats);
        assertEquals(1L, stats.get("totalUsers"));
    }
}