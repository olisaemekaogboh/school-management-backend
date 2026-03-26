package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.support.SupportWsMessageRequest;
import com.inkFront.schoolManagement.dto.support.SupportWsMessageResponse;
import com.inkFront.schoolManagement.service.SupportRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class SupportRealtimeController {

    private final SupportRealtimeService supportRealtimeService;

    @MessageMapping("/support.send")
    @SendToUser("/queue/support-ack")
    public SupportWsMessageResponse sendMessage(
            Principal principal,
            @Payload SupportWsMessageRequest request
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("Unauthenticated WebSocket session");
        }

        return supportRealtimeService.handleRealtimeMessage(principal.getName(), request);
    }
}