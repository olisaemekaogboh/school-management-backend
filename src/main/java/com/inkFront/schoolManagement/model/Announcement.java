// src/main/java/com/inkFront/schoolManagement/model/Announcement.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementPriority priority;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "announcement_audience",
            joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "audience")
    private List<Audience> audience = new ArrayList<>();

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    private String createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Event specific fields
    private LocalDate eventDate;
    private String eventLocation;
    private String eventTime;

    // Fee specific fields
    private Double feeAmount;
    private String feeDescription;
    private LocalDate feeDueDate;

    // Result specific fields
    private LocalDate resultReleaseDate;
    private String term; // FIRST, SECOND, THIRD
    private String session;

    // Attachment
    private String attachmentUrl;
    private String attachmentName;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AnnouncementType {
        RESUMPTION, HOLIDAY, MIDTERM_BREAK, EXAM, RESULT,
        FEE, EVENT, MEETING, SPORTS, CULTURAL, EMERGENCY, GENERAL
    }

    public enum AnnouncementPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum Audience {
        ALL, STUDENTS, PARENTS, TEACHERS, STAFF, BOARDING, DAY
    }
}