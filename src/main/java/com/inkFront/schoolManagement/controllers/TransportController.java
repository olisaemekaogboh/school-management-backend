package com.inkFront.schoolManagement.controllers;

import com.inkFront.schoolManagement.dto.BusRouteDTO;
import com.inkFront.schoolManagement.service.TransportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @PostMapping("/routes")
    public BusRouteDTO create(@Valid @RequestBody BusRouteDTO dto) {
        return transportService.createRoute(dto);
    }

    @PutMapping("/routes/{id}")
    public BusRouteDTO update(@PathVariable Long id, @Valid @RequestBody BusRouteDTO dto) {
        return transportService.updateRoute(id, dto);
    }

    @GetMapping("/routes/{id}")
    public BusRouteDTO get(@PathVariable Long id) {
        return transportService.getRoute(id);
    }

    @DeleteMapping("/routes/{id}")
    public void delete(@PathVariable Long id) {
        transportService.deleteRoute(id);
    }

    @GetMapping("/routes")
    public List<BusRouteDTO> all() {
        return transportService.getAllRoutes();
    }
}