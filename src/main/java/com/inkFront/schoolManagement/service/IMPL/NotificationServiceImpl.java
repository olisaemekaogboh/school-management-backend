package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.service.NotificationService;
import com.inkFront.schoolManagement.service.SmsResult;
import com.inkFront.schoolManagement.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AnnouncementService announcementService;

    @Override
    public SmsResult sendAnnouncementNotifications(Long announcementId) {
        return announcementService.sendAnnouncementNotifications(announcementId);
    }
}