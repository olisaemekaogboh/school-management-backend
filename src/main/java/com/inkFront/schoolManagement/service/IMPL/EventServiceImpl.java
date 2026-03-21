package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.EventDTO;
import com.inkFront.schoolManagement.model.Event;
import com.inkFront.schoolManagement.repository.EventRepository;
import com.inkFront.schoolManagement.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class EventServiceImpl implements EventService {

    @Autowired
    private EventRepository eventRepository;

    @Override
    public EventDTO createEvent(EventDTO eventDTO) {
        Event event = convertToEntity(eventDTO);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        event.setIsActive(true);

        Event savedEvent = eventRepository.save(event);
        return convertToDTO(savedEvent);
    }

    @Override
    public EventDTO updateEvent(Long id, EventDTO eventDTO) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        existingEvent.setTitle(eventDTO.getTitle());
        existingEvent.setDescription(eventDTO.getDescription());
        existingEvent.setEventDate(eventDTO.getEventDate());
        existingEvent.setEventTime(eventDTO.getEventTime());
        existingEvent.setLocation(eventDTO.getLocation());
        existingEvent.setImageUrl(eventDTO.getImageUrl());
        existingEvent.setOrganizer(eventDTO.getOrganizer());
        existingEvent.setUpdatedAt(LocalDateTime.now());

        Event updatedEvent = eventRepository.save(existingEvent);
        return convertToDTO(updatedEvent);
    }

    @Override
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        event.setIsActive(false);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    @Override
    public EventDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return convertToDTO(event);
    }

    @Override
    public List<EventDTO> getAllActiveEvents() {
        return eventRepository.findByIsActiveTrueOrderByEventDateAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDate.now())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        return eventRepository.findByEventDateBetweenAndIsActiveTrueOrderByEventDateAsc(startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private EventDTO convertToDTO(Event event) {
        return new EventDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getEventTime(),
                event.getLocation(),
                event.getImageUrl(),
                event.getOrganizer(),
                event.getIsActive()
        );
    }

    private Event convertToEntity(EventDTO eventDTO) {
        Event event = new Event();
        event.setTitle(eventDTO.getTitle());
        event.setDescription(eventDTO.getDescription());
        event.setEventDate(eventDTO.getEventDate());
        event.setEventTime(eventDTO.getEventTime());
        event.setLocation(eventDTO.getLocation());
        event.setImageUrl(eventDTO.getImageUrl());
        event.setOrganizer(eventDTO.getOrganizer());
        return event;
    }
}
