package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.support.SupportWsMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportWsMessageResponse;

public interface SupportRealtimeService {
    SupportWsMessageResponse handleRealtimeMessage(String principalName, SupportWsMessageRequest request);
}