// src/main/java/com/inkFront/schoolManagement/repository/AcademicSessionRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    Optional<AcademicSession> findByName(String name);
    Optional<AcademicSession> findByActiveTrue();
    boolean existsByName(String name);
}