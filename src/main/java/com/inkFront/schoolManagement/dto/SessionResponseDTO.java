package com.inkFront.schoolManagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inkFront.schoolManagement.model.AcademicSession;
import com.inkFront.schoolManagement.model.Term;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDTO {

    private Long id;

    @JsonProperty("session")
    private String sessionName;

    private LocalDate startDate;
    private LocalDate endDate;
    private Term currentTerm;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SessionResponseDTO fromEntity(AcademicSession entity) {
        return new SessionResponseDTO(
                entity.getId(),
                entity.getSessionName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCurrentTerm(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}