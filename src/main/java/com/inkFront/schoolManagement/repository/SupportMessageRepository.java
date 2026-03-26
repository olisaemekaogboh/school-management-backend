package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SupportMessage;
import com.inkFront.schoolManagement.model.SupportTicket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<SupportMessage> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);
}