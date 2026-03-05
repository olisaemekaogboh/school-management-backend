package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByAnnouncementId(Long announcementId);

}