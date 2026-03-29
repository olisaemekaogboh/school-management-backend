package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeType feeType;

    private String description;

    @Column(nullable = false)
    private Double amount;

    private Double paidAmount;
    private Double balance;

    private LocalDate dueDate;
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String paymentReference;
    private String paymentMethod;

    private Integer reminderCount;
    private LocalDate lastReminderSent;

    private String notes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (paidAmount == null) {
            paidAmount = 0.0;
        }

        if (amount == null) {
            amount = 0.0;
        }

        if (reminderCount == null) {
            reminderCount = 0;
        }

        calculateBalance();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (paidAmount == null) {
            paidAmount = 0.0;
        }

        if (amount == null) {
            amount = 0.0;
        }

        calculateBalance();
    }

    private void calculateBalance() {
        double safeAmount = amount != null ? amount : 0.0;
        double safePaidAmount = paidAmount != null ? paidAmount : 0.0;

        this.balance = safeAmount - safePaidAmount;

        if (this.balance <= 0) {
            this.balance = 0.0;
            this.status = PaymentStatus.PAID;
        } else if (safePaidAmount > 0) {
            this.status = PaymentStatus.PARTIAL;
        } else if (this.dueDate != null && LocalDate.now().isAfter(this.dueDate)) {
            this.status = PaymentStatus.OVERDUE;
        } else {
            this.status = PaymentStatus.PENDING;
        }
    }

    public enum Term {
        FIRST, SECOND, THIRD, ANNUAL
    }

    public enum FeeType {
        TUITION("Tuition Fee"),
        BOARDING("Boarding Fee"),
        DEVELOPMENT("Development Levy"),
        EXAM("Examination Fee"),
        SPORTS("Sports Fee"),
        LIBRARY("Library Fee"),
        ICT("ICT Fee"),
        PTA("PTA Levy"),
        UNIFORM("Uniform Fee"),
        TRANSPORT("Transport Fee"),
        OTHER("Other");

        private final String displayName;

        FeeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PaymentStatus {
        PENDING, PARTIAL, PAID, OVERDUE, WAIVED
    }
}