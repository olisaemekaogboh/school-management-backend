package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.service.AnnouncementService;
import com.inkFront.schoolManagement.service.SmsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private SmsResult testResult;

    @BeforeEach
    void setUp() {
        testResult = SmsResult.builder()
                .successCount(10)
                .failedCount(2)
                .message("Notifications sent")
                .build();
    }

    @Test
    void sendAnnouncementNotifications_ShouldDelegateToAnnouncementService() {
        when(announcementService.sendAnnouncementNotifications(1L)).thenReturn(testResult);

        SmsResult result = notificationService.sendAnnouncementNotifications(1L);

        assertNotNull(result);
        assertEquals(10, result.getSuccessCount());
        assertEquals(2, result.getFailedCount());
        verify(announcementService, times(1)).sendAnnouncementNotifications(1L);
    }

    @Test
    void sendAnnouncementNotifications_WithNonExistentId_ShouldPropagateException() {
        when(announcementService.sendAnnouncementNotifications(999L))
                .thenThrow(new RuntimeException("Announcement not found"));

        assertThrows(RuntimeException.class, () -> {
            notificationService.sendAnnouncementNotifications(999L);
        });
    }
}
