// src/main/java/com/inkFront/schoolManagement/config/SmsConfig.java
package com.inkFront.schoolManagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sms")
@Data
public class SmsConfig {
    private String provider; // twilio, africastalking, smsgateway
    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String apiKey;
    private String username;
    private String apiUrl;
    private boolean enabled = true;
}