// src/main/java/com/inkFront/schoolManagement/service/IMPL/EmailQueueServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.model.EmailLog;
import com.inkFront.schoolManagement.model.EmailQueue;
import com.inkFront.schoolManagement.model.EmailQueueStatus;
import com.inkFront.schoolManagement.repository.EmailLogRepository;
import com.inkFront.schoolManagement.repository.EmailQueueRepository;
import com.inkFront.schoolManagement.service.EmailNotificationService;
import com.inkFront.schoolManagement.service.EmailQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailQueueServiceImpl implements EmailQueueService {

    private final EmailQueueRepository emailQueueRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailNotificationService emailNotificationService;

    private static final int DEFAULT_MAX_RETRIES = 3;

    @Override
    @Transactional
    public void queueEmail(Long announcementId, String to, String subject, String body) {
        EmailQueue queueItem = EmailQueue.builder()
                .announcementId(announcementId)
                .toEmail(to)
                .subject(subject)
                .messageContent(body)
                .status(EmailQueueStatus.PENDING)
                .retryCount(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .nextRetryAt(LocalDateTime.now())
                .build();

        emailQueueRepository.save(queueItem);

        EmailLog emailLog = EmailLog.builder()
                .announcementId(announcementId)
                .toEmail(to)
                .subject(subject)
                .messageContent(body)
                .status("PENDING")
                .build();

        emailLogRepository.save(emailLog);

        log.info("Queued email for {}", to);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 30000)
    public void processQueue() {
        List<EmailQueue> pendingEmails =
                emailQueueRepository.findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(EmailQueueStatus.PENDING, EmailQueueStatus.RETRYING),
                        LocalDateTime.now()
                );

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("Processing {} queued emails", pendingEmails.size());

        for (EmailQueue item : pendingEmails) {
            processSingleEmail(item);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailQueue> getAllQueuedEmails() {
        return emailQueueRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailQueue> getQueuedEmailsByStatus(EmailQueueStatus status) {
        return emailQueueRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailQueue> getQueuedEmailsByAnnouncement(Long announcementId) {
        return emailQueueRepository.findByAnnouncementId(announcementId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getQueueStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("PENDING", emailQueueRepository.countByStatus(EmailQueueStatus.PENDING));
        stats.put("PROCESSING", emailQueueRepository.countByStatus(EmailQueueStatus.PROCESSING));
        stats.put("SENT", emailQueueRepository.countByStatus(EmailQueueStatus.SENT));
        stats.put("FAILED", emailQueueRepository.countByStatus(EmailQueueStatus.FAILED));
        stats.put("RETRYING", emailQueueRepository.countByStatus(EmailQueueStatus.RETRYING));
        stats.put("TOTAL", emailQueueRepository.count());
        return stats;
    }

    @Override
    @Transactional
    public void retryEmail(Long queueId) {
        EmailQueue item = emailQueueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Email queue item not found: " + queueId));

        if (item.getStatus() == EmailQueueStatus.SENT) {
            throw new RuntimeException("Cannot retry an email that is already sent");
        }

        item.setStatus(EmailQueueStatus.RETRYING);
        item.setNextRetryAt(LocalDateTime.now());
        item.setErrorMessage(null);

        emailQueueRepository.save(item);
        log.info("Marked email queue {} for retry", queueId);
    }

    private void processSingleEmail(EmailQueue item) {
        try {
            item.setStatus(EmailQueueStatus.PROCESSING);
            item.setLastAttemptAt(LocalDateTime.now());
            emailQueueRepository.save(item);

            emailNotificationService.sendEmail(
                    item.getToEmail(),
                    item.getSubject(),
                    item.getMessageContent()
            );

            item.setStatus(EmailQueueStatus.SENT);
            item.setSentAt(LocalDateTime.now());
            item.setErrorMessage(null);
            emailQueueRepository.save(item);

            updateLatestEmailLog(item, "SENT", null, LocalDateTime.now());

            log.info("Email sent successfully to {}", item.getToEmail());

        } catch (Exception e) {
            int nextRetryCount = item.getRetryCount() + 1;
            item.setRetryCount(nextRetryCount);
            item.setErrorMessage(e.getMessage());

            if (nextRetryCount >= item.getMaxRetries()) {
                item.setStatus(EmailQueueStatus.FAILED);
                updateLatestEmailLog(item, "FAILED", e.getMessage(), null);
                log.error("Email permanently failed for {} after {} retries",
                        item.getToEmail(), nextRetryCount, e);
            } else {
                item.setStatus(EmailQueueStatus.RETRYING);
                item.setNextRetryAt(LocalDateTime.now().plusMinutes(calculateRetryDelayMinutes(nextRetryCount)));
                updateLatestEmailLog(item, "RETRYING", e.getMessage(), null);
                log.warn("Email retry scheduled for {}. Retry count: {}",
                        item.getToEmail(), nextRetryCount);
            }

            emailQueueRepository.save(item);
        }
    }

    private int calculateRetryDelayMinutes(int retryCount) {
        return switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            default -> 15;
        };
    }

    private void updateLatestEmailLog(EmailQueue item, String status, String errorMessage, LocalDateTime sentAt) {
        List<EmailLog> logs = emailLogRepository.findByAnnouncementId(item.getAnnouncementId());

        EmailLog targetLog = logs.stream()
                .filter(log -> item.getToEmail().equalsIgnoreCase(log.getToEmail()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (targetLog != null) {
            targetLog.setStatus(status);
            targetLog.setErrorMessage(errorMessage);
            if (sentAt != null) {
                targetLog.setSentAt(sentAt);
            }
            emailLogRepository.save(targetLog);
        }
    }
}