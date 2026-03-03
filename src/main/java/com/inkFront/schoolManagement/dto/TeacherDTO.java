// src/main/java/com/inkFront/schoolManagement/dto/TeacherDTO.java
package com.inkFront.schoolManagement.dto;

import com.inkFront.schoolManagement.model.Teacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phoneNumber;
    private String alternatePhone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String qualification;
    private String specialization;
    private Integer yearsOfExperience;
    private String employeeId;
    private String teacherId;
    private String department;
    private String designation;
    private LocalDate dateOfJoining;
    private String employmentType;
    private String employmentStatus;
    private String maritalStatus;
    private List<String> subjects = new ArrayList<>();
    private List<String> qualifications = new ArrayList<>();
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String profilePictureUrl;
    private String status;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TeacherDTO fromEntity(Teacher teacher) {
        if (teacher == null) return null;

        return TeacherDTO.builder()
                .id(teacher.getId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .middleName(teacher.getMiddleName())
                .email(teacher.getEmail())
                .phoneNumber(teacher.getPhoneNumber())
                .alternatePhone(teacher.getAlternatePhone())
                .dateOfBirth(teacher.getDateOfBirth())
                .gender(teacher.getGender() != null ? teacher.getGender().name() : null)
                .address(teacher.getAddress())
                .qualification(teacher.getQualification())
                .specialization(teacher.getSpecialization())
                .yearsOfExperience(teacher.getYearsOfExperience())
                .employeeId(teacher.getEmployeeId())
                .teacherId(teacher.getTeacherId())
                .department(teacher.getDepartment())
                .designation(teacher.getDesignation())
                .dateOfJoining(teacher.getDateOfJoining())
                .employmentType(teacher.getEmploymentType() != null ? teacher.getEmploymentType().name() : null)
                .employmentStatus(teacher.getEmploymentStatus() != null ? teacher.getEmploymentStatus().name() : null)
                .maritalStatus(teacher.getMaritalStatus() != null ? teacher.getMaritalStatus().name() : null)  // FIXED: Convert enum to String
                .subjects(teacher.getSubjects() != null ? teacher.getSubjects() : new ArrayList<>())
                .qualifications(teacher.getQualifications() != null ? teacher.getQualifications() : new ArrayList<>())
                .emergencyContactName(teacher.getEmergencyContactName())
                .emergencyContactPhone(teacher.getEmergencyContactPhone())
                .emergencyContactRelationship(teacher.getEmergencyContactRelationship())
                .profilePictureUrl(teacher.getProfilePictureUrl())
                .status(teacher.getStatus() != null ? teacher.getStatus().name() : null)
                .userId(teacher.getUser() != null ? teacher.getUser().getId() : null)
                .createdAt(teacher.getCreatedAt())
                .updatedAt(teacher.getUpdatedAt())
                .build();
    }

    public static Teacher toEntity(TeacherDTO dto) {
        if (dto == null) return null;

        Teacher teacher = new Teacher();
        teacher.setId(dto.getId());
        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setMiddleName(dto.getMiddleName());
        teacher.setEmail(dto.getEmail());
        teacher.setPhoneNumber(dto.getPhoneNumber());
        teacher.setAlternatePhone(dto.getAlternatePhone());
        teacher.setDateOfBirth(dto.getDateOfBirth());

        // Gender conversion
        if (dto.getGender() != null && !dto.getGender().isEmpty()) {
            try {
                teacher.setGender(Teacher.Gender.valueOf(dto.getGender()));
            } catch (IllegalArgumentException e) {
                teacher.setGender(Teacher.Gender.MALE); // Default
            }
        } else {
            teacher.setGender(Teacher.Gender.MALE);
        }

        teacher.setAddress(dto.getAddress());
        teacher.setQualification(dto.getQualification());
        teacher.setSpecialization(dto.getSpecialization());
        teacher.setYearsOfExperience(dto.getYearsOfExperience());
        teacher.setEmployeeId(dto.getEmployeeId());
        teacher.setTeacherId(dto.getTeacherId());
        teacher.setDepartment(dto.getDepartment());
        teacher.setDesignation(dto.getDesignation());
        teacher.setDateOfJoining(dto.getDateOfJoining());

        // Employment Type conversion
        if (dto.getEmploymentType() != null && !dto.getEmploymentType().isEmpty()) {
            try {
                teacher.setEmploymentType(Teacher.EmploymentType.valueOf(dto.getEmploymentType()));
            } catch (IllegalArgumentException e) {
                teacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
            }
        } else {
            teacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
        }

        // Employment Status conversion
        if (dto.getEmploymentStatus() != null && !dto.getEmploymentStatus().isEmpty()) {
            try {
                teacher.setEmploymentStatus(Teacher.EmploymentStatus.valueOf(dto.getEmploymentStatus()));
            } catch (IllegalArgumentException e) {
                teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
            }
        } else {
            teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        }

        // Marital Status conversion
        if (dto.getMaritalStatus() != null && !dto.getMaritalStatus().isEmpty()) {
            try {
                teacher.setMaritalStatus(Teacher.MaritalStatus.valueOf(dto.getMaritalStatus()));
            } catch (IllegalArgumentException e) {
                teacher.setMaritalStatus(Teacher.MaritalStatus.SINGLE); // Default
            }
        } else {
            teacher.setMaritalStatus(Teacher.MaritalStatus.SINGLE);
        }

        teacher.setSubjects(dto.getSubjects() != null ? dto.getSubjects() : new ArrayList<>());
        teacher.setQualifications(dto.getQualifications() != null ? dto.getQualifications() : new ArrayList<>());
        teacher.setEmergencyContactName(dto.getEmergencyContactName());
        teacher.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        teacher.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        teacher.setProfilePictureUrl(dto.getProfilePictureUrl());

        // Status conversion
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            try {
                teacher.setStatus(Teacher.TeacherStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                teacher.setStatus(Teacher.TeacherStatus.ACTIVE);
            }
        } else {
            teacher.setStatus(Teacher.TeacherStatus.ACTIVE);
        }

        return teacher;
    }

    // Helper method to create a minimal teacher DTO for invitations
    public static TeacherDTO fromInvitation(String firstName, String lastName, String email, String phoneNumber) {
        return TeacherDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .status(Teacher.TeacherStatus.ACTIVE.name())
                .employmentType(Teacher.EmploymentType.FULL_TIME.name())
                .employmentStatus(Teacher.EmploymentStatus.ACTIVE.name())
                .maritalStatus(Teacher.MaritalStatus.SINGLE.name())
                .gender(Teacher.Gender.MALE.name())
                .subjects(new ArrayList<>())
                .qualifications(new ArrayList<>())
                .build();
    }

    // Validation method to check if required fields are present
    public boolean isValidForCreation() {
        return firstName != null && !firstName.isEmpty() &&
                lastName != null && !lastName.isEmpty() &&
                email != null && !email.isEmpty();
    }

    // Method to get full name
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) fullName.append(firstName);
        if (middleName != null && !middleName.isEmpty()) fullName.append(" ").append(middleName);
        if (lastName != null) fullName.append(" ").append(lastName);
        return fullName.toString().trim();
    }
}