// src/main/java/com/inkFront/schoolManagement/repository/SmsLogRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    // Find by exact status
    List<SmsLog> findByStatus(String status);

    // Count by status - Spring Data JPA magic!
    long countByStatus(String status);

    // Find by student
    List<SmsLog> findByStudentId(Long studentId);

    // Find by phone number
    List<SmsLog> findByParentPhone(String parentPhone);

    // Find stuck messages (pending for too long)
    @Query("SELECT s FROM SmsLog s WHERE s.status = 'PENDING' AND s.createdAt < :timeout")
    List<SmsLog> findStuckMessages(@Param("timeout") LocalDateTime timeout);

    // Find by announcement
    @Query("SELECT s FROM SmsLog s WHERE s.announcement.id = :announcementId ORDER BY s.sentAt DESC")
    List<SmsLog> findByAnnouncementId(@Param("announcementId") Long announcementId);

    // Count delivered for an announcement
    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.status = 'DELIVERED' AND s.announcement.id = :announcementId")
    long countDeliveredByAnnouncement(@Param("announcementId") Long announcementId);

    // Count failed for an announcement
    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.status = 'FAILED' AND s.announcement.id = :announcementId")
    long countFailedByAnnouncement(@Param("announcementId") Long announcementId);

    // Get delivery stats grouped by status
    @Query("SELECT s.status, COUNT(s) FROM SmsLog s WHERE s.announcement.id = :announcementId GROUP BY s.status")
    List<Object[]> getDeliveryStats(@Param("announcementId") Long announcementId);

    // Get overall statistics
    @Query("SELECT " +
            "SUM(CASE WHEN s.status = 'DELIVERED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.status = 'SENT' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.status = 'FAILED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.status = 'PENDING' THEN 1 ELSE 0 END), " +
            "COUNT(s) " +
            "FROM SmsLog s")
    List<Object[]> getOverallStats();

    // Find by date range
    @Query("SELECT s FROM SmsLog s WHERE s.sentAt BETWEEN :start AND :end ORDER BY s.sentAt DESC")
    List<SmsLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Find failed messages that need retry
    @Query("SELECT s FROM SmsLog s WHERE s.requiresFollowUp = true AND s.retryCount < 3")
    List<SmsLog> findMessagesToRetry();

}