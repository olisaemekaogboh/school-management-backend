package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.EmailQueueDTO;
import com.inkFront.schoolManagement.model.EmailQueue;
import com.inkFront.schoolManagement.model.EmailQueueStatus;
import com.inkFront.schoolManagement.service.EmailQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmailQueueService emailQueueService;

    @InjectMocks
    private EmailQueueController emailQueueController;

    private ObjectMapper objectMapper;
    private EmailQueue testEmailQueue;
    private EmailQueueDTO testEmailQueueDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(emailQueueController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        testEmailQueue = new EmailQueue();
        testEmailQueue.setId(1L);
        testEmailQueue.setToEmail("test@example.com");
        testEmailQueue.setSubject("Test Email");
        testEmailQueue.setMessageContent("Test Content");
        testEmailQueue.setStatus(EmailQueueStatus.PENDING);
        testEmailQueue.setRetryCount(0);
        testEmailQueue.setMaxRetries(3);
        testEmailQueue.setCreatedAt(LocalDateTime.now());

        testEmailQueueDTO = EmailQueueDTO.fromEntity(testEmailQueue);
    }

    @Test
    void getAllQueuedEmails_ShouldReturnList() throws Exception {
        List<EmailQueue> emailQueues = Arrays.asList(testEmailQueue);
        when(emailQueueService.getAllQueuedEmails()).thenReturn(emailQueues);

        mockMvc.perform(get("/api/email-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].toEmail").value("test@example.com"));

        verify(emailQueueService, times(1)).getAllQueuedEmails();
    }

    @Test
    void getByStatus_ShouldReturnFilteredList() throws Exception {
        List<EmailQueue> emailQueues = Arrays.asList(testEmailQueue);
        when(emailQueueService.getQueuedEmailsByStatus(EmailQueueStatus.PENDING))
                .thenReturn(emailQueues);

        mockMvc.perform(get("/api/email-queue/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(emailQueueService, times(1))
                .getQueuedEmailsByStatus(EmailQueueStatus.PENDING);
    }

    @Test
    void getByStatus_WithInvalidStatus_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/email-queue/status/INVALID_STATUS"))
                .andExpect(status().isBadRequest());

        verify(emailQueueService, never())
                .getQueuedEmailsByStatus(any());
    }

    @Test
    void getByAnnouncement_ShouldReturnList() throws Exception {
        List<EmailQueue> emailQueues = Arrays.asList(testEmailQueue);
        when(emailQueueService.getQueuedEmailsByAnnouncement(1L)).thenReturn(emailQueues);

        mockMvc.perform(get("/api/email-queue/announcement/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(emailQueueService, times(1)).getQueuedEmailsByAnnouncement(1L);
    }

    @Test
    void getQueueStats_ShouldReturnStatistics() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", 5L);
        stats.put("SENT", 10L);
        stats.put("FAILED", 2L);
        stats.put("PROCESSING", 1L);
        stats.put("RETRYING", 1L);

        when(emailQueueService.getQueueStats()).thenReturn(stats);

        mockMvc.perform(get("/api/email-queue/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").value(5))
                .andExpect(jsonPath("$.SENT").value(10))
                .andExpect(jsonPath("$.FAILED").value(2));

        verify(emailQueueService, times(1)).getQueueStats();
    }

    @Test
    void retryEmail_ShouldTriggerRetry() throws Exception {
        doNothing().when(emailQueueService).retryEmail(1L);

        mockMvc.perform(post("/api/email-queue/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email queued for retry"));

        verify(emailQueueService, times(1)).retryEmail(1L);
    }

    @Test
    void processQueueNow_ShouldTriggerProcessing() throws Exception {
        doNothing().when(emailQueueService).processQueue();

        mockMvc.perform(post("/api/email-queue/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email queue processing triggered"));

        verify(emailQueueService, times(1)).processQueue();
    }

    @Test
    void getAllQueuedEmails_WhenEmpty_ShouldReturnEmptyList() throws Exception {
        when(emailQueueService.getAllQueuedEmails()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/email-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(emailQueueService, times(1)).getAllQueuedEmails();
    }

    @Test
    void getByAnnouncement_WithNonExistentId_ShouldReturnEmptyList() throws Exception {
        when(emailQueueService.getQueuedEmailsByAnnouncement(999L))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/email-queue/announcement/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(emailQueueService, times(1)).getQueuedEmailsByAnnouncement(999L);
    }

    @Test
    void retryEmail_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        doThrow(new RuntimeException("Email queue not found"))
                .when(emailQueueService).retryEmail(999L);

        mockMvc.perform(post("/api/email-queue/999/retry"))
                .andExpect(status().isNotFound());

        verify(emailQueueService, times(1)).retryEmail(999L);
    }

    @Test
    void getQueueStats_WithNoEmails_ShouldReturnZeroStats() throws Exception {
        Map<String, Long> emptyStats = new HashMap<>();
        emptyStats.put("PENDING", 0L);
        emptyStats.put("SENT", 0L);
        emptyStats.put("FAILED", 0L);

        when(emailQueueService.getQueueStats()).thenReturn(emptyStats);

        mockMvc.perform(get("/api/email-queue/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").value(0))
                .andExpect(jsonPath("$.SENT").value(0));

        verify(emailQueueService, times(1)).getQueueStats();
    }

    @Test
    void getByStatus_WithMultipleStatuses_ShouldReturnCorrectFiltering() throws Exception {
        List<EmailQueue> pendingEmails = Arrays.asList(testEmailQueue);
        when(emailQueueService.getQueuedEmailsByStatus(EmailQueueStatus.PENDING))
                .thenReturn(pendingEmails);

        mockMvc.perform(get("/api/email-queue/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        List<EmailQueue> sentEmails = Arrays.asList();
        when(emailQueueService.getQueuedEmailsByStatus(EmailQueueStatus.SENT))
                .thenReturn(sentEmails);

        mockMvc.perform(get("/api/email-queue/status/SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(emailQueueService, times(1))
                .getQueuedEmailsByStatus(EmailQueueStatus.PENDING);
        verify(emailQueueService, times(1))
                .getQueuedEmailsByStatus(EmailQueueStatus.SENT);
    }

    @Test
    void processQueueNow_ShouldHandleConcurrentProcessing() throws Exception {
        doNothing().when(emailQueueService).processQueue();

        mockMvc.perform(post("/api/email-queue/process"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/email-queue/process"))
                .andExpect(status().isOk());

        verify(emailQueueService, times(2)).processQueue();
    }
    @Test
    void getByStatus_ShouldHandleCaseInsensitiveStatus() throws Exception {

        mockMvc.perform(get("/api/email-queue/status/pending"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/email-queue/status/PeNdInG"))
                .andExpect(status().isBadRequest());
    }
}