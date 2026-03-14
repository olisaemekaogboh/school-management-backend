package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByClassName(String className);

    Optional<SchoolClass> findByClassNameAndArm(String className, String arm);

    boolean existsByClassNameAndArm(String className, String arm);

    List<SchoolClass> findByCategory(SchoolClass.ClassCategory category);

    List<SchoolClass> findByCategoryAndArm(SchoolClass.ClassCategory category, String arm);

    List<SchoolClass> findByClassNameOrderByArmAsc(String className);

    List<SchoolClass> findByClassTeacherId(Long classTeacherId);

    @Query("""
        SELECT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.id = :id
    """)
    Optional<SchoolClass> findByIdWithTeacher(@Param("id") Long id);

    @Query("""
        SELECT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        ORDER BY c.className ASC, c.arm ASC
    """)
    List<SchoolClass> findAllWithTeacher();

    @Query("""
        SELECT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.classTeacher.id = :teacherId
        ORDER BY c.className ASC, c.arm ASC
    """)
    List<SchoolClass> findByClassTeacherIdWithTeacher(@Param("teacherId") Long teacherId);

    @Query("""
        SELECT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.category = :category
        ORDER BY c.className ASC, c.arm ASC
    """)
    List<SchoolClass> findByCategoryWithTeacher(@Param("category") SchoolClass.ClassCategory category);

    @Query("""
        SELECT c
        FROM SchoolClass c
        WHERE UPPER(REPLACE(TRIM(c.className), ' ', '')) = UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND UPPER(REPLACE(TRIM(c.arm), ' ', '')) = UPPER(REPLACE(TRIM(:arm), ' ', ''))
    """)
    Optional<SchoolClass> findByClassNameAndArmNormalized(
            @Param("className") String className,
            @Param("arm") String arm
    );
}