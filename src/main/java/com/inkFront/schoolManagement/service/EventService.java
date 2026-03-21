package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.EventDTO;

import java.time.LocalDate;
import java.util.List;



public interface EventService {
    EventDTO createEvent(EventDTO eventDTO);
    EventDTO updateEvent(Long id, EventDTO eventDTO);
    void deleteEvent(Long id);
    EventDTO getEventById(Long id);
    List<EventDTO> getAllActiveEvents();
    List<EventDTO> getUpcomingEvents();
    List<EventDTO> getEventsByDateRange(LocalDate startDate, LocalDate endDate);
}