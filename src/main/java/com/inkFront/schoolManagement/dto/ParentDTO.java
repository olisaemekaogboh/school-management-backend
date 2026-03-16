package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class ParentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private String occupation;
    private String companyName;
    private String officeAddress;
    private Parent.Relationship relationship;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Ward IDs only
    private List<Long> wardIds = new ArrayList<>();

    // Safe simplified ward info
    private List<WardInfoDTO> wards = new ArrayList<>();

    public static ParentDTO fromParent(Parent parent) {
        ParentDTO dto = new ParentDTO();

        dto.setId(parent.getId());
        dto.setFirstName(parent.getFirstName());
        dto.setLastName(parent.getLastName());
        dto.setMiddleName(parent.getMiddleName());
        dto.setFullName(buildFullName(
                parent.getFirstName(),
                parent.getMiddleName(),
                parent.getLastName()
        ));
        dto.setEmail(parent.getEmail());
        dto.setPhoneNumber(parent.getPhoneNumber());
        dto.setAlternatePhone(parent.getAlternatePhone());
        dto.setAddress(parent.getAddress());
        dto.setOccupation(parent.getOccupation());
        dto.setCompanyName(parent.getCompanyName());
        dto.setOfficeAddress(parent.getOfficeAddress());
        dto.setRelationship(parent.getRelationship());
        dto.setEmergencyContactName(parent.getEmergencyContactName());
        dto.setEmergencyContactPhone(parent.getEmergencyContactPhone());
        dto.setEmergencyContactRelationship(parent.getEmergencyContactRelationship());
        dto.setProfilePictureUrl(parent.getProfilePictureUrl());
        dto.setCreatedAt(parent.getCreatedAt());
        dto.setUpdatedAt(parent.getUpdatedAt());

        if (parent.getWards() != null) {
            dto.setWardIds(parent.getWards().stream()
                    .map(Student::getId)
                    .collect(Collectors.toList()));

            dto.setWards(parent.getWards().stream()
                    .map(WardInfoDTO::fromStudent)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private static String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder sb = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }

        return sb.toString().trim();
    }

    @Data
    @NoArgsConstructor
    public static class WardInfoDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String middleName;
        private String fullName;
        private String admissionNumber;
        private String studentClass;
        private String classArm;
        private String status;
        private String profilePictureUrl;

        public static WardInfoDTO fromStudent(Student student) {
            WardInfoDTO dto = new WardInfoDTO();
            dto.setId(student.getId());
            dto.setFirstName(student.getFirstName());
            dto.setLastName(student.getLastName());
            dto.setMiddleName(student.getMiddleName());
            dto.setFullName(buildStudentFullName(student));
            dto.setAdmissionNumber(student.getAdmissionNumber());
            dto.setStudentClass(student.getStudentClass());
            dto.setClassArm(student.getClassArm());
            dto.setStatus(student.getStatus() != null ? student.getStatus().name() : null);
            dto.setProfilePictureUrl(student.getProfilePictureUrl());
            return dto;
        }

        private static String buildStudentFullName(Student student) {
            StringBuilder sb = new StringBuilder();

            if (student.getFirstName() != null && !student.getFirstName().isBlank()) {
                sb.append(student.getFirstName().trim());
            }
            if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(student.getMiddleName().trim());
            }
            if (student.getLastName() != null && !student.getLastName().isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(student.getLastName().trim());
            }

            return sb.toString().trim();
        }
    }
}