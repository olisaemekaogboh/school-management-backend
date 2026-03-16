package com.inkFront.schoolManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String middleName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private String occupation;
    private String companyName;
    private String officeAddress;

    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String profilePictureUrl;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"parent", "hibernateLazyInitializer", "handler"})
    @Builder.Default
    private List<Student> wards = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Relationship {
        FATHER, MOTHER, GUARDIAN, OTHER
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}