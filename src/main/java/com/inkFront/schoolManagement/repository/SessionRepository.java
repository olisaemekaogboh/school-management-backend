// src/main/java/com/inkFront/schoolManagement/repository/SessionRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<AcademicSession, Long> {

    Optional<AcademicSession> findBySessionName(String sessionName);

    Optional<AcademicSession> findByActiveTrue();

    boolean existsBySessionName(String sessionName);
}