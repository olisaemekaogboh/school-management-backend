// src/main/java/com/inkFront/schoolManagement/dto/StudentRequestDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Student;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestDTO {

    // Personal Information
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    private String middleName;

    @NotNull(message = "Gender is required")
    private Student.Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String religion;
    private String nationality;

    // Academic Information
    @NotNull(message = "Class is required")
    private Long classId;

    private Student.StudentStatus status;
    private String previousSchool;

    // Parent/Guardian Information
    @NotBlank(message = "Parent name is required")
    private String parentName;

    @Pattern(regexp = "^[0-9]{11}$", message = "Phone number must be 11 digits")
    private String parentPhone;

    @Email(message = "Invalid email format")
    private String parentEmail;

    // Address Information
    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Local Government Area is required")
    private String localGovtArea;

    @NotBlank(message = "State of Origin is required")
    private String stateOfOrigin;

    // Emergency Contact
    private String emergencyContactName;

    @Pattern(regexp = "^[0-9]{11}$", message = "Emergency phone must be 11 digits")
    private String emergencyContactPhone;

    private String emergencyContactRelationship;

    // Promotion fields
    private Boolean excludeFromPromotion;
    private String promotionHoldReason;

    // URL of the profile picture
    private String profilePictureUrl;
}