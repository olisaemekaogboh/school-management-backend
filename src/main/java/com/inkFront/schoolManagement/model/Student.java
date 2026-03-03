package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String middleName;

    @Column(unique = true, nullable = false)
    private String admissionNumber;

    @Column(nullable = false)
    private String studentClass;

    private String classArm;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dateOfBirth;

    private String parentName;

    private String parentPhone;

    private String parentEmail;

    private String address;

    private String localGovtArea;

    private String stateOfOrigin;

    private String nationality;

    private String religion;

    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    private LocalDate admissionDate;

    private String previousSchool;

    private String profilePictureUrl;


    // PROMOTION FIELDS
    @Column(columnDefinition = "boolean default false")
    private boolean excludeFromPromotion;

    private String promotionHoldReason;

    // EMERGENCY CONTACT FIELDS
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // TIMESTAMP FIELDS
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = StudentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Gender {
        MALE, FEMALE
    }

    public enum StudentStatus {
        ACTIVE, GRADUATED, TRANSFERRED, SUSPENDED, WITHDRAWN
    }
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @ManyToOne
    @JoinColumn(name = "bus_route_id")
    private BusRoute busRoute;
}