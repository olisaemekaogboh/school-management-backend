// src/main/java/com/inkFront/schoolManagement/dto/SessionResponseDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.AcademicSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponseDTO {

    private Long id;
    private String sessionName;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SessionResponseDTO fromEntity(AcademicSession session) {
        return SessionResponseDTO.builder()
                .id(session.getId())
                .sessionName(session.getSessionName())
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .active(session.isActive())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}