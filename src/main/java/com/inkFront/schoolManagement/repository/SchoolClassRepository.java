package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByClassCode(String classCode);

    boolean existsByClassName(String className);

}