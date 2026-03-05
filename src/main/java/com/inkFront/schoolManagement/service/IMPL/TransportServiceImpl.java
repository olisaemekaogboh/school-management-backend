package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.BusRouteDTO;
import com.inkFront.schoolManagement.model.BusRoute;
import com.inkFront.schoolManagement.repository.BusRouteRepository;
import com.inkFront.schoolManagement.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportServiceImpl implements TransportService {

    private final BusRouteRepository busRouteRepository;

    private BusRouteDTO toDTO(BusRoute r) {
        BusRouteDTO dto = new BusRouteDTO();
        dto.setId(r.getId());
        dto.setRouteName(r.getRouteName());

        // ✅ entity uses "stops"
        dto.setStops(r.getStops());

        dto.setRouteNumber(r.getRouteNumber());
        dto.setDriverName(r.getDriverName());
        dto.setDriverPhone(r.getDriverPhone());
        dto.setAssistantName(r.getAssistantName());
        dto.setAssistantPhone(r.getAssistantPhone());

        // ✅ entity uses "busNumber"
        dto.setBusNumber(r.getBusNumber());

        dto.setCapacity(r.getCapacity());
        dto.setMorningPickupTime(r.getMorningPickupTime() != null ? r.getMorningPickupTime().toString() : null);
        dto.setAfternoonDropoffTime(r.getAfternoonDropoffTime() != null ? r.getAfternoonDropoffTime().toString() : null);

        dto.setMonthlyFee(r.getMonthlyFee());

        // ✅ entity uses status enum
        dto.setStatus(r.getStatus() != null ? r.getStatus().name() : null);

        return dto;
    }

    private void apply(BusRoute r, BusRouteDTO dto) {
        r.setRouteName(dto.getRouteName());
        r.setRouteNumber(dto.getRouteNumber());

        r.setDriverName(dto.getDriverName());
        r.setDriverPhone(dto.getDriverPhone());
        r.setAssistantName(dto.getAssistantName());
        r.setAssistantPhone(dto.getAssistantPhone());

        r.setBusNumber(dto.getBusNumber());
        r.setCapacity(dto.getCapacity());

        r.setStops(dto.getStops());

        if (dto.getMorningPickupTime() != null && !dto.getMorningPickupTime().trim().isEmpty()) {
            r.setMorningPickupTime(java.time.LocalTime.parse(dto.getMorningPickupTime().trim()));
        }

        if (dto.getAfternoonDropoffTime() != null && !dto.getAfternoonDropoffTime().trim().isEmpty()) {
            r.setAfternoonDropoffTime(java.time.LocalTime.parse(dto.getAfternoonDropoffTime().trim()));
        }

        r.setMonthlyFee(dto.getMonthlyFee());

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            r.setStatus(BusRoute.RouteStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        }
    }

    @Override
    public BusRouteDTO createRoute(BusRouteDTO dto) {
        BusRoute r = new BusRoute();
        apply(r, dto);

        // default status if not supplied
        if (r.getStatus() == null) r.setStatus(BusRoute.RouteStatus.ACTIVE);

        return toDTO(busRouteRepository.save(r));
    }

    @Override
    public BusRouteDTO updateRoute(Long id, BusRouteDTO dto) {
        BusRoute r = busRouteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        apply(r, dto);

        return toDTO(busRouteRepository.save(r));
    }

    @Override
    @Transactional(readOnly = true)
    public BusRouteDTO getRoute(Long id) {
        return busRouteRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Route not found"));
    }

    @Override
    public void deleteRoute(Long id) {
        busRouteRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusRouteDTO> getAllRoutes() {
        return busRouteRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(toList());
    }
}