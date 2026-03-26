package com.inkFront.schoolManagement.dto.support;

import com.inkFront.schoolManagement.model.SupportMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private boolean fromAdmin;
    private String message;
    private LocalDateTime createdAt;

    public static SupportMessageDTO fromEntity(SupportMessage entity) {
        String firstName = entity.getSender().getFirstName() != null ? entity.getSender().getFirstName() : "";
        String lastName = entity.getSender().getLastName() != null ? entity.getSender().getLastName() : "";

        return SupportMessageDTO.builder()
                .id(entity.getId())
                .senderId(entity.getSender().getId())
                .senderName((firstName + " " + lastName).trim())
                .senderRole(entity.getSender().getRole().name())
                .fromAdmin(entity.isFromAdmin())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}