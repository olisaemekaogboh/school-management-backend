// src/main/java/com/inkFront/schoolManagement/service/UserService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.UserDTO;
import com.inkFront.schoolManagement.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserDTO createUser(UserDTO userDTO);
    UserDTO updateUser(Long id, UserDTO userDTO);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    UserDTO getUserByEmail(String email);
    void deleteUser(Long id);
    List<UserDTO> getAllUsers();
    Page<UserDTO> getAllUsersPaginated(Pageable pageable);
    List<UserDTO> searchUsers(String term);
    List<UserDTO> getUsersByRole(User.Role role);
    UserDTO toggleUserStatus(Long id, boolean active);
    Map<String, Object> getUserStatistics();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}