package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByNameIgnoreCase(String name);

    Optional<Subject> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    List<Subject> findByActiveTrue();
}