// src/main/java/com/inkFront/schoolManagement/service/SmsTemplateService.java
package com.inkFront.schoolManagement.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SmsTemplateService {

    public String generateMessage(String templateName, Map<String, String> params) {
        switch (templateName) {
            case "resumption":
                return generateResumptionMessage(params);
            case "result":
                return generateResultMessage(params);
            case "fee_reminder":
                return generateFeeReminderMessage(params);
            case "fee_overdue":
                return generateFeeOverdueMessage(params);
            case "fee_paid":
                return generateFeePaidMessage(params);
            case "event":
                return generateEventMessage(params);
            case "attendance":
                return generateAttendanceMessage(params);
            case "emergency":
                return generateEmergencyMessage(params);
            default:
                return generateGeneralMessage(params);
        }
    }

    private String generateResumptionMessage(Map<String, String> params) {
        return String.format(
                "Dear Parent, School resumes on %s for %s term %s. Please ensure your child is prepared.",
                params.getOrDefault("date", "N/A"),
                params.getOrDefault("term", "N/A"),
                params.getOrDefault("session", "N/A")
        );
    }

    private String generateResultMessage(Map<String, String> params) {
        return String.format(
                "Dear Parent, %s term results for %s are now available on the portal. Your child's average: %s%%",
                params.getOrDefault("term", "N/A"),
                params.getOrDefault("session", "N/A"),
                params.getOrDefault("average", "0")
        );
    }

    private String generateFeeReminderMessage(Map<String, String> params) {
        return String.format(
                "Dear Parent, this is a reminder that school fees of ₦%s for %s term %s for %s is due on %s. " +
                        "Current balance: ₦%s. Please make payment to avoid penalties. Thank you.",
                params.getOrDefault("amount", "0"),
                params.getOrDefault("term", "N/A"),
                params.getOrDefault("session", "N/A"),
                params.getOrDefault("studentName", "your ward"),
                params.getOrDefault("dueDate", "N/A"),
                params.getOrDefault("balance", "0")
        );
    }

    private String generateFeeOverdueMessage(Map<String, String> params) {
        return String.format(
                "URGENT: Your fee payment of ₦%s for %s term %s for %s is %s days overdue. " +
                        "Current balance: ₦%s. Please make immediate payment to avoid further action. " +
                        "Contact the school if you have any questions.",
                params.getOrDefault("amount", "0"),
                params.getOrDefault("term", "N/A"),
                params.getOrDefault("session", "N/A"),
                params.getOrDefault("studentName", "your ward"),
                params.getOrDefault("daysOverdue", "0"),
                params.getOrDefault("balance", "0")
        );
    }

    private String generateFeePaidMessage(Map<String, String> params) {
        return String.format(
                "Dear Parent, thank you for your payment of ₦%s for %s term %s for %s. " +
                        "Your current balance is ₦%s. Receipt: %s",
                params.getOrDefault("amount", "0"),
                params.getOrDefault("term", "N/A"),
                params.getOrDefault("session", "N/A"),
                params.getOrDefault("studentName", "your ward"),
                params.getOrDefault("balance", "0"),
                params.getOrDefault("reference", "N/A")
        );
    }

    private String generateEventMessage(Map<String, String> params) {
        return String.format(
                "Dear Parent, %s will be held on %s at %s. %s",
                params.getOrDefault("event", "An event"),
                params.getOrDefault("date", "N/A"),
                params.getOrDefault("time", "N/A"),
                params.getOrDefault("location", "")
        );
    }

    private String generateAttendanceMessage(Map<String, String> params) {
        return String.format(
                "Dear Parent, your ward %s was marked %s on %s. Reason: %s",
                params.getOrDefault("studentName", "your ward"),
                params.getOrDefault("status", "absent"),
                params.getOrDefault("date", "N/A"),
                params.getOrDefault("reason", "No reason provided")
        );
    }

    private String generateEmergencyMessage(Map<String, String> params) {
        return String.format(
                "URGENT: %s. Please contact the school immediately.",
                params.getOrDefault("message", "Emergency situation")
        );
    }

    private String generateGeneralMessage(Map<String, String> params) {
        return params.getOrDefault("message",
                "School announcement. Please check the portal for more details.");
    }
}