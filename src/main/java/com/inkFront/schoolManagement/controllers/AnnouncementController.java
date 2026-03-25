// src/main/java/com/inkFront/schoolManagement/controllers/AnnouncementController.java
package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.AnnouncementDTO;
import com.inkFront.schoolManagement.dto.SmsLogDTO;
import com.inkFront.schoolManagement.model.Announcement;
import com.inkFront.schoolManagement.repository.SmsLogRepository;
import com.inkFront.schoolManagement.repository.EmailLogRepository;
import com.inkFront.schoolManagement.service.AnnouncementService;
import com.inkFront.schoolManagement.service.SmsResult;
import com.inkFront.schoolManagement.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "https://localhost:3000")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SmsLogRepository smsLogRepository;
    private final EmailLogRepository emailLogRepository;
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<AnnouncementDTO> createAnnouncement(@Valid @RequestBody AnnouncementDTO announcementDTO) {
        Announcement announcement = announcementService.createAnnouncement(announcementDTO);
        return new ResponseEntity<>(AnnouncementDTO.fromAnnouncement(announcement), HttpStatus.CREATED);
    }
    // In AnnouncementController.java

    @PostMapping("/{id}/notify")
    public ResponseEntity<SmsResult> sendNotifications(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.sendAnnouncementNotifications(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementDTO> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementDTO announcementDTO) {
        Announcement announcement = announcementService.updateAnnouncement(id, announcementDTO);
        return ResponseEntity.ok(AnnouncementDTO.fromAnnouncement(announcement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }
    // In AnnouncementController.java
    @GetMapping("/{id}/email-history")
    public ResponseEntity<List<com.inkFront.schoolManagement.dto.EmailLogDTO>> getEmailHistory(@PathVariable Long id) {
        var logs = emailLogRepository.findByAnnouncementId(id).stream()
                .map(com.inkFront.schoolManagement.dto.EmailLogDTO::fromEmailLog)
                .toList();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDTO> getAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncement(id);
        return ResponseEntity.ok(AnnouncementDTO.fromAnnouncement(announcement));
    }

    @GetMapping
    public ResponseEntity<List<AnnouncementDTO>> getAllActiveAnnouncements() {
        List<Announcement> announcements = announcementService.getAllActiveAnnouncements();
        List<AnnouncementDTO> dtos = announcements.stream()
                .map(AnnouncementDTO::fromAnnouncement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AnnouncementDTO>> getAnnouncementsByType(
            @PathVariable Announcement.AnnouncementType type) {
        List<Announcement> announcements = announcementService.getAnnouncementsByType(type);
        List<AnnouncementDTO> dtos = announcements.stream()
                .map(AnnouncementDTO::fromAnnouncement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/audience/{audience}")
    public ResponseEntity<List<AnnouncementDTO>> getAnnouncementsByAudience(
            @PathVariable Announcement.Audience audience) {
        List<Announcement> announcements = announcementService.getAnnouncementsByAudience(audience);
        List<AnnouncementDTO> dtos = announcements.stream()
                .map(AnnouncementDTO::fromAnnouncement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/upcoming/events")
    public ResponseEntity<List<AnnouncementDTO>> getUpcomingEvents() {
        List<Announcement> announcements = announcementService.getUpcomingEvents();
        List<AnnouncementDTO> dtos = announcements.stream()
                .map(AnnouncementDTO::fromAnnouncement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/upcoming/fees")
    public ResponseEntity<List<AnnouncementDTO>> getUpcomingFeeDeadlines() {
        List<Announcement> announcements = announcementService.getUpcomingFeeDeadlines();
        List<AnnouncementDTO> dtos = announcements.stream()
                .map(AnnouncementDTO::fromAnnouncement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getSchoolCalendar(@RequestParam String session) {
        Map<String, Object> calendar = announcementService.getSchoolCalendar(session);
        return ResponseEntity.ok(calendar);
    }

    @PostMapping("/resumption")
    public ResponseEntity<AnnouncementDTO> createResumptionAnnouncement(
            @RequestParam String session,
            @RequestParam LocalDate date,
            @RequestParam String term) {
        Announcement announcement = announcementService.createResumptionAnnouncement(session, date, term);
        return new ResponseEntity<>(AnnouncementDTO.fromAnnouncement(announcement), HttpStatus.CREATED);
    }

    @PostMapping("/midterm-break")
    public ResponseEntity<AnnouncementDTO> createMidtermBreakAnnouncement(
            @RequestParam String session,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        Announcement announcement = announcementService.createMidtermBreakAnnouncement(session, start, end);
        return new ResponseEntity<>(AnnouncementDTO.fromAnnouncement(announcement), HttpStatus.CREATED);
    }

    @PostMapping("/result-release")
    public ResponseEntity<AnnouncementDTO> createResultReleaseAnnouncement(
            @RequestParam String session,
            @RequestParam String term,
            @RequestParam LocalDate date) {
        Announcement announcement = announcementService.createResultReleaseAnnouncement(session, term, date);
        return new ResponseEntity<>(AnnouncementDTO.fromAnnouncement(announcement), HttpStatus.CREATED);
    }

    @PostMapping("/fee")
    public ResponseEntity<AnnouncementDTO> createFeeAnnouncement(
            @RequestParam String description,
            @RequestParam Double amount,
            @RequestParam LocalDate dueDate,
            @RequestParam Announcement.Audience audience) {
        Announcement announcement = announcementService.createFeeAnnouncement(description, amount, dueDate, audience);
        return new ResponseEntity<>(AnnouncementDTO.fromAnnouncement(announcement), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/sms-history")
    public ResponseEntity<List<SmsLogDTO>> getSmsHistory(@PathVariable Long id) {
        List<SmsLogDTO> history = smsLogRepository.findByAnnouncementId(id)
                .stream()
                .map(SmsLogDTO::fromSmsLog)
                .toList();

        return ResponseEntity.ok(history);
    }


    @GetMapping("/sms-stats")
    public ResponseEntity<Map<String, Object>> getSmsStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", smsLogRepository.count());
        stats.put("delivered", smsLogRepository.countByStatus("DELIVERED"));
        stats.put("sent", smsLogRepository.countByStatus("SENT"));
        stats.put("failed", smsLogRepository.countByStatus("FAILED"));
        stats.put("pending", smsLogRepository.countByStatus("PENDING"));

        return ResponseEntity.ok(stats);
    }
}