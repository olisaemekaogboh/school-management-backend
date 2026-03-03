// src/main/java/com/inkFront/schoolManagement/dto/TeacherInvitationDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.TeacherInvitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInvitationDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String token;
    private LocalDateTime expiryDate;
    private boolean used;
    private LocalDateTime createdAt;

    public static TeacherInvitationDTO fromEntity(TeacherInvitation invitation) {
        if (invitation == null) return null;

        return TeacherInvitationDTO.builder()
                .id(invitation.getId())
                .firstName(invitation.getFirstName())
                .lastName(invitation.getLastName())
                .email(invitation.getEmail())
                .phoneNumber(invitation.getPhoneNumber())
                .token(invitation.getToken())
                .expiryDate(invitation.getExpiryDate())
                .used(invitation.isUsed())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}