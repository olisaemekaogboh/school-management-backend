package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByClassName(String className);

    Optional<SchoolClass> findByClassNameAndArm(String className, String arm);

    boolean existsByClassNameAndArm(String className, String arm);
    List<SchoolClass> findByCategory(SchoolClass.ClassCategory category);
    List<SchoolClass> findByClassNameOrderByArmAsc(String className);

    // ADD THIS
    List<SchoolClass> findByClassTeacherId(Long classTeacherId);
    @Query("SELECT DISTINCT c FROM SchoolClass c LEFT JOIN FETCH c.subjects")
    List<SchoolClass> findAllWithSubjects();
}