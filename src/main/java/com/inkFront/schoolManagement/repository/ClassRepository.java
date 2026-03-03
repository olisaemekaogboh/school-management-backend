// src/main/java/com/inkFront/schoolManagement/repository/ClassRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, Long> {
    Optional<SchoolClass> findByClassName(String className);
    Optional<SchoolClass> findByClassCode(String classCode);
    List<SchoolClass> findByCategory(SchoolClass.ClassCategory category);

    @Query("SELECT c FROM SchoolClass c WHERE c.classTeacher.id = :teacherId")
    List<SchoolClass> findByClassTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.studentClass = :className")
    int countStudentsInClass(@Param("className") String className);
}