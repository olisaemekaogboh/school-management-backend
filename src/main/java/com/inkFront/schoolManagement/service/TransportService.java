package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.BusRouteDTO;

import java.util.List;

public interface TransportService {
    BusRouteDTO createRoute(BusRouteDTO dto);
    BusRouteDTO updateRoute(Long id, BusRouteDTO dto);
    BusRouteDTO getRoute(Long id);
    void deleteRoute(Long id);
    List<BusRouteDTO> getAllRoutes();
}