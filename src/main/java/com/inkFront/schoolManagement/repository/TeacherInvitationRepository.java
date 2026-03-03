// src/main/java/com/inkFront/schoolManagement/repository/TeacherInvitationRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.TeacherInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherInvitationRepository extends JpaRepository<TeacherInvitation, Long> {

    // Find by token
    Optional<TeacherInvitation> findByToken(String token);

    // Find by email
    Optional<TeacherInvitation> findByEmail(String email);

    // Find pending invitations (not used and not expired)
    List<TeacherInvitation> findByUsedFalseAndExpiryDateAfter(LocalDateTime now);

    // Find expired invitations
    List<TeacherInvitation> findByUsedFalseAndExpiryDateBefore(LocalDateTime now);

    // Delete expired invitations
    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(LocalDateTime date);

    // Check if email has pending invitation
    boolean existsByEmailAndUsedFalseAndExpiryDateAfter(String email, LocalDateTime now);

    // Count pending invitations
    long countByUsedFalseAndExpiryDateAfter(LocalDateTime now);

    // Find by email and not used
    Optional<TeacherInvitation> findByEmailAndUsedFalse(String email);

    // Delete by email
    @Modifying
    @Transactional
    void deleteByEmail(String email);

    // Find invitations created between dates
    List<TeacherInvitation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Find invitations by status
    @Query("SELECT i FROM TeacherInvitation i WHERE i.used = :used AND i.expiryDate > :now")
    List<TeacherInvitation> findByUsedAndExpiryAfter(@Param("used") boolean used, @Param("now") LocalDateTime now);
}