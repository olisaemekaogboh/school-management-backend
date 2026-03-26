package com.inkFront.schoolManagement.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportWsMessageResponse {
    private Long ticketId;
    private Long messageId;
    private String senderName;
    private String senderRole;
    private boolean fromAdmin;
    private String message;
    private String ticketStatus;
    private LocalDateTime createdAt;
}