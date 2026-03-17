package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.BusRouteRequestDTO;
import com.inkFront.schoolManagement.dto.BusRouteResponseDTO;
import com.inkFront.schoolManagement.dto.TransportStatisticsDTO;
import com.inkFront.schoolManagement.dto.TransportStudentDTO;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.BusRoute;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.BusRouteRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.TransportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportServiceImpl implements TransportService {

    private final BusRouteRepository busRouteRepository;
    private final StudentRepository studentRepository;

    @Override
    public BusRouteResponseDTO createRoute(BusRouteRequestDTO request) {
        String routeCode = request.getRouteCode().trim().toUpperCase();

        if (busRouteRepository.existsByRouteCode(routeCode)) {
            throw new IllegalArgumentException("Route code already exists");
        }

        BusRoute route = new BusRoute();
        route.setRouteName(request.getRouteName());
        route.setRouteCode(routeCode);
        route.setPickupLocation(request.getPickupLocation());
        route.setDropoffLocation(request.getDropoffLocation());
        route.setPickupTime(request.getPickupTime());
        route.setDropoffTime(request.getDropoffTime());
        route.setDriverName(request.getDriverName());
        route.setDriverPhone(request.getDriverPhone());
        route.setAssistantName(request.getAssistantName());
        route.setAssistantPhone(request.getAssistantPhone());
        route.setMonthlyFee(request.getMonthlyFee());
        route.setCapacity(request.getCapacity());
        route.setActive(request.getActive() == null || request.getActive());

        return toResponse(busRouteRepository.save(route));
    }

    @Override
    public BusRouteResponseDTO updateRoute(Long id, BusRouteRequestDTO request) {
        BusRoute route = busRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus route not found with id: " + id));

        String routeCode = request.getRouteCode().trim().toUpperCase();

        busRouteRepository.findByRouteCode(routeCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Route code already exists");
            }
        });

        long assignedCount = studentRepository.countByBusRouteId(id);

        if (request.getCapacity() < assignedCount) {
            throw new IllegalArgumentException("Capacity cannot be less than currently assigned students");
        }

        route.setRouteName(request.getRouteName());
        route.setRouteCode(routeCode);
        route.setPickupLocation(request.getPickupLocation());
        route.setDropoffLocation(request.getDropoffLocation());
        route.setPickupTime(request.getPickupTime());
        route.setDropoffTime(request.getDropoffTime());
        route.setDriverName(request.getDriverName());
        route.setDriverPhone(request.getDriverPhone());
        route.setAssistantName(request.getAssistantName());
        route.setAssistantPhone(request.getAssistantPhone());
        route.setMonthlyFee(request.getMonthlyFee());
        route.setCapacity(request.getCapacity());
        route.setActive(request.getActive() == null || request.getActive());

        return toResponse(busRouteRepository.save(route));
    }

    @Override
    public BusRouteResponseDTO getRoute(Long id) {
        BusRoute route = busRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus route not found with id: " + id));
        return toResponse(route);
    }

    @Override
    public void deleteRoute(Long id) {
        BusRoute route = busRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus route not found with id: " + id));

        long assignedCount = studentRepository.countByBusRouteId(id);
        if (assignedCount > 0) {
            throw new IllegalArgumentException("Cannot delete a route that still has assigned students");
        }

        busRouteRepository.delete(route);
    }

    @Override
    public List<BusRouteResponseDTO> getAllRoutes() {
        return busRouteRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<BusRouteResponseDTO> getActiveRoutes() {
        return busRouteRepository.findByActiveTrueOrderByRouteNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BusRouteResponseDTO assignStudentToRoute(Long studentId, Long routeId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        BusRoute route = busRouteRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus route not found with id: " + routeId));

        if (!Boolean.TRUE.equals(route.getActive())) {
            throw new IllegalArgumentException("Cannot assign a student to an inactive route");
        }

        Long currentRouteId = student.getBusRoute() != null ? student.getBusRoute().getId() : null;
        long assignedCount = studentRepository.countByBusRouteId(routeId);

        if (currentRouteId == null || !currentRouteId.equals(routeId)) {
            if (assignedCount >= route.getCapacity()) {
                throw new IllegalArgumentException("This route is already full");
            }
        }

        student.setBusRoute(route);
        studentRepository.save(student);

        return toResponse(route);
    }

    @Override
    public void removeStudentFromRoute(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setBusRoute(null);
        studentRepository.save(student);
    }

    @Override
    public List<TransportStudentDTO> getRouteStudents(Long routeId) {
        if (!busRouteRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Bus route not found with id: " + routeId);
        }

        return studentRepository.findTransportStudentsByRouteId(routeId)
                .stream()
                .map(student -> new TransportStudentDTO(
                        student.getId(),
                        student.getFirstName(),
                        student.getLastName(),
                        student.getMiddleName(),
                        student.getAdmissionNumber(),
                        student.getStudentClass(),
                        student.getClassArm(),
                        student.getParentName(),
                        student.getParentPhone()
                ))
                .toList();
    }

    @Override
    public BusRouteResponseDTO updateBusLocation(Long routeId, Double latitude, Double longitude) {
        BusRoute route = busRouteRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus route not found with id: " + routeId));

        route.setCurrentLatitude(latitude);
        route.setCurrentLongitude(longitude);

        return toResponse(busRouteRepository.save(route));
    }

    @Override
    public Map<String, Double> getBusLocation(Long routeId) {
        BusRoute route = busRouteRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus route not found with id: " + routeId));

        Map<String, Double> location = new HashMap<>();
        location.put("latitude", route.getCurrentLatitude());
        location.put("longitude", route.getCurrentLongitude());
        return location;
    }

    @Override
    public TransportStatisticsDTO getTransportStatistics() {
        long totalRoutes = busRouteRepository.count();
        long activeRoutes = busRouteRepository.countByActiveTrue();
        long assignedStudents = studentRepository.countByBusRouteIsNotNull();
        long unassignedStudents = studentRepository.countByBusRouteIsNull();
        long totalCapacity = busRouteRepository.sumTotalCapacity();

        long availableSlots = totalCapacity - assignedStudents;

        return new TransportStatisticsDTO(
                totalRoutes,
                activeRoutes,
                assignedStudents,
                unassignedStudents,
                totalCapacity,
                availableSlots
        );
    }

    @Override
    public BusRouteResponseDTO getStudentAssignedRoute(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (student.getBusRoute() == null) {
            throw new ResourceNotFoundException("Student has no assigned transport route");
        }

        return toResponse(student.getBusRoute());
    }

    private BusRouteResponseDTO toResponse(BusRoute route) {
        long assigned = studentRepository.countByBusRouteId(route.getId());
        long available = route.getCapacity() == null ? 0 : route.getCapacity() - assigned;

        return new BusRouteResponseDTO(
                route.getId(),
                route.getRouteName(),
                route.getRouteCode(),
                route.getPickupLocation(),
                route.getDropoffLocation(),
                route.getPickupTime(),
                route.getDropoffTime(),
                route.getDriverName(),
                route.getDriverPhone(),
                route.getAssistantName(),
                route.getAssistantPhone(),
                route.getMonthlyFee(),
                route.getCapacity(),
                assigned,
                available,
                route.getActive(),
                route.getCurrentLatitude(),
                route.getCurrentLongitude(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }
}