// src/main/java/com/inkFront/schoolManagement/dto/ParentDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private String occupation;
    private String companyName;
    private String officeAddress;
    private String relationship;
    private List<Long> wardIds;
    private List<String> wardNames;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Convert Entity to DTO
    public static ParentDTO fromEntity(Parent parent) {
        if (parent == null) return null;

        ParentDTO dto = new ParentDTO();
        dto.setId(parent.getId());
        dto.setFirstName(parent.getFirstName());
        dto.setLastName(parent.getLastName());
        dto.setMiddleName(parent.getMiddleName());
        dto.setEmail(parent.getEmail());
        dto.setPhoneNumber(parent.getPhoneNumber());
        dto.setAlternatePhone(parent.getAlternatePhone());
        dto.setAddress(parent.getAddress());
        dto.setOccupation(parent.getOccupation());
        dto.setCompanyName(parent.getCompanyName());
        dto.setOfficeAddress(parent.getOfficeAddress());
        dto.setRelationship(parent.getRelationship() != null ? parent.getRelationship().name() : null);

        if (parent.getWards() != null && !parent.getWards().isEmpty()) {
            dto.setWardIds(parent.getWards().stream()
                    .map(Student::getId)
                    .collect(Collectors.toList()));
            dto.setWardNames(parent.getWards().stream()
                    .map(s -> s.getFirstName() + " " + s.getLastName())
                    .collect(Collectors.toList()));
        }

        dto.setEmergencyContactName(parent.getEmergencyContactName());
        dto.setEmergencyContactPhone(parent.getEmergencyContactPhone());
        dto.setEmergencyContactRelationship(parent.getEmergencyContactRelationship());
        dto.setProfilePictureUrl(parent.getProfilePictureUrl());
        dto.setCreatedAt(parent.getCreatedAt());
        dto.setUpdatedAt(parent.getUpdatedAt());

        return dto;
    }

    // Alias for fromEntity (to match the method call in PublicController)
    public static ParentDTO fromParent(Parent parent) {
        return fromEntity(parent);
    }

    // Convert DTO to Entity
    public static Parent toEntity(ParentDTO dto) {
        if (dto == null) return null;

        Parent parent = new Parent();
        parent.setId(dto.getId());
        parent.setFirstName(dto.getFirstName());
        parent.setLastName(dto.getLastName());
        parent.setMiddleName(dto.getMiddleName());
        parent.setEmail(dto.getEmail());
        parent.setPhoneNumber(dto.getPhoneNumber());
        parent.setAlternatePhone(dto.getAlternatePhone());
        parent.setAddress(dto.getAddress());
        parent.setOccupation(dto.getOccupation());
        parent.setCompanyName(dto.getCompanyName());
        parent.setOfficeAddress(dto.getOfficeAddress());

        if (dto.getRelationship() != null) {
            parent.setRelationship(Parent.Relationship.valueOf(dto.getRelationship()));
        }

        parent.setEmergencyContactName(dto.getEmergencyContactName());
        parent.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        parent.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        parent.setProfilePictureUrl(dto.getProfilePictureUrl());

        return parent;
    }
}