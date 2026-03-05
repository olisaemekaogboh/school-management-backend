package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.service.SmsResult;

public interface NotificationService {

    SmsResult sendAnnouncementNotifications(Long announcementId);

}