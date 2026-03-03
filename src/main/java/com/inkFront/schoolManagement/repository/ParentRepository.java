// src/main/java/com/inkFront/schoolManagement/repository/ParentRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    // Find by email (exact match)
    Optional<Parent> findByEmail(String email);

    // Find by email case-insensitive
    @Query("SELECT p FROM Parent p WHERE LOWER(p.email) = LOWER(:email)")
    Optional<Parent> findByEmailIgnoreCase(@Param("email") String email);

    // Find by phone number
    Optional<Parent> findByPhoneNumber(String phoneNumber);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find parents with their wards (eager loading)
    @Query("SELECT p FROM Parent p LEFT JOIN FETCH p.wards WHERE p.id = :id")
    Optional<Parent> findByIdWithWards(@Param("id") Long id);

    // Search parents by name or email
    @Query("SELECT p FROM Parent p WHERE " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Parent> searchParents(@Param("searchTerm") String searchTerm);

    // Find parents with no wards
    @Query("SELECT p FROM Parent p WHERE p.wards IS EMPTY")
    List<Parent> findParentsWithNoWards();
}