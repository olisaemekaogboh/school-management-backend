// src/main/java/com/inkFront/schoolManagement/model/Teacher.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "teachers")
@Data
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String middleName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;
    private String alternatePhone;
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    private String address;
    private String qualification;
    private String specialization;
    private Integer yearsOfExperience;
    private String employeeId;
    private String teacherId;
    private String department;
    private String designation;
    private LocalDate dateOfJoining;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status")
    private EmploymentStatus employmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;
    // In Teacher.java entity
    @ElementCollection
    @CollectionTable(name = "teacher_subjects", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "subject")
    private Set<String> subjects = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "teacher_qualifications", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "qualification")
    private Set<String> qualifications = new HashSet<>();
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TeacherStatus status;

    @OneToOne(mappedBy = "teacher")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Gender {
        MALE, FEMALE
    }

    public enum EmploymentType {
        FULL_TIME, PART_TIME, CONTRACT, VISITING
    }

    public enum EmploymentStatus {
        ACTIVE, ON_LEAVE, TERMINATED, RETIRED, RESIGNED
    }

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED
    }

    public enum TeacherStatus {
        ACTIVE, INACTIVE, ON_LEAVE, TERMINATED
    }

}