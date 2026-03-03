// src/main/java/com/inkFront/schoolManagement/service/SmsService.java
package com.inkFront.schoolManagement.service;

import java.util.List;
import java.util.Map;

public interface SmsService {

    com.inkFront.schoolManagement.service.SmsResult sendSms(String phoneNumber, String message);

    List<com.inkFront.schoolManagement.service.SmsResult> sendBulkSms(List<String> phoneNumbers, String message);

    com.inkFront.schoolManagement.service.SmsResult sendSmsWithTemplate(String phoneNumber, String templateName, Map<String, String> params);

    boolean validatePhoneNumber(String phoneNumber);

    String formatPhoneNumber(String phoneNumber);
}