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

    Optional<SchoolClass> findByClassNameAndArm(String className, String arm);

    boolean existsByClassNameAndArm(String className, String arm);

    List<SchoolClass> findByCategory(SchoolClass.ClassCategory category);

    List<SchoolClass> findByClassNameOrderByArmAsc(String className);

    List<SchoolClass> findByClassTeacherId(Long classTeacherId);

    @Query("""
        SELECT DISTINCT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.id = :id
    """)
    Optional<SchoolClass> findByIdWithTeacher(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.classTeacher.id = :teacherId
        ORDER BY c.className ASC, c.arm ASC
    """)
    List<SchoolClass> findByClassTeacherIdWithTeacher(@Param("teacherId") Long teacherId);

    @Query("""
        SELECT DISTINCT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        ORDER BY c.className ASC, c.arm ASC
    """)
    List<SchoolClass> findAllWithTeacher();

    @Query("""
        SELECT DISTINCT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.category = :category
        ORDER BY c.className ASC, c.arm ASC
    """)
    List<SchoolClass> findByCategoryWithTeacher(@Param("category") SchoolClass.ClassCategory category);
}