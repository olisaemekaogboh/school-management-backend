package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "result_checker_pin_usages",
        indexes = {
                @Index(name = "idx_result_checker_pin_usage_pin", columnList = "pin_id"),
                @Index(name = "idx_result_checker_pin_usage_student", columnList = "student_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultCheckerPinUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pin_id", nullable = false)
    private ResultCheckerPin pin;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultCheckerPin.PinScope pinScope;

    @Column(nullable = false, length = 30)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Result.Term term;

    @Column(length = 150)
    private String usedByName;

    @Column(updatable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}