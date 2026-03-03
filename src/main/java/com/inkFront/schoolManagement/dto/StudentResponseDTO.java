// src/main/java/com/inkFront/schoolManagement/dto/StudentResponseDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDTO {

    private Long id;

    // Personal Information
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String religion;
    private String nationality;

    // Academic Information
    private String admissionNumber;
    private String studentClass;
    private String classArm;
    private String status;
    private LocalDate admissionDate;
    private String previousSchool;

    // Parent/Guardian Information
    private String parentName;
    private String parentPhone;
    private String parentEmail;

    // Address Information
    private String address;
    private String localGovtArea;
    private String stateOfOrigin;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Promotion fields
    private boolean excludeFromPromotion;
    private String promotionHoldReason;

    // Profile picture
    private String profilePictureUrl;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StudentResponseDTO fromStudent(Student student) {
        if (student == null) {
            return null;
        }

        String fullName = student.getFirstName() + " " +
                (student.getMiddleName() != null ? student.getMiddleName() + " " : "") +
                student.getLastName();

        return StudentResponseDTO.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .middleName(student.getMiddleName())
                .fullName(fullName)
                .gender(student.getGender() != null ? student.getGender().toString() : null)
                .dateOfBirth(student.getDateOfBirth())
                .religion(student.getReligion())
                .nationality(student.getNationality())
                .admissionNumber(student.getAdmissionNumber())
                .studentClass(student.getStudentClass())
                .classArm(student.getClassArm())
                .status(student.getStatus() != null ? student.getStatus().toString() : null)
                .admissionDate(student.getAdmissionDate())
                .previousSchool(student.getPreviousSchool())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .parentEmail(student.getParentEmail())
                .address(student.getAddress())
                .localGovtArea(student.getLocalGovtArea())
                .stateOfOrigin(student.getStateOfOrigin())
                .emergencyContactName(student.getEmergencyContactName())
                .emergencyContactPhone(student.getEmergencyContactPhone())
                .emergencyContactRelationship(student.getEmergencyContactRelationship())
                .excludeFromPromotion(student.isExcludeFromPromotion())
                .promotionHoldReason(student.getPromotionHoldReason())
                .profilePictureUrl(student.getProfilePictureUrl())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}