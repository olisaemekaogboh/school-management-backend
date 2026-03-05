// src/main/java/com/inkFront/schoolManagement/repository/AcademicSessionRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<AcademicSession> findFirstByActiveTrue();

    @Modifying
    @Query("UPDATE AcademicSession s SET s.active = false WHERE s.id <> :activeId AND s.active = true")
    int deactivateOthers(@Param("activeId") Long activeId);
}