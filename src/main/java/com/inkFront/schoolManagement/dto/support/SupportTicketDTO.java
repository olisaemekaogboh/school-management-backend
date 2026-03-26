package com.inkFront.schoolManagement.dto.support;

import com.inkFront.schoolManagement.model.SupportTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketDTO {
    private Long id;
    private String ticketNumber;
    private String subject;
    private String category;
    private String status;
    private Long createdByUserId;
    private String createdByName;
    private String createdByRole;
    private Long assignedAdminUserId;
    private String assignedAdminName;
    private boolean requesterUnread;
    private boolean adminUnread;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SupportMessageDTO> messages;

    public static SupportTicketDTO fromEntity(SupportTicket entity, List<SupportMessageDTO> messages) {
        String creatorFirst = entity.getCreatedBy() != null && entity.getCreatedBy().getFirstName() != null
                ? entity.getCreatedBy().getFirstName() : "";
        String creatorLast = entity.getCreatedBy() != null && entity.getCreatedBy().getLastName() != null
                ? entity.getCreatedBy().getLastName() : "";

        String adminName = null;
        if (entity.getAssignedAdmin() != null) {
            String adminFirst = entity.getAssignedAdmin().getFirstName() != null
                    ? entity.getAssignedAdmin().getFirstName() : "";
            String adminLast = entity.getAssignedAdmin().getLastName() != null
                    ? entity.getAssignedAdmin().getLastName() : "";
            adminName = (adminFirst + " " + adminLast).trim();
        }

        return SupportTicketDTO.builder()
                .id(entity.getId())
                .ticketNumber(entity.getTicketNumber())
                .subject(entity.getSubject())
                .category(entity.getCategory())
                .status(entity.getStatus().name())
                .createdByUserId(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdByName((creatorFirst + " " + creatorLast).trim())
                .createdByRole(entity.getCreatedBy() != null ? entity.getCreatedBy().getRole().name() : null)
                .assignedAdminUserId(entity.getAssignedAdmin() != null ? entity.getAssignedAdmin().getId() : null)
                .assignedAdminName(adminName)
                .requesterUnread(entity.isRequesterUnread())
                .adminUnread(entity.isAdminUnread())
                .lastMessageAt(entity.getLastMessageAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .messages(messages)
                .build();
    }

    public static SupportTicketDTO fromEntity(SupportTicket entity) {
        return fromEntity(entity, List.of());
    }
}