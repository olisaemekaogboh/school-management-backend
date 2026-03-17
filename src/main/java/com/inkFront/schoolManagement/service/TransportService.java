package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.BusRouteRequestDTO;
import com.inkFront.schoolManagement.dto.BusRouteResponseDTO;
import com.inkFront.schoolManagement.dto.TransportStatisticsDTO;
import com.inkFront.schoolManagement.dto.TransportStudentDTO;

import java.util.List;
import java.util.Map;

public interface TransportService {

    BusRouteResponseDTO createRoute(BusRouteRequestDTO request);

    BusRouteResponseDTO updateRoute(Long id, BusRouteRequestDTO request);

    BusRouteResponseDTO getRoute(Long id);

    void deleteRoute(Long id);

    List<BusRouteResponseDTO> getAllRoutes();

    List<BusRouteResponseDTO> getActiveRoutes();

    BusRouteResponseDTO assignStudentToRoute(Long studentId, Long routeId);

    void removeStudentFromRoute(Long studentId);

    List<TransportStudentDTO> getRouteStudents(Long routeId);

    BusRouteResponseDTO updateBusLocation(Long routeId, Double latitude, Double longitude);

    Map<String, Double> getBusLocation(Long routeId);

    TransportStatisticsDTO getTransportStatistics();

    BusRouteResponseDTO getStudentAssignedRoute(Long studentId);
}