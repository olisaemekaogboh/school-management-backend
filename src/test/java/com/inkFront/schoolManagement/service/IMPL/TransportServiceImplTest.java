// src/test/java/com/inkFront/schoolManagement/service/IMPL/TransportServiceImplTest.java
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportServiceImplTest {

    @Mock
    private BusRouteRepository busRouteRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private TransportServiceImpl transportService;

    private BusRoute testRoute;
    private BusRouteRequestDTO testRequest;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testRoute = new BusRoute();
        testRoute.setId(1L);
        testRoute.setRouteName("North Campus Route");
        testRoute.setRouteCode("NC-001");
        testRoute.setPickupLocation("North Gate");
        testRoute.setDropoffLocation("School Main Building");
        testRoute.setPickupTime(LocalTime.of(7, 30));
        testRoute.setDropoffTime(LocalTime.of(14, 30));
        testRoute.setDriverName("John Doe");
        testRoute.setDriverPhone("+1234567890");
        testRoute.setMonthlyFee(BigDecimal.valueOf(5000));
        testRoute.setCapacity(30);
        testRoute.setActive(true);

        testRequest = new BusRouteRequestDTO();
        testRequest.setRouteName("North Campus Route");
        testRequest.setRouteCode("NC-001");
        testRequest.setPickupLocation("North Gate");
        testRequest.setDropoffLocation("School Main Building");
        testRequest.setPickupTime(LocalTime.of(7, 30));
        testRequest.setDropoffTime(LocalTime.of(14, 30));
        testRequest.setDriverName("John Doe");
        testRequest.setDriverPhone("+1234567890");
        testRequest.setMonthlyFee(BigDecimal.valueOf(5000));
        testRequest.setCapacity(30);
        testRequest.setActive(true);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setAdmissionNumber("STU001");
    }

    @Test
    void createRoute_ShouldCreateRoute() {
        when(busRouteRepository.existsByRouteCode("NC-001")).thenReturn(false);
        when(busRouteRepository.save(any(BusRoute.class))).thenReturn(testRoute);

        BusRouteResponseDTO result = transportService.createRoute(testRequest);

        assertNotNull(result);
        assertEquals("North Campus Route", result.getRouteName());
        verify(busRouteRepository).save(any(BusRoute.class));
    }

    @Test
    void createRoute_WithExistingCode_ShouldThrowException() {
        when(busRouteRepository.existsByRouteCode("NC-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> transportService.createRoute(testRequest));
    }

    @Test
    void updateRoute_ShouldUpdateRoute() {
        when(busRouteRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(busRouteRepository.findByRouteCode("NC-001")).thenReturn(Optional.of(testRoute));
        when(studentRepository.countByBusRouteId(1L)).thenReturn(10L);
        when(busRouteRepository.save(any(BusRoute.class))).thenReturn(testRoute);

        BusRouteResponseDTO result = transportService.updateRoute(1L, testRequest);

        assertNotNull(result);
        verify(busRouteRepository).save(any(BusRoute.class));
    }

    @Test
    void updateRoute_WithNonExistentId_ShouldThrowException() {
        when(busRouteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transportService.updateRoute(999L, testRequest));
    }

    @Test
    void updateRoute_WithCapacityLessThanAssigned_ShouldThrowException() {
        testRequest.setCapacity(5);

        when(busRouteRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(studentRepository.countByBusRouteId(1L)).thenReturn(10L);

        assertThrows(IllegalArgumentException.class, () -> transportService.updateRoute(1L, testRequest));
    }

    @Test
    void getRoute_ShouldReturnRoute() {
        when(busRouteRepository.findById(1L)).thenReturn(Optional.of(testRoute));

        BusRouteResponseDTO result = transportService.getRoute(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAllRoutes_ShouldReturnList() {
        when(busRouteRepository.findAll()).thenReturn(List.of(testRoute));

        List<BusRouteResponseDTO> result = transportService.getAllRoutes();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void deleteRoute_ShouldDeleteRoute() {
        when(busRouteRepository.findById(1L)).thenReturn(Optional.of(testRoute));
        when(studentRepository.countByBusRouteId(1L)).thenReturn(0L);
        doNothing().when(busRouteRepository).delete(testRoute);

        transportService.deleteRoute(1L);

        verify(busRouteRepository).delete(testRoute);
    }

    @Test
    void getRouteStudents_ShouldReturnStudents() {
        when(busRouteRepository.existsById(1L)).thenReturn(true);
        when(studentRepository.findTransportStudentsByRouteId(1L)).thenReturn(List.of(testStudent));

        List<TransportStudentDTO> result = transportService.getRouteStudents(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // TransportServiceImplTest.java
    @Test
    void getStatistics_ShouldReturnStats() {

        TransportStatisticsDTO result = transportService.getTransportStatistics();

        assertNotNull(result);
    }
}