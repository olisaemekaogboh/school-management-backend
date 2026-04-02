package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkFront.schoolManagement.dto.BusRouteRequestDTO;
import com.inkFront.schoolManagement.dto.BusRouteResponseDTO;
import com.inkFront.schoolManagement.dto.TransportStatisticsDTO;
import com.inkFront.schoolManagement.dto.TransportStudentDTO;
import com.inkFront.schoolManagement.service.TransportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransportService transportService;

    @InjectMocks
    private TransportController transportController;

    private ObjectMapper objectMapper;
    private BusRouteRequestDTO routeRequest;
    private BusRouteResponseDTO routeResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transportController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        routeRequest = new BusRouteRequestDTO();
        routeRequest.setRouteName("North Campus Route");
        routeRequest.setRouteCode("NC-001");
        routeRequest.setPickupLocation("North Gate");
        routeRequest.setDropoffLocation("School Main Building");
        routeRequest.setPickupTime(LocalTime.of(7, 30));
        routeRequest.setDropoffTime(LocalTime.of(14, 30));
        routeRequest.setDriverName("John Doe");
        routeRequest.setDriverPhone("+1234567890");
        routeRequest.setCapacity(30);
        routeRequest.setMonthlyFee(BigDecimal.valueOf(5000.0));
        routeRequest.setActive(true);

        routeResponse = new BusRouteResponseDTO();
        routeResponse.setId(1L);
        routeResponse.setRouteName("North Campus Route");
        routeResponse.setRouteCode("NC-001");
        routeResponse.setPickupLocation("North Gate");
        routeResponse.setDropoffLocation("School Main Building");
        routeResponse.setDriverName("John Doe");
        routeResponse.setDriverPhone("+1234567890");
        routeResponse.setCapacity(30);
        routeResponse.setActive(true);
    }

    @Test
    void createRoute_ShouldReturnCreatedRoute() throws Exception {
        when(transportService.createRoute(any(BusRouteRequestDTO.class))).thenReturn(routeResponse);

        mockMvc.perform(post("/api/transport/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.routeName").value("North Campus Route"))
                .andExpect(jsonPath("$.routeCode").value("NC-001"));

        verify(transportService, times(1)).createRoute(any(BusRouteRequestDTO.class));
    }

    @Test
    void updateRoute_ShouldReturnUpdatedRoute() throws Exception {
        when(transportService.updateRoute(eq(1L), any(BusRouteRequestDTO.class))).thenReturn(routeResponse);

        mockMvc.perform(put("/api/transport/routes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(transportService, times(1)).updateRoute(eq(1L), any(BusRouteRequestDTO.class));
    }

    @Test
    void getRoute_ShouldReturnRoute() throws Exception {
        when(transportService.getRoute(1L)).thenReturn(routeResponse);

        mockMvc.perform(get("/api/transport/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(transportService, times(1)).getRoute(1L);
    }

    @Test
    void deleteRoute_ShouldReturnNoContent() throws Exception {
        doNothing().when(transportService).deleteRoute(1L);

        mockMvc.perform(delete("/api/transport/routes/1"))
                .andExpect(status().isNoContent());

        verify(transportService, times(1)).deleteRoute(1L);
    }

    @Test
    void getAllRoutes_ShouldReturnList() throws Exception {
        List<BusRouteResponseDTO> routes = Arrays.asList(routeResponse);
        when(transportService.getAllRoutes()).thenReturn(routes);

        mockMvc.perform(get("/api/transport/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(transportService, times(1)).getAllRoutes();
    }

    @Test
    void getActiveRoutes_ShouldReturnList() throws Exception {
        List<BusRouteResponseDTO> routes = Arrays.asList(routeResponse);
        when(transportService.getActiveRoutes()).thenReturn(routes);

        mockMvc.perform(get("/api/transport/routes/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(transportService, times(1)).getActiveRoutes();
    }

    @Test
    void assignStudentToRoute_ShouldReturnUpdatedRoute() throws Exception {
        when(transportService.assignStudentToRoute(eq(1L), eq(1L))).thenReturn(routeResponse);

        mockMvc.perform(post("/api/transport/assign")
                        .param("studentId", "1")
                        .param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(transportService, times(1)).assignStudentToRoute(eq(1L), eq(1L));
    }

    @Test
    void assignStudentToRoute_WithStopIndex_ShouldReturnUpdatedRoute() throws Exception {
        when(transportService.assignStudentToRoute(eq(1L), eq(1L))).thenReturn(routeResponse);

        mockMvc.perform(post("/api/transport/assign")
                        .param("studentId", "1")
                        .param("routeId", "1")
                        .param("stopIndex", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(transportService, times(1)).assignStudentToRoute(eq(1L), eq(1L));
    }

    @Test
    void removeStudentFromRoute_ShouldReturnNoContent() throws Exception {
        doNothing().when(transportService).removeStudentFromRoute(1L);

        mockMvc.perform(delete("/api/transport/remove/1"))
                .andExpect(status().isNoContent());

        verify(transportService, times(1)).removeStudentFromRoute(1L);
    }

    @Test
    void getRouteStudents_ShouldReturnList() throws Exception {
        List<TransportStudentDTO> students = Arrays.asList(new TransportStudentDTO());
        when(transportService.getRouteStudents(1L)).thenReturn(students);

        mockMvc.perform(get("/api/transport/routes/1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(transportService, times(1)).getRouteStudents(1L);
    }

    @Test
    void updateBusLocation_ShouldReturnUpdatedRoute() throws Exception {
        when(transportService.updateBusLocation(eq(1L), eq(6.5244), eq(3.3792)))
                .thenReturn(routeResponse);

        mockMvc.perform(post("/api/transport/update-location/1")
                        .param("lat", "6.5244")
                        .param("lng", "3.3792"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(transportService, times(1)).updateBusLocation(eq(1L), eq(6.5244), eq(3.3792));
    }

    @Test
    void getBusLocation_ShouldReturnLocation() throws Exception {
        Map<String, Double> location = new HashMap<>();
        location.put("lat", 6.5244);
        location.put("lng", 3.3792);

        when(transportService.getBusLocation(1L)).thenReturn(location);

        mockMvc.perform(get("/api/transport/location/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(6.5244))
                .andExpect(jsonPath("$.lng").value(3.3792));

        verify(transportService, times(1)).getBusLocation(1L);
    }

    @Test
    void getTransportStatistics_ShouldReturnStatistics() throws Exception {
        TransportStatisticsDTO stats = new TransportStatisticsDTO(
                5L,   // totalRoutes
                4L,   // activeRoutes
                150L, // assignedStudents
                25L,  // unassignedStudents
                195L, // totalCapacity
                45L   // availableSlots
        );

        when(transportService.getTransportStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/transport/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRoutes").value(5))
                .andExpect(jsonPath("$.activeRoutes").value(4))
                .andExpect(jsonPath("$.assignedStudents").value(150))
                .andExpect(jsonPath("$.availableSlots").value(45));

        verify(transportService, times(1)).getTransportStatistics();
    }

    @Test
    void getStudentAssignedRoute_ShouldReturnRoute() throws Exception {
        when(transportService.getStudentAssignedRoute(1L)).thenReturn(routeResponse);

        mockMvc.perform(get("/api/transport/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(transportService, times(1)).getStudentAssignedRoute(1L);
    }

    @Test
    void createRoute_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        BusRouteRequestDTO invalidRequest = new BusRouteRequestDTO();

        mockMvc.perform(post("/api/transport/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transportService, never()).createRoute(any());
    }
}