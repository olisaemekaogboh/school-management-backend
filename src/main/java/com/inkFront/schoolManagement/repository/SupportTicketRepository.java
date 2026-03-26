package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SupportTicket;
import com.inkFront.schoolManagement.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"createdBy", "assignedAdmin"})
    List<SupportTicket> findByCreatedByOrderByUpdatedAtDesc(User user);

    @EntityGraph(attributePaths = {"createdBy", "assignedAdmin"})
    List<SupportTicket> findAllByOrderByUpdatedAtDesc();

    boolean existsByTicketNumber(String ticketNumber);
}