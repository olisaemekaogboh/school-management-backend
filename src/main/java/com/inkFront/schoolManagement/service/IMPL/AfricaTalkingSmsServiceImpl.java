// src/main/java/com/inkFront/schoolManagement/service/IMPL/AfricaTalkingSmsServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

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
import com.africastalking.AfricasTalking;
import com.africastalking.sms.Recipient;
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

    // Field to store the current announcement for SMS logging
    private Announcement currentAnnouncement;

    private void initialize() {
        if (!initialized) {
            try {
                AfricasTalking.initialize(smsConfig.getUsername(), smsConfig.getApiKey());
                smsService = AfricasTalking.getService(com.africastalking.SmsService.class);
                initialized = true;
                log.info("Africa's Talking SMS service initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Africa's Talking SMS service: {}", e.getMessage());
                throw new RuntimeException("SMS service initialization failed", e);
            }
        }
    }

    public void setCurrentAnnouncement(Announcement announcement) {
        this.currentAnnouncement = announcement;
    }

    @Override
    public SmsResult sendSms(String phoneNumber, String message) {
        SmsLog smsLog = null;
        try {
            initialize();

            // Format for database storage (local format)
            String dbFormattedNumber = formatPhoneNumberForDatabase(phoneNumber);

            // Format for SMS sending (international format with +)
            String smsFormattedNumber = formatPhoneNumberForSMS(phoneNumber);

            if (smsFormattedNumber == null || dbFormattedNumber == null) {
                logSmsFailure(phoneNumber, message, "Invalid phone number");
                return createFailedResult(phoneNumber, "Invalid phone number");
            }

            log.info("Sending SMS to {} (SMS format: {})", dbFormattedNumber, smsFormattedNumber);

            // Create log entry with database format
            smsLog = createSmsLog(dbFormattedNumber, message, "PENDING", currentAnnouncement);

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
            log.error("Error sending SMS to {}: {}", phoneNumber, e.getMessage());
            if (smsLog != null) {
                updateSmsLog(smsLog, null, "FAILED", e.getMessage());
            } else {
                logSmsFailure(phoneNumber, message, e.getMessage());
            }
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
            log.info("Message length: {}", message.length());

            // Format numbers for both purposes
            for (String phone : phoneNumbers) {
                String dbFormat = formatPhoneNumberForDatabase(phone);
                String smsFormat = formatPhoneNumberForSMS(phone);

                if (dbFormat != null && smsFormat != null) {
                    dbFormattedNumbers.add(dbFormat);
                    smsFormattedNumbers.add(smsFormat);
                    log.info("Formatted {} -> DB: {}, SMS: {}", phone, dbFormat, smsFormat);
                } else {
                    log.warn("Failed to format phone number: {}", phone);
                }
            }

            log.info("Valid numbers after formatting: {} for SMS, {} for DB",
                    smsFormattedNumbers.size(), dbFormattedNumbers.size());

            if (smsFormattedNumbers.isEmpty()) {
                log.warn("No valid phone numbers to send SMS");
                return results;
            }

            // Create logs for all recipients using database format
            for (String dbNumber : dbFormattedNumbers) {
                log.info("Creating SMS log for number (db format): {}", dbNumber);
                SmsLog smsLog = createSmsLog(dbNumber, message, "PENDING", currentAnnouncement);
                if (smsLog != null) {
                    smsLogs.add(smsLog);
                    log.info("Created log with ID: {} for announcement: {}",
                            smsLog.getId(),
                            currentAnnouncement != null ? currentAnnouncement.getId() : "null");
                } else {
                    log.error("Failed to create SMS log for {}", dbNumber);
                }
            }

            log.info("Created {} SMS logs", smsLogs.size());

            // Save logs immediately
            if (!smsLogs.isEmpty()) {
                List<SmsLog> savedLogs = smsLogRepository.saveAll(smsLogs);
                log.info("Saved {} logs to database", savedLogs.size());
                smsLogs = savedLogs; // Use the saved versions with IDs
            }

            // Send SMS using the SMS-formatted numbers (with +)
            int batchSize = 100;
            for (int i = 0; i < smsFormattedNumbers.size(); i += batchSize) {
                int end = Math.min(i + batchSize, smsFormattedNumbers.size());
                List<String> batchList = smsFormattedNumbers.subList(i, end);
                String[] batch = batchList.toArray(new String[0]);

                log.info("Sending batch of {} messages", batch.length);
                List<Recipient> response = smsService.send(message, batch, true);

                if (response != null) {
                    log.info("Received response for {} recipients", response.size());
                    for (int j = 0; j < response.size(); j++) {
                        Recipient recipient = response.get(j);
                        if (i + j < smsLogs.size()) {
                            SmsLog smsLog = smsLogs.get(i + j);
                            boolean success = "Success".equalsIgnoreCase(recipient.status);

                            log.info("Updating log for {}: status={}, messageId={}",
                                    recipient.number, recipient.status, recipient.messageId);

                            if (success) {
                                updateSmsLog(smsLog, recipient.messageId, "SENT", null);
                                results.add(createSuccessResult(recipient, dbFormattedNumbers.get(i + j)));
                            } else {
                                updateSmsLog(smsLog, recipient.messageId, "FAILED", recipient.status);
                                results.add(createFailedResult(dbFormattedNumbers.get(i + j), recipient.status));
                            }
                        }
                    }
                }
            }

            // Final save of updated logs
            if (!smsLogs.isEmpty()) {
                List<SmsLog> finalSavedLogs = smsLogRepository.saveAll(smsLogs);
                log.info("Final save: updated {} logs in database", finalSavedLogs.size());
            }

            // Force flush to ensure all changes are committed
            smsLogRepository.flush();

        } catch (Exception e) {
            log.error("Error sending bulk SMS: {}", e.getMessage(), e);
            for (String phone : phoneNumbers) {
                results.add(createFailedResult(phone, e.getMessage()));
            }
        }

        log.info("========== SMS DEBUG END ==========");

        // Add statistics
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
        // Keep the original method for backward compatibility
        // This now calls the database formatter
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

        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        log.info("Formatting for SMS: {} -> cleaned: {}", phoneNumber, cleaned);

        if (cleaned.isEmpty()) {
            log.warn("Phone number contains no digits: {}", phoneNumber);
            return null;
        }

        // Handle Nigerian phone numbers for Africa's Talking
        if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            // Format: 09090909090 -> 2349090909090
            cleaned = "234" + cleaned.substring(1);
            log.info("Converted 11-digit local format to: {}", cleaned);
        } else if (cleaned.length() == 10 && !cleaned.startsWith("0")) {
            // Format: 8090909090 -> 2348090909090
            cleaned = "234" + cleaned;
            log.info("Converted 10-digit format to: {}", cleaned);
        } else if (cleaned.length() == 13 && cleaned.startsWith("234")) {
            // Already has country code without +
            log.info("Already in country code format: {}", cleaned);
        } else if (cleaned.length() == 14 && cleaned.startsWith("234")) {
            // Handle case with extra digit
            cleaned = cleaned.substring(0, 13);
            log.info("Trimmed to 13 digits: {}", cleaned);
        } else if (cleaned.length() > 13) {
            // Too many digits, extract last 13
            cleaned = cleaned.substring(cleaned.length() - 13);
            log.info("Extracted last 13 digits: {}", cleaned);
        } else {
            log.warn("Unexpected phone number format - length: {}, digits: {}", cleaned.length(), cleaned);
            return null;
        }

        // Ensure we have exactly 13 digits for Nigerian numbers
        if (cleaned.length() != 13) {
            log.warn("Phone number should be 13 digits with country code, got {} digits: {}",
                    cleaned.length(), cleaned);
            return null;
        }

        // Add the plus sign for Africa's Talking
        String formatted = "+" + cleaned;
        log.info("Final formatted for SMS: {}", formatted);

        return formatted;
    }

    // ==================== HELPER METHODS ====================

    private void logSmsFailure(String phoneNumber, String message, String error) {
        log.error("SMS failed to {}: {} - Error: {}", phoneNumber, message, error);

        try {
            String dbFormattedNumber = formatPhoneNumberForDatabase(phoneNumber);

            SmsLog smsLog = SmsLog.builder()
                    .parentPhone(dbFormattedNumber)
                    .messageContent(message.length() > 500 ? message.substring(0, 500) : message)
                    .messageType(detectMessageType(message))
                    .status("FAILED")
                    .deliveryStatus(3)
                    .errorMessage(error)
                    .requiresFollowUp(true)
                    .retryCount(0)
                    .sentAt(LocalDateTime.now())
                    .announcement(currentAnnouncement)
                    .build();

            // Try to find student by phone number using database format
            Optional<Student> studentOpt = findStudentByPhone(dbFormattedNumber);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                smsLog.setStudentId(student.getId());
                smsLog.setStudentName(student.getFirstName() + " " + student.getLastName());
                smsLog.setStudentClass(student.getStudentClass());
                smsLog.setParentName(student.getParentName());
            } else {
                smsLog.setStudentName("Unknown Student");
                smsLog.setParentName("Unknown Parent");
            }

            smsLogRepository.save(smsLog);
            log.info("Saved failure log for {}", dbFormattedNumber);

        } catch (Exception e) {
            log.error("Error saving failure log: {}", e.getMessage());
        }
    }

    private SmsLog createSmsLog(String phoneNumber, String message, String status, Announcement announcement) {
        try {
            log.info("Creating SMS log for phone: {}", phoneNumber);

            SmsLog smsLog = SmsLog.builder()
                    .parentPhone(phoneNumber)
                    .messageContent(message.length() > 500 ? message.substring(0, 500) : message)
                    .messageType(detectMessageType(message))
                    .status(status)
                    .deliveryStatus(status.equals("PENDING") ? 0 : status.equals("SENT") ? 1 : 3)
                    .sentAt(LocalDateTime.now())
                    .retryCount(0)
                    .requiresFollowUp(false)
                    .announcement(announcement)
                    .build();

            // Try to find student by phone number
            Optional<Student> studentOpt = findStudentByPhone(phoneNumber);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                smsLog.setStudentId(student.getId());
                smsLog.setStudentName(student.getFirstName() + " " + student.getLastName());
                smsLog.setStudentClass(student.getStudentClass());
                smsLog.setParentName(student.getParentName());
                log.info("Found student: {} for phone {}", student.getFirstName(), phoneNumber);
            } else {
                log.info("No student found for phone: {}, using placeholder", phoneNumber);
                smsLog.setStudentName("Unknown Student");
                smsLog.setParentName("Unknown Parent");
            }

            SmsLog savedLog = smsLogRepository.save(smsLog);
            log.info("Saved SMS log with ID: {} for announcement: {} with student: {}",
                    savedLog.getId(),
                    announcement != null ? announcement.getId() : "null",
                    savedLog.getStudentName());
            return savedLog;

        } catch (Exception e) {
            log.error("Error creating SMS log: {}", e.getMessage(), e);
            return null;
        }
    }

    private void updateSmsLog(SmsLog smsLog, String messageId, String status, String error) {
        if (smsLog == null) return;

        try {
            smsLog.setMessageId(messageId);
            smsLog.setStatus(status);

            if ("SENT".equals(status)) {
                smsLog.setDeliveryStatus(1);
                smsLog.setSentAt(LocalDateTime.now());
            } else if ("DELIVERED".equals(status)) {
                smsLog.setDeliveryStatus(2);
                smsLog.setDeliveredAt(LocalDateTime.now());
            } else if ("FAILED".equals(status)) {
                smsLog.setDeliveryStatus(3);
                smsLog.setErrorMessage(error);
                smsLog.setRequiresFollowUp(true);
            }

            smsLogRepository.save(smsLog);
            log.info("Updated SMS log ID: {} with status: {}", smsLog.getId(), status);

        } catch (Exception e) {
            log.error("Error updating SMS log: {}", e.getMessage(), e);
        }
    }

    private Optional<Student> findStudentByPhone(String phoneNumber) {
        log.info("Searching for student with phone: {}", phoneNumber);

        // Format to local for database comparison
        String localNumber = PhoneNumberUtils.formatToLocal(phoneNumber);
        log.info("Local format: {}", localNumber);

        // Try exact match with parent phone
        List<Student> students = studentRepository.findByParentPhone(localNumber);
        if (!students.isEmpty()) {
            log.info("Found {} student(s) by exact parent phone match", students.size());
            return Optional.of(students.get(0));
        }

        // Try emergency contact
        Optional<Student> emergencyMatch = studentRepository.findAll().stream()
                .filter(s -> localNumber.equals(s.getEmergencyContactPhone()))
                .findFirst();

        if (emergencyMatch.isPresent()) {
            log.info("Found student by emergency contact match");
            return emergencyMatch;
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
        String lowerMsg = message.toLowerCase();
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