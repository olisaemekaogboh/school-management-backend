// src/test/java/com/inkFront/schoolManagement/service/IMPL/SupportServiceImplTest.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.support.CreateSupportTicketRequest;
import com.inkFront.schoolManagement.dto.support.SendSupportMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportTicketDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportServiceImplTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private SupportMessageRepository supportMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SupportServiceImpl supportService;

    private User testUser;
    private User testAdmin;
    private SupportTicket testTicket;
    private CreateSupportTicketRequest createRequest;

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

        createRequest = new CreateSupportTicketRequest();
        createRequest.setSubject("Login Issue");
        createRequest.setMessage("I cannot login to the portal");
        createRequest.setCategory("Technical");

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createTicket_ShouldCreateTicket() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.count()).thenReturn(0L);
        when(supportTicketRepository.existsByTicketNumber(anyString())).thenReturn(false);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(new SupportMessage());
        when(supportMessageRepository.findByTicketOrderByCreatedAtAsc(any(SupportTicket.class))).thenReturn(List.of());

        SupportTicketDTO result = supportService.createTicket(createRequest);

        assertNotNull(result);
        assertEquals("Login Issue", result.getSubject());
        verify(supportTicketRepository).save(any(SupportTicket.class));
        verify(supportMessageRepository).save(any(SupportMessage.class));
    }

    @Test
    void createTicket_AsAdmin_ShouldThrowException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsernameOrEmail("admin")).thenReturn(Optional.of(testAdmin));

        assertThrows(BusinessException.class, () -> supportService.createTicket(createRequest));
    }

    @Test
    void getMyTickets_ShouldReturnList() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findByCreatedByOrderByUpdatedAtDesc(testUser))
                .thenReturn(Arrays.asList(testTicket));

        List<SupportTicketDTO> result = supportService.getMyTickets();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllTicketsForAdmin_AsAdmin_ShouldReturnList() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsernameOrEmail("admin")).thenReturn(Optional.of(testAdmin));
        when(supportTicketRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(Arrays.asList(testTicket));

        List<SupportTicketDTO> result = supportService.getAllTicketsForAdmin();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllTicketsForAdmin_AsNonAdmin_ShouldThrowException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));

        assertThrows(AccessDeniedException.class, () -> supportService.getAllTicketsForAdmin());
    }

    @Test
    void sendMessage_ShouldAppendMessage() {
        SendSupportMessageRequest request = new SendSupportMessageRequest();
        request.setMessage("Any update?");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenReturn(new SupportMessage());
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);
        when(supportMessageRepository.findByTicketOrderByCreatedAtAsc(testTicket)).thenReturn(List.of());

        SupportTicketDTO result = supportService.sendMessage(1L, request);

        assertNotNull(result);
        verify(supportMessageRepository).save(any(SupportMessage.class));
        verify(supportTicketRepository).save(any(SupportTicket.class));
    }
}