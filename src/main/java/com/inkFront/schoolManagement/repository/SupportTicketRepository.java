package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SupportTicket;
import com.inkFront.schoolManagement.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"createdBy", "assignedAdmin"})
    List<SupportTicket> findByCreatedByOrderByUpdatedAtDesc(User user);

    @EntityGraph(attributePaths = {"createdBy", "assignedAdmin"})
    List<SupportTicket> findAllByOrderByUpdatedAtDesc();

    boolean existsByTicketNumber(String ticketNumber);
    List<SupportTicket> findByCreatedBy_Id(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE SupportTicket t
    SET t.assignedAdmin = null
    WHERE t.assignedAdmin IS NOT NULL
      AND t.assignedAdmin.id = :userId
""")
    int clearAssignedAdmin(@Param("userId") Long userId);
}