// src/test/java/com/inkFront/schoolManagement/service/IMPL/SupportRealtimeServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.support.SupportWsMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportWsMessageResponse;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.SupportMessage;
import com.inkFront.schoolManagement.model.SupportTicket;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportRealtimeServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private SupportMessageRepository supportMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SupportRealtimeServiceImpl supportRealtimeService;

    private User testUser;
    private User testAdmin;
    private SupportTicket testTicket;
    private SupportWsMessageRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(User.Role.PARENT);

        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@school.com");
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        testAdmin.setRole(User.Role.ADMIN);

        testTicket = new SupportTicket();
        testTicket.setId(1L);
        testTicket.setTicketNumber("SUP-2024-00001");
        testTicket.setSubject("Login Issue");
        testTicket.setStatus(SupportTicket.TicketStatus.OPEN);
        testTicket.setCreatedBy(testUser);

        testRequest = new SupportWsMessageRequest();
        testRequest.setTicketId(1L);
        testRequest.setMessage("I need help with login");
    }

    @Test
    void handleRealtimeMessage_AsUser_ShouldSendMessage() {
        SupportMessage savedMessage = new SupportMessage();
        savedMessage.setId(1L);
        savedMessage.setMessage("I need help with login");

        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(savedMessage);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

        SupportWsMessageResponse result =
                supportRealtimeService.handleRealtimeMessage("john_doe", testRequest);

        assertNotNull(result);
        assertEquals(1L, result.getTicketId());
        assertEquals("I need help with login", result.getMessage());
        assertEquals("John Doe", result.getSenderName());
        assertEquals("PARENT", result.getSenderRole());
        assertFalse(result.isFromAdmin());

        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleRealtimeMessage_AsAdmin_ShouldSendMessage() {
        SupportMessage savedMessage = new SupportMessage();
        savedMessage.setId(2L);
        savedMessage.setMessage("I need help with login");

        testTicket.setAssignedAdmin(null);

        when(userRepository.findByUsernameOrEmail("admin")).thenReturn(Optional.of(testAdmin));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(savedMessage);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

        SupportWsMessageResponse result =
                supportRealtimeService.handleRealtimeMessage("admin", testRequest);

        assertNotNull(result);
        assertTrue(result.isFromAdmin());
        assertEquals("Admin User", result.getSenderName());

        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleRealtimeMessage_WithClosedTicket_AsUser_ShouldThrowException() {
        testTicket.setStatus(SupportTicket.TicketStatus.CLOSED);

        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        assertThrows(BusinessException.class,
                () -> supportRealtimeService.handleRealtimeMessage("john_doe", testRequest));
    }

    @Test
    void handleRealtimeMessage_WithClosedTicket_AsAdmin_ShouldAllow() {
        SupportMessage savedMessage = new SupportMessage();
        savedMessage.setId(3L);
        savedMessage.setMessage("I need help with login");

        testTicket.setStatus(SupportTicket.TicketStatus.CLOSED);
        testTicket.setAssignedAdmin(null);

        when(userRepository.findByUsernameOrEmail("admin")).thenReturn(Optional.of(testAdmin));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(savedMessage);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

        SupportWsMessageResponse result =
                supportRealtimeService.handleRealtimeMessage("admin", testRequest);

        assertNotNull(result);
        verify(supportTicketRepository).save(any(SupportTicket.class));
    }

    @Test
    void handleRealtimeMessage_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findByUsernameOrEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> supportRealtimeService.handleRealtimeMessage("unknown", testRequest));
    }

    // SupportRealtimeServiceImplTest.java
    @Test
    void handleRealtimeMessage_WithNonExistentTicket_ShouldThrowException() {
        testRequest.setTicketId(1L);

        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> supportRealtimeService.handleRealtimeMessage("john_doe", testRequest));
    }
    @Test
    void handleRealtimeMessage_WithUnauthorizedUser_ShouldThrowException() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("other_user");
        otherUser.setEmail("other@example.com");
        otherUser.setRole(User.Role.PARENT);

        when(userRepository.findByUsernameOrEmail("other_user")).thenReturn(Optional.of(otherUser));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        assertThrows(AccessDeniedException.class,
                () -> supportRealtimeService.handleRealtimeMessage("other_user", testRequest));
    }
}