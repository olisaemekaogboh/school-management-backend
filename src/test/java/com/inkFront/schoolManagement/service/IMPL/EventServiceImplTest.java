package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.EventDTO;
import com.inkFront.schoolManagement.model.Event;
import com.inkFront.schoolManagement.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event testEvent;
    private EventDTO testEventDTO;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitle("School Sports Day");
        testEvent.setDescription("Annual sports competition"); // Changed from setMessage to setDescription
        testEvent.setEventDate(LocalDate.of(2024, 5, 15));
        testEvent.setEventTime("09:00"); // Changed from LocalTime to String
        testEvent.setLocation("School Field");
        testEvent.setOrganizer("Sports Department");
        testEvent.setIsActive(true);
        testEvent.setCreatedAt(LocalDateTime.now());

        testEventDTO = new EventDTO();
        testEventDTO.setId(1L);
        testEventDTO.setTitle("School Sports Day");
        testEventDTO.setDescription("Annual sports competition"); // Changed from setMessage to setDescription
        testEventDTO.setEventDate(LocalDate.of(2024, 5, 15));
        testEventDTO.setEventTime("09:00"); // Changed from LocalTime to String
        testEventDTO.setLocation("School Field");
        testEventDTO.setOrganizer("Sports Department");
        testEventDTO.setIsActive(true);
    }

    @Test
    void createEvent_ShouldCreateAndReturnEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventDTO result = eventService.createEvent(testEventDTO);

        assertNotNull(result);
        assertEquals("School Sports Day", result.getTitle());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEvent_ShouldUpdateAndReturnEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventDTO result = eventService.updateEvent(1L, testEventDTO);

        assertNotNull(result);
        verify(eventRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEvent_WithNonExistentId_ShouldThrowException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            eventService.updateEvent(999L, testEventDTO);
        });
    }

    @Test
    void deleteEvent_ShouldSoftDelete() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.deleteEvent(1L);

        assertFalse(testEvent.getIsActive());
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    void deleteEvent_WithNonExistentId_ShouldThrowException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            eventService.deleteEvent(999L);
        });
    }

    @Test
    void getEventById_ShouldReturnEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        EventDTO result = eventService.getEventById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    void getEventById_WithNonExistentId_ShouldThrowException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            eventService.getEventById(999L);
        });
    }

    @Test
    void getAllActiveEvents_ShouldReturnList() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByIsActiveTrueOrderByEventDateAsc()).thenReturn(events);

        List<EventDTO> result = eventService.getAllActiveEvents();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByIsActiveTrueOrderByEventDateAsc();
    }

    @Test
    void getUpcomingEvents_ShouldReturnList() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class))).thenReturn(events);

        List<EventDTO> result = eventService.getUpcomingEvents();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findUpcomingEvents(any(LocalDate.class));
    }

    @Test
    void getEventsByDateRange_ShouldReturnFilteredList() {
        LocalDate start = LocalDate.of(2024, 5, 1);
        LocalDate end = LocalDate.of(2024, 5, 31);
        List<Event> events = Arrays.asList(testEvent);

        when(eventRepository.findByEventDateBetweenAndIsActiveTrueOrderByEventDateAsc(start, end))
                .thenReturn(events);

        List<EventDTO> result = eventService.getEventsByDateRange(start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1))
                .findByEventDateBetweenAndIsActiveTrueOrderByEventDateAsc(start, end);
    }

    @Test
    void convertToDTO_ShouldMapCorrectly() {
        EventDTO dto = ReflectionTestUtils.invokeMethod(eventService, "convertToDTO", testEvent);

        assertNotNull(dto);
        assertEquals(testEvent.getId(), dto.getId());
        assertEquals(testEvent.getTitle(), dto.getTitle());
        assertEquals(testEvent.getDescription(), dto.getDescription());
    }
}