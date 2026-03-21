package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Event;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByIsActiveTrueOrderByEventDateAsc();

    List<Event> findByEventDateAfterAndIsActiveTrueOrderByEventDateAsc(LocalDate date);

    List<Event> findByEventDateBetweenAndIsActiveTrueOrderByEventDateAsc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.eventDate >= :today ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEvents(@Param("today") LocalDate today);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.eventDate < :today ORDER BY e.eventDate DESC")
    List<Event> findPastEvents(@Param("today") LocalDate today);
}
