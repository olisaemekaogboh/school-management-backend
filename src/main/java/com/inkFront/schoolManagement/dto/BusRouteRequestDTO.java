package com.inkFront.schoolManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteRequestDTO {

    @NotBlank(message = "Route name is required")
    private String routeName;

    @NotBlank(message = "Route code is required")
    private String routeCode;

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Drop-off location is required")
    private String dropoffLocation;

    @NotNull(message = "Pickup time is required")
    private LocalTime pickupTime;

    @NotNull(message = "Drop-off time is required")
    private LocalTime dropoffTime;

    @NotBlank(message = "Driver name is required")
    private String driverName;

    @NotBlank(message = "Driver phone is required")
    private String driverPhone;

    private String assistantName;

    private String assistantPhone;

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal monthlyFee;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private Boolean active;
}