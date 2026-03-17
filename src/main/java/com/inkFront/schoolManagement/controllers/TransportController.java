package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.BusRouteRequestDTO;
import com.inkFront.schoolManagement.dto.BusRouteResponseDTO;
import com.inkFront.schoolManagement.dto.TransportStatisticsDTO;
import com.inkFront.schoolManagement.dto.TransportStudentDTO;
import com.inkFront.schoolManagement.service.TransportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transport")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @PostMapping("/routes")
    public ResponseEntity<BusRouteResponseDTO> createRoute(@Valid @RequestBody BusRouteRequestDTO request) {
        return new ResponseEntity<>(transportService.createRoute(request), HttpStatus.CREATED);
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<BusRouteResponseDTO> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody BusRouteRequestDTO request
    ) {
        return ResponseEntity.ok(transportService.updateRoute(id, request));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<BusRouteResponseDTO> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(transportService.getRoute(id));
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        transportService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routes")
    public ResponseEntity<List<BusRouteResponseDTO>> getAllRoutes() {
        return ResponseEntity.ok(transportService.getAllRoutes());
    }

    @GetMapping("/routes/active")
    public ResponseEntity<List<BusRouteResponseDTO>> getActiveRoutes() {
        return ResponseEntity.ok(transportService.getActiveRoutes());
    }

    @PostMapping("/assign")
    public ResponseEntity<BusRouteResponseDTO> assignStudentToRoute(
            @RequestParam Long studentId,
            @RequestParam Long routeId,
            @RequestParam(required = false) Integer stopIndex
    ) {
        return ResponseEntity.ok(transportService.assignStudentToRoute(studentId, routeId));
    }

    @DeleteMapping("/remove/{studentId}")
    public ResponseEntity<Void> removeStudentFromRoute(@PathVariable Long studentId) {
        transportService.removeStudentFromRoute(studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routes/{routeId}/students")
    public ResponseEntity<List<TransportStudentDTO>> getRouteStudents(@PathVariable Long routeId) {
        return ResponseEntity.ok(transportService.getRouteStudents(routeId));
    }

    @PostMapping("/update-location/{routeId}")
    public ResponseEntity<BusRouteResponseDTO> updateBusLocation(
            @PathVariable Long routeId,
            @RequestParam("lat") Double latitude,
            @RequestParam("lng") Double longitude
    ) {
        return ResponseEntity.ok(transportService.updateBusLocation(routeId, latitude, longitude));
    }

    @GetMapping("/location/{routeId}")
    public ResponseEntity<Map<String, Double>> getBusLocation(@PathVariable Long routeId) {
        return ResponseEntity.ok(transportService.getBusLocation(routeId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<TransportStatisticsDTO> getTransportStatistics() {
        return ResponseEntity.ok(transportService.getTransportStatistics());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<BusRouteResponseDTO> getStudentAssignedRoute(@PathVariable Long studentId) {
        return ResponseEntity.ok(transportService.getStudentAssignedRoute(studentId));
    }
}