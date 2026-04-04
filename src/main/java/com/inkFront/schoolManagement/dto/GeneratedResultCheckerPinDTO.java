package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResultCheckerPinDTO {

    private Long id;
    private String pin;
    private String pinScope;
    private String targetType;
    private Long studentId;
    private Long classId;
    private String session;
    private Result.Term term;
    private Integer maxUsage;
    private Integer usedCount;
    private Boolean active;
    private LocalDateTime expiresAt;
    private String notes;
    private String createdByName;
    private LocalDateTime createdAt;
}