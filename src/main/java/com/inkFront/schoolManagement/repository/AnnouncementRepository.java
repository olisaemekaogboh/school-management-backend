// src/main/java/com/inkFront/schoolManagement/repository/AnnouncementRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByActiveTrueOrderByPriorityDescCreatedAtDesc();

    List<Announcement> findByTypeAndActiveTrueOrderByCreatedAtDesc(Announcement.AnnouncementType type);

    @Query("SELECT a FROM Announcement a WHERE a.active = true AND a.startDate <= :date AND a.endDate >= :date ORDER BY a.priority DESC")
    List<Announcement> findActiveAnnouncementsForDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM Announcement a WHERE a.active = true AND :audience MEMBER OF a.audience ORDER BY a.priority DESC")
    List<Announcement> findByAudience(@Param("audience") Announcement.Audience audience);

    List<Announcement> findByEventDateAfterOrderByEventDateAsc(LocalDate date);

    List<Announcement> findByFeeDueDateAfterOrderByFeeDueDateAsc(LocalDate date);

}