// src/main/java/com/inkFront/schoolManagement/model/BusRoute.java
package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bus_routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String routeName;

    private String routeNumber;

    private String driverName;

    private String driverPhone;

    private String assistantName;

    private String assistantPhone;

    private String busNumber;

    private Integer capacity;

    @ElementCollection
    @CollectionTable(name = "route_stops", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "stop")
    private List<String> stops = new ArrayList<>();

    private LocalTime morningPickupTime;

    private LocalTime afternoonDropoffTime;


    @OneToMany(mappedBy = "busRoute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();
    private Double monthlyFee;

    @Enumerated(EnumType.STRING)
    private RouteStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = RouteStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RouteStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
}