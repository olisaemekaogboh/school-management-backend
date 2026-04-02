package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.UserDTO;
import com.inkFront.schoolManagement.dto.auth.LoginRequest;
import com.inkFront.schoolManagement.dto.auth.LoginResponse;
import com.inkFront.schoolManagement.dto.auth.RegisterRequest;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.security.SecurityUtils;
import com.inkFront.schoolManagement.service.AuthService;
import com.inkFront.schoolManagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private UserDTO userDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");

        loginResponse = new LoginResponse();
        loginResponse.setAccessToken("access-token-123");
        loginResponse.setUser(userResponse);

        userDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.PARENT)
                .isActive(true)
                .build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(User.Role.PARENT);
    }

    @Test
    void register_ShouldReturnLoginResponse() throws Exception {
        when(authService.register(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));
    }

    @Test
    void login_ShouldReturnLoginResponse() throws Exception {
        when(authService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));
    }

    @Test
    void getCurrentUser_ShouldReturnUserDTO() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateCurrentUser_ShouldReturnUpdatedUser() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userService.updateUser(eq(1L), any())).thenReturn(userDTO);

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void deleteCurrentUser_ShouldReturnNoContent() throws Exception {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        doNothing().when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllUsers_ShouldReturnList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllUsersPaginated_ShouldReturnPage() {
        Page<UserDTO> userPage = new PageImpl<>(
                List.of(userDTO),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id").ascending()),
                1
        );

        when(userService.getAllUsersPaginated(any()))
                .thenReturn(userPage);

        ResponseEntity<Page<UserDTO>> response =
                userController.getAllUsersPaginated(0, 10, "id", "asc");

        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.http.HttpStatus.OK,
                response.getStatusCode()
        );
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
        org.junit.jupiter.api.Assertions.assertEquals(1, response.getBody().getContent().size());

        verify(userService, times(1)).getAllUsersPaginated(any());
    }
    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        when(userService.createUser(any())).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        when(userService.updateUser(eq(1L), any())).thenReturn(userDTO);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void toggleUserStatus_ShouldReturnUpdatedUser() throws Exception {
        userDTO.setActive(false);

        when(userService.toggleUserStatus(eq(1L), eq(false))).thenReturn(userDTO);

        mockMvc.perform(patch("/api/users/1/toggle-status")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void searchUsers_ShouldReturnList() throws Exception {
        when(userService.searchUsers("test")).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/api/users/search")
                        .param("term", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUsersByRole_ShouldReturnList() throws Exception {
        when(userService.getUsersByRole(User.Role.PARENT)).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/api/users/role/PARENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUserStatistics_ShouldReturnStatistics() throws Exception {
        Map<String, Object> stats = Map.of("totalUsers", 10);

        when(userService.getUserStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10));
    }
}