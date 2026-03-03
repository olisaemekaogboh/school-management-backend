// src/main/java/com/inkFront/schoolManagement/service/AnnouncementService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.AnnouncementDTO;
import com.inkFront.schoolManagement.model.Announcement;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnnouncementService {

    Announcement createAnnouncement(AnnouncementDTO announcementDTO);

    Announcement updateAnnouncement(Long id, AnnouncementDTO announcementDTO);

    void deleteAnnouncement(Long id);

    Announcement getAnnouncement(Long id);

    List<Announcement> getAllActiveAnnouncements();

    List<Announcement> getAnnouncementsByType(Announcement.AnnouncementType type);

    List<Announcement> getAnnouncementsByAudience(Announcement.Audience audience);

    List<Announcement> getUpcomingEvents();

    List<Announcement> getUpcomingFeeDeadlines();

    Map<String, Object> getSchoolCalendar(String session);

    Announcement createResumptionAnnouncement(String session, LocalDate date, String term);

    Announcement createMidtermBreakAnnouncement(String session, LocalDate start, LocalDate end);

    Announcement createResultReleaseAnnouncement(String session, String term, LocalDate date);

    Announcement createFeeAnnouncement(String description, Double amount, LocalDate dueDate, Announcement.Audience audience);

    SmsResult sendAnnouncementNotifications(Long id);
}