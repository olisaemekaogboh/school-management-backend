package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.model.EmailLog;
import com.inkFront.schoolManagement.model.EmailQueue;
import com.inkFront.schoolManagement.model.EmailQueueStatus;
import com.inkFront.schoolManagement.repository.EmailLogRepository;
import com.inkFront.schoolManagement.repository.EmailQueueRepository;
import com.inkFront.schoolManagement.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueServiceImplTest {

    @Mock
    private EmailQueueRepository emailQueueRepository;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private EmailQueueServiceImpl emailQueueService;

    private EmailQueue testQueueItem;
    private EmailLog testEmailLog;

    @BeforeEach
    void setUp() {
        testQueueItem = EmailQueue.builder()
                .id(1L)
                .announcementId(1L)
                .toEmail("test@example.com")
                .subject("Test Subject")
                .messageContent("Test Content")
                .status(EmailQueueStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        testEmailLog = EmailLog.builder()
                .id(1L)
                .announcementId(1L)
                .toEmail("test@example.com")
                .subject("Test Subject")
                .messageContent("Test Content")
                .status("PENDING")
                .build();
    }

    @Test
    void queueEmail_ShouldSaveQueueAndLog() {
        when(emailQueueRepository.save(any(EmailQueue.class))).thenReturn(testQueueItem);
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(testEmailLog);

        emailQueueService.queueEmail(1L, "test@example.com", "Test Subject", "Test Content");

        verify(emailQueueRepository, times(1)).save(any(EmailQueue.class));
        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
    }

    @Test
    void processQueue_WithPendingEmails_ShouldProcessThem() {
        List<EmailQueue> pendingEmails = Arrays.asList(testQueueItem);
        when(emailQueueRepository.findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class))).thenReturn(pendingEmails);
        when(emailQueueRepository.save(any(EmailQueue.class))).thenReturn(testQueueItem);
        doNothing().when(emailNotificationService).sendEmail(anyString(), anyString(), anyString());

        emailQueueService.processQueue();

        verify(emailNotificationService, times(1)).sendEmail(anyString(), anyString(), anyString());
        verify(emailQueueRepository, times(2)).save(any(EmailQueue.class));
    }

    @Test
    void processQueue_WithNoPendingEmails_ShouldDoNothing() {
        when(emailQueueRepository.findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class))).thenReturn(Arrays.asList());

        emailQueueService.processQueue();

        verify(emailNotificationService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(emailQueueRepository, never()).save(any(EmailQueue.class));
    }

    @Test
    void processQueue_WithEmailFailure_ShouldRetry() {
        List<EmailQueue> pendingEmails = Arrays.asList(testQueueItem);
        when(emailQueueRepository.findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class))).thenReturn(pendingEmails);
        when(emailQueueRepository.save(any(EmailQueue.class))).thenReturn(testQueueItem);
        doThrow(new RuntimeException("Email send failed")).when(emailNotificationService)
                .sendEmail(anyString(), anyString(), anyString());

        emailQueueService.processQueue();

        verify(emailNotificationService, times(1)).sendEmail(anyString(), anyString(), anyString());
        verify(emailQueueRepository, times(2)).save(any(EmailQueue.class));
        assertEquals(EmailQueueStatus.RETRYING, testQueueItem.getStatus());
    }

    @Test
    void processQueue_WithMaxRetriesExceeded_ShouldMarkFailed() {
        testQueueItem.setRetryCount(3);
        List<EmailQueue> pendingEmails = Arrays.asList(testQueueItem);

        when(emailQueueRepository.findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                anyList(), any(LocalDateTime.class))).thenReturn(pendingEmails);
        when(emailQueueRepository.save(any(EmailQueue.class))).thenReturn(testQueueItem);
        doThrow(new RuntimeException("Email send failed")).when(emailNotificationService)
                .sendEmail(anyString(), anyString(), anyString());

        emailQueueService.processQueue();

        verify(emailNotificationService, times(1)).sendEmail(anyString(), anyString(), anyString());
        verify(emailQueueRepository, times(2)).save(any(EmailQueue.class));
        assertEquals(EmailQueueStatus.FAILED, testQueueItem.getStatus());
    }

    @Test
    void getAllQueuedEmails_ShouldReturnList() {
        List<EmailQueue> emails = Arrays.asList(testQueueItem);
        when(emailQueueRepository.findAllByOrderByCreatedAtDesc()).thenReturn(emails);

        List<EmailQueue> result = emailQueueService.getAllQueuedEmails();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(emailQueueRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getQueuedEmailsByStatus_ShouldReturnFilteredList() {
        List<EmailQueue> emails = Arrays.asList(testQueueItem);
        when(emailQueueRepository.findByStatusOrderByCreatedAtDesc(EmailQueueStatus.PENDING))
                .thenReturn(emails);

        List<EmailQueue> result = emailQueueService.getQueuedEmailsByStatus(EmailQueueStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(emailQueueRepository, times(1)).findByStatusOrderByCreatedAtDesc(EmailQueueStatus.PENDING);
    }

    @Test
    void getQueuedEmailsByAnnouncement_ShouldReturnList() {
        List<EmailQueue> emails = Arrays.asList(testQueueItem);
        when(emailQueueRepository.findByAnnouncementId(1L)).thenReturn(emails);

        List<EmailQueue> result = emailQueueService.getQueuedEmailsByAnnouncement(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(emailQueueRepository, times(1)).findByAnnouncementId(1L);
    }

    @Test
    void getQueueStats_ShouldReturnStatistics() {
        when(emailQueueRepository.countByStatus(EmailQueueStatus.PENDING)).thenReturn(5L);
        when(emailQueueRepository.countByStatus(EmailQueueStatus.PROCESSING)).thenReturn(2L);
        when(emailQueueRepository.countByStatus(EmailQueueStatus.SENT)).thenReturn(10L);
        when(emailQueueRepository.countByStatus(EmailQueueStatus.FAILED)).thenReturn(3L);
        when(emailQueueRepository.countByStatus(EmailQueueStatus.RETRYING)).thenReturn(1L);
        when(emailQueueRepository.count()).thenReturn(21L);

        Map<String, Long> stats = emailQueueService.getQueueStats();

        assertNotNull(stats);
        assertEquals(5L, stats.get("PENDING"));
        assertEquals(2L, stats.get("PROCESSING"));
        assertEquals(10L, stats.get("SENT"));
        assertEquals(3L, stats.get("FAILED"));
        assertEquals(1L, stats.get("RETRYING"));
        assertEquals(21L, stats.get("TOTAL"));
    }

    @Test
    void retryEmail_ShouldMarkForRetry() {
        when(emailQueueRepository.findById(1L)).thenReturn(Optional.of(testQueueItem));
        when(emailQueueRepository.save(any(EmailQueue.class))).thenReturn(testQueueItem);

        emailQueueService.retryEmail(1L);

        assertEquals(EmailQueueStatus.RETRYING, testQueueItem.getStatus());
        verify(emailQueueRepository, times(1)).save(testQueueItem);
    }

    @Test
    void retryEmail_WithNonExistentId_ShouldThrowException() {
        when(emailQueueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            emailQueueService.retryEmail(999L);
        });
    }

    @Test
    void retryEmail_WithSentEmail_ShouldThrowException() {
        testQueueItem.setStatus(EmailQueueStatus.SENT);
        when(emailQueueRepository.findById(1L)).thenReturn(Optional.of(testQueueItem));

        assertThrows(RuntimeException.class, () -> {
            emailQueueService.retryEmail(1L);
        });
    }

    @Test
    void calculateRetryDelayMinutes_ShouldReturnCorrectDelays() {
        int delay1 = ReflectionTestUtils.invokeMethod(emailQueueService, "calculateRetryDelayMinutes", 1);
        int delay2 = ReflectionTestUtils.invokeMethod(emailQueueService, "calculateRetryDelayMinutes", 2);
        int delay3 = ReflectionTestUtils.invokeMethod(emailQueueService, "calculateRetryDelayMinutes", 3);

        assertEquals(1, delay1);
        assertEquals(5, delay2);
        assertEquals(15, delay3);
    }
}
