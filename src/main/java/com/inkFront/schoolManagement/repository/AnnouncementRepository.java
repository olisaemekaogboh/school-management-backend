// src/main/java/com/inkFront/schoolManagement/repository/AnnouncementRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // =========================
    // EXISTING METHODS (yours)
    // =========================
    List<Announcement> findByActiveTrueOrderByPriorityDescCreatedAtDesc();

    List<Announcement> findByTypeAndActiveTrueOrderByCreatedAtDesc(Announcement.AnnouncementType type);

    @Query("""
        SELECT a
        FROM Announcement a
        WHERE a.active = true
          AND a.startDate <= :date
          AND a.endDate >= :date
        ORDER BY a.priority DESC
    """)
    List<Announcement> findActiveAnnouncementsForDate(@Param("date") LocalDate date);

    @Query("""
        SELECT a
        FROM Announcement a
        WHERE a.active = true
          AND :audience MEMBER OF a.audience
        ORDER BY a.priority DESC
    """)
    List<Announcement> findByAudience(@Param("audience") Announcement.Audience audience);

    List<Announcement> findByEventDateAfterOrderByEventDateAsc(LocalDate date);

    List<Announcement> findByFeeDueDateAfterOrderByFeeDueDateAsc(LocalDate date);

    // =========================
// NEW: FETCH audience to avoid LazyInitializationException
// =========================

    @Query("""
    select distinct a
    from Announcement a
    join fetch a.audience aud
    where a.active = true
    order by a.priority desc, a.createdAt desc
""")
    List<Announcement> findAllActiveWithAudience();

    @Query("""
    select a
    from Announcement a
    join fetch a.audience aud
    where a.id = :id
""")
    Optional<Announcement> findByIdWithAudience(@Param("id") Long id);

    @Query("""
    select distinct a
    from Announcement a
    join fetch a.audience aud
    where a.active = true and a.type = :type
    order by a.createdAt desc
""")
    List<Announcement> findByTypeActiveWithAudience(@Param("type") Announcement.AnnouncementType type);

    @Query("""
    select distinct a
    from Announcement a
    join fetch a.audience aud
    where a.active = true and :audience member of a.audience
    order by a.priority desc, a.createdAt desc
""")
    List<Announcement> findByAudienceActiveWithAudience(@Param("audience") Announcement.Audience audience);

    @Query("""
    select distinct a
    from Announcement a
    join fetch a.audience aud
    where a.eventDate > :date
    order by a.eventDate asc
""")
    List<Announcement> findByEventDateAfterWithAudience(@Param("date") LocalDate date);

    @Query("""
    select distinct a
    from Announcement a
    join fetch a.audience aud
    where a.feeDueDate > :date
    order by a.feeDueDate asc
""")
    List<Announcement> findByFeeDueDateAfterWithAudience(@Param("date") LocalDate date);
}