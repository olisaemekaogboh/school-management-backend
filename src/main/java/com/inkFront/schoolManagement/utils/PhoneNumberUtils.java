// src/main/java/com/inkFront/schoolManagement/utils/PhoneNumberUtils.java
package com.inkFront.schoolManagement.utils;

import java.util.regex.Pattern;

public class PhoneNumberUtils {

    private static final Pattern NIGERIAN_PHONE_PATTERN =
            Pattern.compile("^(0|234)?[789][01]\\d{8}$");

    /**
     * Validate Nigerian phone number (accepts 09090909090 format)
     */
    public static boolean validateNigerianPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Remove any non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // Check if it's a valid Nigerian number (11 digits starting with 0)
        if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            return true;
        }

        // Check if it's a valid Nigerian number with country code
        if (cleaned.length() == 13 && cleaned.startsWith("234")) {
            return true;
        }

        return false;
    }

    /**
     * Format phone number to Nigerian local format (09090909090)
     */
    public static String formatToLocal(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // If it's already in local format (11 digits starting with 0)
        if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            return cleaned;
        }

        // If it's in international format (+2349090909090 or 2349090909090)
        if (cleaned.length() == 13 && cleaned.startsWith("234")) {
            return "0" + cleaned.substring(3);
        }

        // If it's 10 digits (9090909090), add leading 0
        if (cleaned.length() == 10) {
            return "0" + cleaned;
        }

        // Return as is if we can't format
        return cleaned;
    }

    /**
     * Format phone number to international format (+2349090909090)
     */
    public static String formatToInternational(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        String local = formatToLocal(phoneNumber);
        if (local != null && local.startsWith("0")) {
            return "+234" + local.substring(1);
        }

        return phoneNumber;
    }

    /**
     * Extract phone numbers from comma-separated list
     */
    public static String[] extractPhoneNumbers(String phoneList) {
        if (phoneList == null || phoneList.trim().isEmpty()) {
            return new String[0];
        }
        return phoneList.split("[,\\s]+");
    }
}