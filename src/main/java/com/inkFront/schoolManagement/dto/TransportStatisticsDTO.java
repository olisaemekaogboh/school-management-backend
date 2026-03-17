package com.inkFront.schoolManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransportStatisticsDTO {
    private long totalRoutes;
    private long activeRoutes;
    private long assignedStudents;
    private long unassignedStudents;
    private long totalCapacity;
    private long availableSlots;
}