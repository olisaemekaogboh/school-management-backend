// src/main/java/com/inkFront/schoolManagement/model/PortalNotification.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "portal_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String message;

    private String audience; // "ALL", "PARENTS", "STUDENTS", etc.

    private Long announcementId;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}