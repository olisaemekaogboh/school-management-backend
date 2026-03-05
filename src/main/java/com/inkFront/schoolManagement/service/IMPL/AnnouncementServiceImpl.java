// src/main/java/com/inkFront/schoolManagement/service/IMPL/AnnouncementServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.AnnouncementDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Announcement;
import com.inkFront.schoolManagement.model.SmsLog;
import com.inkFront.schoolManagement.repository.AnnouncementRepository;
import com.inkFront.schoolManagement.repository.SmsLogRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.AnnouncementService;
import com.inkFront.schoolManagement.service.EmailNotificationService;
import com.inkFront.schoolManagement.service.SmsResult;
import com.inkFront.schoolManagement.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final StudentRepository studentRepository;
    private final SmsService smsService;
    private final SmsLogRepository smsLogRepository;

    // ✅ keep these ONLY if you actually created these classes
    // If you haven't created them yet, comment them out until you do.
    private final EmailNotificationService emailNotificationService;
    private final com.inkFront.schoolManagement.repository.EmailLogRepository emailLogRepository;

    // =========================
    // CRUD
    // =========================

    @Override
    public Announcement createAnnouncement(AnnouncementDTO dto) {
        log.info("Creating new announcement: {}", dto.getTitle());
        Announcement announcement = mapToEntity(dto);
        return announcementRepository.save(announcement);
    }

    @Override
    public Announcement updateAnnouncement(Long id, AnnouncementDTO dto) {
        log.info("Updating announcement: {}", id);

        Announcement announcement = announcementRepository.findByIdWithAudience(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        updateEntity(announcement, dto);
        return announcementRepository.save(announcement);
    }

    @Override
    public void deleteAnnouncement(Long id) {
        log.info("Deleting announcement: {}", id);

        // delete logs first
        try {
            List<SmsLog> smsLogs = smsLogRepository.findByAnnouncementId(id);
            if (!smsLogs.isEmpty()) {
                smsLogRepository.deleteAll(smsLogs);
                log.info("Deleted {} SMS logs for announcement {}", smsLogs.size(), id);
            }
        } catch (Exception e) {
            log.error("Error deleting SMS logs: {}", e.getMessage());
        }

        announcementRepository.deleteById(id);
    }

    @Override
    public Announcement getAnnouncement(Long id) {
        // ✅ FIX: you were calling findAllActiveWithAudience() which returns List, not Optional
        return announcementRepository.findByIdWithAudience(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
    }

    // =========================
    // LIST / FILTER
    // =========================

    @Override
    public List<Announcement> getAllActiveAnnouncements() {
        return announcementRepository.findAllActiveWithAudience();
    }

    @Override
    public List<Announcement> getAnnouncementsByType(Announcement.AnnouncementType type) {
        return announcementRepository.findByTypeActiveWithAudience(type);
    }

    @Override
    public List<Announcement> getAnnouncementsByAudience(Announcement.Audience audience) {
        return announcementRepository.findByAudienceActiveWithAudience(audience);
    }

    @Override
    public List<Announcement> getUpcomingEvents() {
        return announcementRepository.findByEventDateAfterWithAudience(LocalDate.now());
    }

    @Override
    public List<Announcement> getUpcomingFeeDeadlines() {
        return announcementRepository.findByFeeDueDateAfterWithAudience(LocalDate.now());
    }

    // =========================
    // CALENDAR
    // =========================

    @Override
    public Map<String, Object> getSchoolCalendar(String session) {
        Map<String, Object> calendar = new HashMap<>();
        List<Map<String, Object>> events = new ArrayList<>();

        // First Term
        events.add(createCalendarEvent("First Term Resumption",
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 9, 8)));
        events.add(createCalendarEvent("First Term Midterm Break",
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 10, 15),
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 10, 22)));
        events.add(createCalendarEvent("First Term Exams",
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 11, 20),
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 11, 30)));
        events.add(createCalendarEvent("First Term Ends",
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 12, 12)));
        events.add(createCalendarEvent("First Term Results Release",
                LocalDate.of(Integer.parseInt(session.split("/")[0]), 12, 20)));

        // Second Term
        events.add(createCalendarEvent("Second Term Resumption",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 1, 6)));
        events.add(createCalendarEvent("Second Term Midterm Break",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 2, 10),
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 2, 17)));
        events.add(createCalendarEvent("Second Term Exams",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 3, 15),
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 3, 25)));
        events.add(createCalendarEvent("Second Term Ends",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 4, 5)));
        events.add(createCalendarEvent("Second Term Results Release",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 4, 15)));

        // Third Term
        events.add(createCalendarEvent("Third Term Resumption",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 4, 22)));
        events.add(createCalendarEvent("Third Term Midterm Break",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 5, 20),
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 5, 27)));
        events.add(createCalendarEvent("Third Term Exams",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 6, 15),
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 6, 25)));
        events.add(createCalendarEvent("Third Term Ends",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 7, 15)));
        events.add(createCalendarEvent("Third Term Results Release",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 7, 25)));
        events.add(createCalendarEvent("Graduation Ceremony",
                LocalDate.of(Integer.parseInt(session.split("/")[0]) + 1, 7, 30)));

        calendar.put("session", session);
        calendar.put("events", events);
        return calendar;
    }

    // =========================
    // QUICK CREATE HELPERS
    // =========================

    @Override
    public Announcement createResumptionAnnouncement(String session, LocalDate date, String term) {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title(term + " Term Resumption")
                .content("School resumes for " + term + " term of " + session + " session on " +
                        date.getDayOfMonth() + " " + date.getMonth() + ", " + date.getYear())
                .type(Announcement.AnnouncementType.RESUMPTION)
                .priority(Announcement.AnnouncementPriority.HIGH)
                .audience(List.of(Announcement.Audience.ALL))
                .eventDate(date)
                .startDate(date.minusDays(7))
                .endDate(date)
                .term(term)
                .session(session)
                .active(true)
                .build();

        return createAnnouncement(dto);
    }

    @Override
    public Announcement createMidtermBreakAnnouncement(String session, LocalDate start, LocalDate end) {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title("Midterm Break")
                .content("Midterm break will run from " + start + " to " + end +
                        ". School resumes on " + end.plusDays(1))
                .type(Announcement.AnnouncementType.MIDTERM_BREAK)
                .priority(Announcement.AnnouncementPriority.NORMAL)
                .audience(List.of(Announcement.Audience.ALL))
                .startDate(start)
                .endDate(end)
                .eventDate(start)
                .session(session)
                .active(true)
                .build();

        return createAnnouncement(dto);
    }

    @Override
    public Announcement createResultReleaseAnnouncement(String session, String term, LocalDate date) {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title(term + " Term Results Release")
                .content(term + " term results for " + session + " session will be released on " +
                        date + ". Parents can access results via the portal.")
                .type(Announcement.AnnouncementType.RESULT)
                .priority(Announcement.AnnouncementPriority.HIGH)
                .audience(List.of(Announcement.Audience.PARENTS, Announcement.Audience.STUDENTS))
                .resultReleaseDate(date)
                .term(term)
                .session(session)
                .active(true)
                .build();

        return createAnnouncement(dto);
    }

    @Override
    public Announcement createFeeAnnouncement(String description, Double amount,
                                              LocalDate dueDate, Announcement.Audience audience) {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title("School Fees Announcement")
                .content(description + " - Amount: ₦" + amount + ". Payment due by " + dueDate)
                .type(Announcement.AnnouncementType.FEE)
                .priority(Announcement.AnnouncementPriority.HIGH)
                .audience(List.of(audience))
                .feeAmount(amount)
                .feeDescription(description)
                .feeDueDate(dueDate)
                .active(true)
                .build();

        return createAnnouncement(dto);
    }

    // =========================
    // ✅ REQUIRED BY INTERFACE
    // =========================

    @Override
    public SmsResult sendAnnouncementNotifications(Long announcementId) {
        Announcement announcement = getAnnouncement(announcementId);
        log.info("Sending notifications for announcement: {}", announcement.getTitle());

        // 1) Try SMS first
        List<String> phoneNumbers = getRecipientPhoneNumbers(announcement.getAudience());

        if (!phoneNumbers.isEmpty()) {
            String smsMessage = formatSmsMessage(announcement);

            // attach announcement to SMS logs if your SmsService supports it
            if (smsService instanceof AfricaTalkingSmsServiceImpl impl) {
                impl.setCurrentAnnouncement(announcement);
            }

            List<SmsResult> results = smsService.sendBulkSms(phoneNumbers, smsMessage);

            if (smsService instanceof AfricaTalkingSmsServiceImpl impl) {
                impl.setCurrentAnnouncement(null);
            }

            SmsResult summary = results.isEmpty() ? null : results.get(0);
            if (summary != null && summary.getSuccessCount() > 0) {
                return summary;
            }
        }

        // 2) Fallback to Email
        return sendAnnouncementEmails(announcement);
    }

    // =========================
    // EMAIL HELPERS
    // =========================

    private List<String> getRecipientEmails(List<Announcement.Audience> audiences) {
        List<String> emails = new ArrayList<>();

        for (Announcement.Audience audience : audiences) {
            switch (audience) {
                case ALL:
                case PARENTS:
                    emails.addAll(studentRepository.findAll().stream()
                            .map(s -> s.getParentEmail())
                            .filter(e -> e != null && !e.trim().isEmpty())
                            .toList());
                    break;
                case STUDENTS:
                case TEACHERS:
                default:
                    break;
            }
        }
        return emails.stream().distinct().toList();
    }

    private SmsResult sendAnnouncementEmails(Announcement announcement) {
        List<String> emails = getRecipientEmails(announcement.getAudience());

        if (emails.isEmpty()) {
            return SmsResult.builder()
                    .successCount(0)
                    .failedCount(0)
                    .message("No recipient emails found")
                    .build();
        }

        String subject = "School Announcement: " + announcement.getTitle();
        String body = formatEmailMessage(announcement);

        int success = 0;
        int failed = 0;

        for (String to : emails) {
            // If you haven't created EmailLog entity yet, remove this block and just send emails.
            try {
                // Optional logging (requires EmailLog entity + repo)
                // EmailLog logRow = EmailLog.builder()...build();
                // logRow = emailLogRepository.save(logRow);

                emailNotificationService.sendEmail(to, subject, body);

                // logRow.setStatus("SENT"); logRow.setSentAt(LocalDateTime.now()); emailLogRepository.save(logRow);
                success++;
            } catch (Exception e) {
                // logRow.setStatus("FAILED"); logRow.setErrorMessage(e.getMessage()); emailLogRepository.save(logRow);
                failed++;
                log.error("Email send failed to {}: {}", to, e.getMessage());
            }
        }

        return SmsResult.builder()
                .successCount(success)
                .failedCount(failed)
                .message(String.format("📧 Email sent: %d success, %d failed", success, failed))
                .build();
    }

    private String formatEmailMessage(Announcement announcement) {
        StringBuilder message = new StringBuilder();

        message.append("FAITH FOUNDATION INTERNATIONAL SCHOOL\n\n");
        message.append("Title: ").append(announcement.getTitle()).append("\n");
        message.append("Priority: ").append(announcement.getPriority()).append("\n");
        message.append("Type: ").append(announcement.getType()).append("\n\n");
        message.append(announcement.getContent()).append("\n\n");

        if (announcement.getEventDate() != null) {
            message.append("Date: ").append(announcement.getEventDate()).append("\n");
            if (announcement.getEventTime() != null) message.append("Time: ").append(announcement.getEventTime()).append("\n");
            if (announcement.getEventLocation() != null) message.append("Location: ").append(announcement.getEventLocation()).append("\n");
            message.append("\n");
        }

        if (announcement.getFeeAmount() != null) {
            message.append("Amount: ₦").append(String.format("%,.0f", announcement.getFeeAmount())).append("\n");
            if (announcement.getFeeDueDate() != null) message.append("Due date: ").append(announcement.getFeeDueDate()).append("\n");
            message.append("\n");
        }

        if (announcement.getResultReleaseDate() != null) {
            message.append("Results release: ").append(announcement.getResultReleaseDate()).append("\n\n");
        }

        message.append("Portal: http://localhost:3000\n");
        message.append("--\nSchool Admin");
        return message.toString();
    }

    // =========================
    // SMS HELPERS
    // =========================

    private List<String> getRecipientPhoneNumbers(List<Announcement.Audience> audiences) {
        List<String> phoneNumbers = new ArrayList<>();

        for (Announcement.Audience audience : audiences) {
            switch (audience) {
                case ALL:
                case PARENTS:
                    phoneNumbers.addAll(getAllParentPhoneNumbers());
                    break;
                case STUDENTS:
                    phoneNumbers.addAll(getStudentEmergencyContacts());
                    break;
                case TEACHERS:
                default:
                    break;
            }
        }

        return phoneNumbers.stream().distinct().collect(Collectors.toList());
    }

    private List<String> getAllParentPhoneNumbers() {
        return studentRepository.findAll().stream()
                .map(student -> student.getParentPhone())
                .filter(phone -> phone != null && !phone.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> getStudentEmergencyContacts() {
        return studentRepository.findAll().stream()
                .map(student -> student.getEmergencyContactPhone())
                .filter(phone -> phone != null && !phone.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String formatSmsMessage(Announcement announcement) {
        StringBuilder message = new StringBuilder();

        if (announcement.getPriority() == Announcement.AnnouncementPriority.URGENT) {
            message.append("🚨 URGENT: ");
        } else if (announcement.getPriority() == Announcement.AnnouncementPriority.HIGH) {
            message.append("📢 IMPORTANT: ");
        }

        message.append(announcement.getTitle()).append("\n\n");
        message.append(announcement.getContent());

        if (announcement.getEventDate() != null) {
            message.append("\n\n📅 Date: ").append(announcement.getEventDate());
            if (announcement.getEventTime() != null) message.append(" at ").append(announcement.getEventTime());
            if (announcement.getEventLocation() != null) message.append("\n📍 Location: ").append(announcement.getEventLocation());
        }

        if (announcement.getFeeAmount() != null) {
            message.append("\n\n💰 Amount: ₦").append(String.format("%,.0f", announcement.getFeeAmount()));
            if (announcement.getFeeDueDate() != null) message.append("\n⏰ Due Date: ").append(announcement.getFeeDueDate());
        }

        if (announcement.getResultReleaseDate() != null) {
            message.append("\n\n📊 Results Release Date: ").append(announcement.getResultReleaseDate());
        }

        message.append("\n\n---\nFaith Foundation International School");
        message.append("\nVisit portal for more details");
        return message.toString();
    }

    // =========================
    // ENTITY MAPPING
    // =========================

    private Map<String, Object> createCalendarEvent(String title, LocalDate date) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("date", date);
        event.put("type", "single");
        return event;
    }

    private Map<String, Object> createCalendarEvent(String title, LocalDate start, LocalDate end) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("startDate", start);
        event.put("endDate", end);
        event.put("type", "range");
        return event;
    }

    private Announcement mapToEntity(AnnouncementDTO dto) {
        Announcement announcement = new Announcement();
        updateEntity(announcement, dto);
        return announcement;
    }

    private void updateEntity(Announcement announcement, AnnouncementDTO dto) {
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setType(dto.getType());
        announcement.setPriority(dto.getPriority());
        announcement.setAudience(dto.getAudience() != null ? dto.getAudience() : new ArrayList<>());
        announcement.setStartDate(dto.getStartDate());
        announcement.setEndDate(dto.getEndDate());
        announcement.setActive(dto.isActive());
        announcement.setCreatedBy(dto.getCreatedBy());
        announcement.setEventDate(dto.getEventDate());
        announcement.setEventLocation(dto.getEventLocation());
        announcement.setEventTime(dto.getEventTime());
        announcement.setFeeAmount(dto.getFeeAmount());
        announcement.setFeeDescription(dto.getFeeDescription());
        announcement.setFeeDueDate(dto.getFeeDueDate());
        announcement.setResultReleaseDate(dto.getResultReleaseDate());
        announcement.setTerm(dto.getTerm());
        announcement.setSession(dto.getSession());
        announcement.setAttachmentUrl(dto.getAttachmentUrl());
        announcement.setAttachmentName(dto.getAttachmentName());
    }
}