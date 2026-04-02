package com.inkFront.schoolManagement.controllers;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkFront.schoolManagement.dto.EventDTO;
import com.inkFront.schoolManagement.exception.GlobalExceptionHandler;
import com.inkFront.schoolManagement.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private ObjectMapper objectMapper;
    private EventDTO testEventDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testEventDTO = new EventDTO();
        testEventDTO.setId(1L);
        testEventDTO.setTitle("School Sports Day");
        testEventDTO.setDescription("Annual sports competition");
        testEventDTO.setEventDate(LocalDate.of(2024, 5, 15));
        testEventDTO.setEventTime("09:00");  // eventTime is String
        testEventDTO.setLocation("School Sports Field");
        testEventDTO.setOrganizer("Sports Department");
        testEventDTO.setIsActive(true);  // Use setIsActive, not setActive
    }

    @Test
    void getAllEvents_ShouldReturnList() throws Exception {
        List<EventDTO> events = Arrays.asList(testEventDTO);
        when(eventService.getAllActiveEvents()).thenReturn(events);

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("School Sports Day"));

        verify(eventService, times(1)).getAllActiveEvents();
    }

    @Test
    void getUpcomingEvents_ShouldReturnUpcomingEvents() throws Exception {
        List<EventDTO> upcomingEvents = Arrays.asList(testEventDTO);
        when(eventService.getUpcomingEvents()).thenReturn(upcomingEvents);

        mockMvc.perform(get("/api/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getUpcomingEvents();
    }

    @Test
    void getEventsByDateRange_ShouldReturnFilteredEvents() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 5, 1);
        LocalDate endDate = LocalDate.of(2024, 5, 31);
        List<EventDTO> events = Arrays.asList(testEventDTO);

        when(eventService.getEventsByDateRange(eq(startDate), eq(endDate)))
                .thenReturn(events);

        mockMvc.perform(get("/api/events/date-range")
                        .param("startDate", "2024-05-01")
                        .param("endDate", "2024-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsByDateRange(eq(startDate), eq(endDate));
    }

    @Test
    void getEventById_ShouldReturnEvent() throws Exception {
        when(eventService.getEventById(1L)).thenReturn(testEventDTO);

        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("School Sports Day"));

        verify(eventService, times(1)).getEventById(1L);
    }

    @Test
    void createEvent_ShouldReturnCreatedEvent() throws Exception {
        when(eventService.createEvent(any(EventDTO.class))).thenReturn(testEventDTO);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEventDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("School Sports Day"));

        verify(eventService, times(1)).createEvent(any(EventDTO.class));
    }

    @Test
    void updateEvent_ShouldReturnUpdatedEvent() throws Exception {
        when(eventService.updateEvent(eq(1L), any(EventDTO.class))).thenReturn(testEventDTO);

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEventDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(eventService, times(1)).updateEvent(eq(1L), any(EventDTO.class));
    }

    @Test
    void deleteEvent_ShouldReturnNoContent() throws Exception {
        doNothing().when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isNoContent());

        verify(eventService, times(1)).deleteEvent(1L);
    }

    @Test
    void getEventsByDateRange_WithInvalidDates_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/date-range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2024-05-31"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsByDateRange(any(), any());
    }

    @Test
    void getEventById_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(eventService.getEventById(999L))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).getEventById(999L);
    }
}
