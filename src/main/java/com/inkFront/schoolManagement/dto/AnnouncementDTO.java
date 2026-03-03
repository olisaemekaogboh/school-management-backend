// src/main/java/com/inkFront/schoolManagement/dto/AnnouncementDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Announcement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDTO {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 2000, message = "Content must be between 10 and 2000 characters")
    private String content;

    @NotNull(message = "Type is required")
    private Announcement.AnnouncementType type;

    @NotNull(message = "Priority is required")
    private Announcement.AnnouncementPriority priority;

    @NotEmpty(message = "At least one audience must be selected")
    private List<Announcement.Audience> audience;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active;

    private String createdBy;

    // Event specific fields
    private LocalDate eventDate;
    private String eventLocation;
    private String eventTime;

    // Fee specific fields
    private Double feeAmount;
    private String feeDescription;
    private LocalDate feeDueDate;

    // Result specific fields
    private LocalDate resultReleaseDate;
    private String term;
    private String session;

    // Attachment
    private String attachmentUrl;
    private String attachmentName;

    public static AnnouncementDTO fromAnnouncement(Announcement announcement) {
        return AnnouncementDTO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .audience(announcement.getAudience())
                .startDate(announcement.getStartDate())
                .endDate(announcement.getEndDate())
                .active(announcement.isActive())
                .createdBy(announcement.getCreatedBy())
                .eventDate(announcement.getEventDate())
                .eventLocation(announcement.getEventLocation())
                .eventTime(announcement.getEventTime())
                .feeAmount(announcement.getFeeAmount())
                .feeDescription(announcement.getFeeDescription())
                .feeDueDate(announcement.getFeeDueDate())
                .resultReleaseDate(announcement.getResultReleaseDate())
                .term(announcement.getTerm())
                .session(announcement.getSession())
                .attachmentUrl(announcement.getAttachmentUrl())
                .attachmentName(announcement.getAttachmentName())
                .build();
    }
}