package com.inkFront.schoolManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    @JsonIgnoreProperties({"classTeacher"})
    private SchoolClass schoolClass;

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

    @Column(columnDefinition = "boolean default false")
    private boolean excludeFromPromotion;

    private String promotionHoldReason;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_route_id")
    @JsonIgnoreProperties({"students"})
    private BusRoute busRoute;

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

    public String getStudentClass() {
        return schoolClass != null ? schoolClass.getClassName() : null;
    }

    public String getClassArm() {
        return schoolClass != null ? schoolClass.getArm() : null;
    }

    public enum Gender {
        MALE, FEMALE
    }

    public enum StudentStatus {
        ACTIVE, GRADUATED, TRANSFERRED, SUSPENDED, WITHDRAWN
    }
}