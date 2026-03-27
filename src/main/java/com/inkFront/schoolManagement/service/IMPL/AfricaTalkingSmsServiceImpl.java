package com.inkFront.schoolManagement.service.IMPL;

import com.africastalking.AfricasTalking;
import com.africastalking.sms.Recipient;
import com.inkFront.schoolManagement.config.SmsConfig;
import com.inkFront.schoolManagement.model.Announcement;
import com.inkFront.schoolManagement.model.SmsLog;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.SmsLogRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.SmsResult;
import com.inkFront.schoolManagement.service.SmsService;
import com.inkFront.schoolManagement.service.SmsTemplateService;
import com.inkFront.schoolManagement.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AfricaTalkingSmsServiceImpl implements SmsService {

    private final SmsConfig smsConfig;
    private final SmsTemplateService templateService;
    private final SmsLogRepository smsLogRepository;
    private final StudentRepository studentRepository;

    private com.africastalking.SmsService smsService;
    private boolean initialized = false;

    // Stores current announcement context for logs
    private Announcement currentAnnouncement;

    private void initialize() {
        if (!initialized) {
            try {
                AfricasTalking.initialize(smsConfig.getUsername(), smsConfig.getApiKey());
                smsService = AfricasTalking.getService(com.africastalking.SmsService.class);
                initialized = true;
                log.info("Africa's Talking SMS service initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Africa's Talking SMS service: {}", e.getMessage(), e);
                throw new RuntimeException("SMS service initialization failed", e);
            }
        }
    }

    public void setCurrentAnnouncement(Announcement announcement) {
        this.currentAnnouncement = announcement;
    }

    @Override
    public SmsResult sendSms(String phoneNumber, String message) {
        try {
            initialize();

            String dbFormattedNumber = formatPhoneNumberForDatabase(phoneNumber);
            String smsFormattedNumber = formatPhoneNumberForSMS(phoneNumber);

            if (dbFormattedNumber == null || smsFormattedNumber == null) {
                logSmsFailure(phoneNumber, message, "Invalid phone number");
                return createFailedResult(phoneNumber, "Invalid phone number");
            }

            log.info("Sending SMS to {} (SMS format: {})", dbFormattedNumber, smsFormattedNumber);

            SmsLog smsLog = buildSmsLog(dbFormattedNumber, message, "PENDING", currentAnnouncement);
            smsLog = saveSmsLogSafely(smsLog);

            String[] recipients = new String[]{smsFormattedNumber};
            List<Recipient> response = smsService.send(message, recipients, true);

            if (response != null && !response.isEmpty()) {
                Recipient recipient = response.get(0);
                boolean success = "Success".equalsIgnoreCase(recipient.status);

                if (success) {
                    updateSmsLog(smsLog, recipient.messageId, "SENT", null);
                    return createSuccessResult(recipient, dbFormattedNumber);
                } else {
                    updateSmsLog(smsLog, recipient.messageId, "FAILED", recipient.status);
                    return createFailedResult(dbFormattedNumber, recipient.status);
                }
            }

            updateSmsLog(smsLog, null, "FAILED", "No response from gateway");
            return createFailedResult(dbFormattedNumber, "No response from SMS gateway");

        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", phoneNumber, e.getMessage(), e);
            logSmsFailure(phoneNumber, message, e.getMessage());
            return createFailedResult(phoneNumber, e.getMessage());
        }
    }

    @Override
    public List<SmsResult> sendBulkSms(List<String> phoneNumbers, String message) {
        List<SmsResult> results = new ArrayList<>();
        List<SmsLog> smsLogs = new ArrayList<>();
        List<String> smsFormattedNumbers = new ArrayList<>();
        List<String> dbFormattedNumbers = new ArrayList<>();

        try {
            initialize();

            log.info("========== SMS DEBUG ==========");
            log.info("Attempting to send bulk SMS to {} recipients", phoneNumbers.size());
            log.info("Message length: {}", message != null ? message.length() : 0);

            for (String phone : phoneNumbers) {
                String dbFormat = formatPhoneNumberForDatabase(phone);
                String smsFormat = formatPhoneNumberForSMS(phone);

                if (dbFormat != null && smsFormat != null) {
                    dbFormattedNumbers.add(dbFormat);
                    smsFormattedNumbers.add(smsFormat);
                    log.info("Formatted {} -> DB: {}, SMS: {}", phone, dbFormat, smsFormat);
                } else {
                    log.warn("Failed to format phone number: {}", phone);
                    results.add(createFailedResult(phone, "Invalid phone number"));
                }
            }

            log.info("Valid numbers after formatting: {} for SMS, {} for DB",
                    smsFormattedNumbers.size(), dbFormattedNumbers.size());

            if (smsFormattedNumbers.isEmpty()) {
                log.warn("No valid phone numbers to send SMS");
                SmsResult summary = SmsResult.builder()
                        .successCount(0)
                        .failedCount((int) results.stream().filter(r -> "FAILED".equals(r.getStatus())).count())
                        .failedNumbers(results.stream()
                                .filter(r -> "FAILED".equals(r.getStatus()))
                                .map(SmsResult::getPhoneNumber)
                                .collect(Collectors.toList()))
                        .successNumbers(new ArrayList<>())
                        .logs(new ArrayList<>())
                        .build();
                results.add(0, summary);
                return results;
            }

            // Build logs only; do not save one-by-one
            for (String dbNumber : dbFormattedNumbers) {
                SmsLog smsLog = buildSmsLog(dbNumber, message, "PENDING", currentAnnouncement);
                smsLogs.add(smsLog);
            }

            // Save once
            if (!smsLogs.isEmpty()) {
                smsLogs = smsLogRepository.saveAll(smsLogs);
                log.info("Saved {} SMS logs to database", smsLogs.size());
            }

            int batchSize = 100;

            for (int i = 0; i < smsFormattedNumbers.size(); i += batchSize) {
                int end = Math.min(i + batchSize, smsFormattedNumbers.size());
                List<String> batchSmsNumbers = smsFormattedNumbers.subList(i, end);
                String[] batch = batchSmsNumbers.toArray(new String[0]);

                log.info("Sending batch of {} messages", batch.length);

                List<Recipient> response = smsService.send(message, batch, true);

                if (response == null || response.isEmpty()) {
                    log.warn("No response returned for SMS batch starting at index {}", i);

                    for (int j = i; j < end; j++) {
                        SmsLog smsLog = smsLogs.get(j);
                        updateSmsLogInMemory(smsLog, null, "FAILED", "No response from gateway");
                        results.add(createFailedResult(dbFormattedNumbers.get(j), "No response from SMS gateway"));
                    }
                    continue;
                }

                log.info("Received response for {} recipients", response.size());

                for (int j = 0; j < batch.length; j++) {
                    int globalIndex = i + j;

                    SmsLog smsLog = smsLogs.get(globalIndex);
                    String dbNumber = dbFormattedNumbers.get(globalIndex);

                    if (j >= response.size()) {
                        updateSmsLogInMemory(smsLog, null, "FAILED", "Missing recipient response");
                        results.add(createFailedResult(dbNumber, "Missing recipient response"));
                        continue;
                    }

                    Recipient recipient = response.get(j);
                    boolean success = "Success".equalsIgnoreCase(recipient.status);

                    log.info("Updating log for {}: status={}, messageId={}",
                            recipient.number, recipient.status, recipient.messageId);

                    if (success) {
                        updateSmsLogInMemory(smsLog, recipient.messageId, "SENT", null);
                        results.add(createSuccessResult(recipient, dbNumber));
                    } else {
                        updateSmsLogInMemory(smsLog, recipient.messageId, "FAILED", recipient.status);
                        results.add(createFailedResult(dbNumber, recipient.status));
                    }
                }
            }

            if (!smsLogs.isEmpty()) {
                smsLogRepository.saveAll(smsLogs);
                log.info("Updated {} SMS logs after provider response", smsLogs.size());
            }

        } catch (Exception e) {
            log.error("Error sending bulk SMS: {}", e.getMessage(), e);

            for (String phone : phoneNumbers) {
                results.add(createFailedResult(phone, e.getMessage()));
            }
        }

        log.info("========== SMS DEBUG END ==========");

        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();

        List<String> failedNumbers = results.stream()
                .filter(r -> "FAILED".equals(r.getStatus()))
                .map(SmsResult::getPhoneNumber)
                .collect(Collectors.toList());

        List<String> successNumbers = results.stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()))
                .map(SmsResult::getPhoneNumber)
                .collect(Collectors.toList());

        SmsResult summary = SmsResult.builder()
                .successCount((int) successCount)
                .failedCount((int) failedCount)
                .failedNumbers(failedNumbers)
                .successNumbers(successNumbers)
                .logs(smsLogs)
                .build();

        results.add(0, summary);
        return results;
    }

    @Override
    public SmsResult sendSmsWithTemplate(String phoneNumber, String templateName, Map<String, String> params) {
        String message = templateService.generateMessage(templateName, params);
        return sendSms(phoneNumber, message);
    }

    @Override
    public boolean validatePhoneNumber(String phoneNumber) {
        return PhoneNumberUtils.validateNigerianPhoneNumber(phoneNumber);
    }

    @Override
    public String formatPhoneNumber(String phoneNumber) {
        return formatPhoneNumberForDatabase(phoneNumber);
    }

    /**
     * Format phone number for DATABASE STORAGE (local format: 09090909090)
     */
    public String formatPhoneNumberForDatabase(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Null or empty phone number provided for database formatting");
            return null;
        }
        return PhoneNumberUtils.formatToLocal(phoneNumber);
    }

    /**
     * Format phone number for AFRICA'S TALKING (international format: +2349090909090)
     */
    private String formatPhoneNumberForSMS(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Null or empty phone number provided for SMS formatting");
            return null;
        }

        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        log.info("Formatting for SMS: {} -> cleaned: {}", phoneNumber, cleaned);

        if (cleaned.isEmpty()) {
            log.warn("Phone number contains no digits: {}", phoneNumber);
            return null;
        }

        if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            cleaned = "234" + cleaned.substring(1);
            log.info("Converted 11-digit local format to: {}", cleaned);
        } else if (cleaned.length() == 10 && !cleaned.startsWith("0")) {
            cleaned = "234" + cleaned;
            log.info("Converted 10-digit format to: {}", cleaned);
        } else if (cleaned.length() == 13 && cleaned.startsWith("234")) {
            log.info("Already in country code format: {}", cleaned);
        } else if (cleaned.length() == 14 && cleaned.startsWith("234")) {
            cleaned = cleaned.substring(0, 13);
            log.info("Trimmed to 13 digits: {}", cleaned);
        } else if (cleaned.length() > 13) {
            cleaned = cleaned.substring(cleaned.length() - 13);
            log.info("Extracted last 13 digits: {}", cleaned);
        } else {
            log.warn("Unexpected phone number format - length: {}, digits: {}", cleaned.length(), cleaned);
            return null;
        }

        if (cleaned.length() != 13) {
            log.warn("Phone number should be 13 digits with country code, got {} digits: {}",
                    cleaned.length(), cleaned);
            return null;
        }

        String formatted = "+" + cleaned;
        log.info("Final formatted for SMS: {}", formatted);
        return formatted;
    }

    // ==================== HELPER METHODS ====================

    private void logSmsFailure(String phoneNumber, String message, String error) {
        log.error("SMS failed to {}: {} - Error: {}", phoneNumber, message, error);

        try {
            String dbFormattedNumber = formatPhoneNumberForDatabase(phoneNumber);

            SmsLog smsLog = buildFailureLog(dbFormattedNumber, message, error, currentAnnouncement);
            saveSmsLogSafely(smsLog);

            log.info("Saved failure log for {}", dbFormattedNumber);
        } catch (Exception e) {
            log.error("Error saving failure log: {}", e.getMessage(), e);
        }
    }

    private SmsLog buildSmsLog(String phoneNumber, String message, String status, Announcement announcement) {
        log.info("Creating SMS log object for phone: {}", phoneNumber);

        SmsLog smsLog = SmsLog.builder()
                .parentPhone(phoneNumber)
                .messageContent(message)
                .messageType(detectMessageType(message))
                .status(status)
                .deliveryStatus(resolveDeliveryStatus(status))
                .sentAt(LocalDateTime.now())
                .retryCount(0)
                .requiresFollowUp(false)
                .announcement(announcement)
                .build();

        enrichLogWithStudentDetails(smsLog, phoneNumber);
        return smsLog;
    }

    private SmsLog buildFailureLog(String phoneNumber, String message, String error, Announcement announcement) {
        SmsLog smsLog = SmsLog.builder()
                .parentPhone(phoneNumber)
                .messageContent(message)
                .messageType(detectMessageType(message))
                .status("FAILED")
                .deliveryStatus(3)
                .errorMessage(error)
                .requiresFollowUp(true)
                .retryCount(0)
                .sentAt(LocalDateTime.now())
                .announcement(announcement)
                .build();

        enrichLogWithStudentDetails(smsLog, phoneNumber);
        return smsLog;
    }

    private void enrichLogWithStudentDetails(SmsLog smsLog, String phoneNumber) {
        try {
            Optional<Student> studentOpt = findStudentByPhone(phoneNumber);

            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                smsLog.setStudentId(student.getId());
                smsLog.setStudentName(student.getFirstName() + " " + student.getLastName());
                smsLog.setStudentClass(
                        student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : null
                );
                smsLog.setParentName(student.getParentName());
                log.info("Found student: {} for phone {}", student.getFirstName(), phoneNumber);
            } else {
                smsLog.setStudentName("Unknown Student");
                smsLog.setParentName("Unknown Parent");
                log.info("No student found for phone: {}, using placeholder", phoneNumber);
            }
        } catch (Exception e) {
            log.error("Error enriching SMS log with student details for {}: {}", phoneNumber, e.getMessage(), e);
            smsLog.setStudentName("Unknown Student");
            smsLog.setParentName("Unknown Parent");
        }
    }

    private SmsLog saveSmsLogSafely(SmsLog smsLog) {
        try {
            SmsLog savedLog = smsLogRepository.save(smsLog);
            log.info("Saved SMS log with ID: {}", savedLog.getId());
            return savedLog;
        } catch (Exception e) {
            log.error("Error saving SMS log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save SMS log", e);
        }
    }

    private void updateSmsLog(SmsLog smsLog, String messageId, String status, String error) {
        if (smsLog == null) {
            return;
        }

        try {
            updateSmsLogInMemory(smsLog, messageId, status, error);
            smsLogRepository.save(smsLog);
            log.info("Updated SMS log ID: {} with status: {}", smsLog.getId(), status);
        } catch (Exception e) {
            log.error("Error updating SMS log: {}", e.getMessage(), e);
        }
    }

    private void updateSmsLogInMemory(SmsLog smsLog, String messageId, String status, String error) {
        if (smsLog == null) {
            return;
        }

        smsLog.setMessageId(messageId);
        smsLog.setStatus(status);

        if ("SENT".equals(status)) {
            smsLog.setDeliveryStatus(1);
            smsLog.setSentAt(LocalDateTime.now());
            smsLog.setErrorMessage(null);
            smsLog.setRequiresFollowUp(false);
        } else if ("DELIVERED".equals(status)) {
            smsLog.setDeliveryStatus(2);
            smsLog.setDeliveredAt(LocalDateTime.now());
            smsLog.setErrorMessage(null);
            smsLog.setRequiresFollowUp(false);
        } else if ("FAILED".equals(status)) {
            smsLog.setDeliveryStatus(3);
            smsLog.setErrorMessage(error);
            smsLog.setRequiresFollowUp(true);
        } else if ("PENDING".equals(status)) {
            smsLog.setDeliveryStatus(0);
        }
    }

    private int resolveDeliveryStatus(String status) {
        if ("PENDING".equals(status)) {
            return 0;
        }
        if ("SENT".equals(status)) {
            return 1;
        }
        if ("DELIVERED".equals(status)) {
            return 2;
        }
        return 3;
    }

    private Optional<Student> findStudentByPhone(String phoneNumber) {
        log.info("Searching for student with phone: {}", phoneNumber);

        String localNumber = PhoneNumberUtils.formatToLocal(phoneNumber);
        log.info("Local format: {}", localNumber);

        if (localNumber == null || localNumber.isBlank()) {
            return Optional.empty();
        }

        List<Student> parentMatches =
                studentRepository.findByParentPhoneOrderByLastNameAscFirstNameAsc(localNumber);

        if (!parentMatches.isEmpty()) {
            log.info("Found {} student(s) by exact parent phone match", parentMatches.size());
            return Optional.of(parentMatches.get(0));
        }

        List<Student> emergencyMatches =
                studentRepository.findByEmergencyContactPhone(localNumber);

        if (!emergencyMatches.isEmpty()) {
            log.info("Found {} student(s) by emergency contact phone match", emergencyMatches.size());
            return Optional.of(emergencyMatches.get(0));
        }

        log.warn("No student found for phone: {} (local: {})", phoneNumber, localNumber);
        return Optional.empty();
    }

    private SmsResult createSuccessResult(Recipient recipient, String phoneNumber) {
        return SmsResult.builder()
                .messageId(recipient.messageId)
                .phoneNumber(phoneNumber)
                .status("SUCCESS")
                .sentAt(LocalDateTime.now())
                .cost(0)
                .build();
    }

    private SmsResult createFailedResult(String phoneNumber, String error) {
        return SmsResult.builder()
                .phoneNumber(phoneNumber)
                .status("FAILED")
                .errorMessage(error)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private String detectMessageType(String message) {
        String lowerMsg = message == null ? "" : message.toLowerCase();

        if (lowerMsg.contains("fee") || lowerMsg.contains("payment") || lowerMsg.contains("₦")) {
            return "FEE_REMINDER";
        }
        if (lowerMsg.contains("resume") || lowerMsg.contains("resumption")) {
            return "RESUMPTION";
        }
        if (lowerMsg.contains("result") || lowerMsg.contains("score") || lowerMsg.contains("grade")) {
            return "RESULT";
        }
        if (lowerMsg.contains("midterm") || lowerMsg.contains("break")) {
            return "MIDTERM_BREAK";
        }
        if (lowerMsg.contains("urgent") || lowerMsg.contains("emergency")) {
            return "EMERGENCY";
        }
        if (lowerMsg.contains("event") || lowerMsg.contains("sports") || lowerMsg.contains("cultural")) {
            return "EVENT";
        }
        return "GENERAL";
    }
}