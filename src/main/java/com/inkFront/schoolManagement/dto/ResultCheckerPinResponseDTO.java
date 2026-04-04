package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.ResultCheckerPin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultCheckerPinResponseDTO {

    private Long id;
    private String pinScope;
    private String targetType;
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;
    private String classArm;
    private String session;
    private Result.Term term;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer remainingUsage;
    private Boolean active;
    private LocalDateTime expiresAt;
    private String notes;
    private String createdByName;
    private LocalDateTime createdAt;

    public static ResultCheckerPinResponseDTO fromEntity(ResultCheckerPin pin) {
        String studentName = null;
        if (pin.getStudent() != null) {
            String first = pin.getStudent().getFirstName() == null ? "" : pin.getStudent().getFirstName().trim();
            String middle = pin.getStudent().getMiddleName() == null ? "" : pin.getStudent().getMiddleName().trim();
            String last = pin.getStudent().getLastName() == null ? "" : pin.getStudent().getLastName().trim();
            studentName = (first + " " + middle + " " + last).replaceAll("\\s+", " ").trim();
        }

        return new ResultCheckerPinResponseDTO(
                pin.getId(),
                pin.getPinScope() != null ? pin.getPinScope().name() : null,
                pin.getTargetType() != null ? pin.getTargetType().name() : null,
                pin.getStudent() != null ? pin.getStudent().getId() : null,
                studentName,
                pin.getSchoolClass() != null ? pin.getSchoolClass().getId() : null,
                pin.getSchoolClass() != null ? pin.getSchoolClass().getClassName() : null,
                pin.getSchoolClass() != null ? pin.getSchoolClass().getArm() : null,
                pin.getSession(),
                pin.getTerm(),
                pin.getMaxUsage(),
                pin.getUsedCount(),
                Math.max((pin.getMaxUsage() != null ? pin.getMaxUsage() : 0) - (pin.getUsedCount() != null ? pin.getUsedCount() : 0), 0),
                pin.isActive(),
                pin.getExpiresAt(),
                pin.getNotes(),
                pin.getCreatedByName(),
                pin.getCreatedAt()
        );
    }
}