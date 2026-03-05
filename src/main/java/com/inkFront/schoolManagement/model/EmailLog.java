package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long announcementId;

    private String toEmail;

    private String subject;

    @Column(length = 2000)
    private String messageContent;

    private String status;

    private String errorMessage;

    private LocalDateTime sentAt;
}