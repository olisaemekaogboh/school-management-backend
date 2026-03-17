package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteResponseDTO {

    private Long id;
    private String routeName;
    private String routeCode;
    private String pickupLocation;
    private String dropoffLocation;
    private LocalTime pickupTime;
    private LocalTime dropoffTime;
    private String driverName;
    private String driverPhone;
    private String assistantName;
    private String assistantPhone;
    private BigDecimal monthlyFee;
    private Integer capacity;
    private Long assignedStudents;
    private Long availableSlots;
    private Boolean active;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}