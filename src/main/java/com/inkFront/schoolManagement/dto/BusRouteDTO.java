package com.inkFront.schoolManagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class BusRouteDTO {

    private Long id;

    private String routeName;
    private String routeNumber;

    private String driverName;
    private String driverPhone;

    private String assistantName;
    private String assistantPhone;

    private String busNumber;

    private Integer capacity;

    // ✅ entity uses stops
    private List<String> stops;

    // times as strings "07:30"
    private String morningPickupTime;
    private String afternoonDropoffTime;

    private Double monthlyFee;

    // ACTIVE / INACTIVE / MAINTENANCE
    private String status;
}