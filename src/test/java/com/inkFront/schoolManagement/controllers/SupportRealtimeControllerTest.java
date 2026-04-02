package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.support.SupportWsMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportWsMessageResponse;
import com.inkFront.schoolManagement.service.SupportRealtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportRealtimeControllerTest {

    @Mock
    private SupportRealtimeService supportRealtimeService;

    @Mock
    private Principal principal;

    @Mock
    private SupportWsMessageRequest request;

    @Mock
    private SupportWsMessageResponse response;

    @InjectMocks
    private SupportRealtimeController supportRealtimeController;

    @BeforeEach
    void setUp() {
        // no shared stubbing here
    }

    @Test
    void sendMessage_WithValidPrincipal_ShouldReturnResponse() {
        when(principal.getName()).thenReturn("john.doe@example.com");
        when(response.getTicketId()).thenReturn(1L);
        when(response.getMessage()).thenReturn("Hello, I need help");
        when(supportRealtimeService.handleRealtimeMessage(eq("john.doe@example.com"), eq(request)))
                .thenReturn(response);

        SupportWsMessageResponse result = supportRealtimeController.sendMessage(principal, request);

        assertNotNull(result);
        assertEquals(1L, result.getTicketId());
        assertEquals("Hello, I need help", result.getMessage());

        verify(supportRealtimeService, times(1))
                .handleRealtimeMessage(eq("john.doe@example.com"), eq(request));
    }

    @Test
    void sendMessage_WithNullPrincipal_ShouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                supportRealtimeController.sendMessage(null, request)
        );

        assertEquals("Unauthenticated WebSocket session", exception.getMessage());

        verify(supportRealtimeService, never()).handleRealtimeMessage(anyString(), any());
    }

    @Test
    void sendMessage_WithEmptyPrincipalName_ShouldThrowException() {
        when(principal.getName()).thenReturn("");

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                supportRealtimeController.sendMessage(principal, request)
        );

        assertEquals("Unauthenticated WebSocket session", exception.getMessage());

        verify(supportRealtimeService, never()).handleRealtimeMessage(anyString(), any());
    }

    @Test
    void sendMessage_WithBlankPrincipalName_ShouldThrowException() {
        when(principal.getName()).thenReturn("   ");

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                supportRealtimeController.sendMessage(principal, request)
        );

        assertEquals("Unauthenticated WebSocket session", exception.getMessage());

        verify(supportRealtimeService, never()).handleRealtimeMessage(anyString(), any());
    }

    @Test
    void sendMessage_WithNullRequest_ShouldHandleGracefully() {
        when(principal.getName()).thenReturn("john.doe@example.com");
        when(supportRealtimeService.handleRealtimeMessage(eq("john.doe@example.com"), isNull()))
                .thenThrow(new IllegalArgumentException("Request cannot be null"));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                supportRealtimeController.sendMessage(principal, null)
        );

        assertEquals("Request cannot be null", exception.getMessage());

        verify(supportRealtimeService, times(1))
                .handleRealtimeMessage(eq("john.doe@example.com"), isNull());
    }
}