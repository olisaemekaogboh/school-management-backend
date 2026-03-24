// src/main/java/com/inkFront/schoolManagement/repository/EmailQueueRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.EmailQueue;
import com.inkFront.schoolManagement.model.EmailQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long> {

    List<EmailQueue> findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            List<EmailQueueStatus> statuses,
            LocalDateTime nextRetryAt
    );

    List<EmailQueue> findByAnnouncementId(Long announcementId);

    List<EmailQueue> findByStatusOrderByCreatedAtDesc(EmailQueueStatus status);

    List<EmailQueue> findAllByOrderByCreatedAtDesc();

    long countByStatus(EmailQueueStatus status);
}