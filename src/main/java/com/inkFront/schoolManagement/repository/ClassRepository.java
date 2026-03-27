package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Teacher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByClassName(String className);

    Optional<SchoolClass> findByClassNameAndArm(String className, String arm);

    boolean existsByClassNameAndArm(String className, String arm);

    List<SchoolClass> findByCategory(SchoolClass.ClassCategory category);

    List<SchoolClass> findByCategoryAndArm(SchoolClass.ClassCategory category, String arm);

    List<SchoolClass> findByClassNameOrderByArmAsc(String className);

    List<SchoolClass> findByClassTeacherId(Long classTeacherId);

    @EntityGraph(attributePaths = {"classTeacher"})
    Optional<SchoolClass> findById(Long id);

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

    @Query("""
        SELECT c
        FROM SchoolClass c
        LEFT JOIN FETCH c.classTeacher
        WHERE c.id IN :ids
    """)
    List<SchoolClass> findAllByIdWithTeacher(@Param("ids") Collection<Long> ids);

    @Query("""
        SELECT c
        FROM SchoolClass c
        WHERE c.id IN :ids
    """)
    List<SchoolClass> findAllByIdIn(@Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClass c
        SET c.classTeacher = null
        WHERE c.classTeacher IS NOT NULL
          AND c.classTeacher.id = :teacherId
    """)
    int clearTeacherFromClasses(@Param("teacherId") Long teacherId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClass c
        SET c.classTeacher = :newTeacher
        WHERE c.classTeacher IS NOT NULL
          AND c.classTeacher.id = :oldTeacherId
    """)
    int reassignTeacherInClasses(@Param("oldTeacherId") Long oldTeacherId,
                                 @Param("newTeacher") Teacher newTeacher);
}