package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmail(String email);

    Optional<Teacher> findByTeacherId(String teacherId);

    Optional<Teacher> findByEmployeeId(String employeeId);

    @EntityGraph(attributePaths = {"subjects", "qualifications", "user"})
    @Query("SELECT t FROM Teacher t WHERE t.id = :id")
    Optional<Teacher> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"subjects", "qualifications", "user"})
    @Query("SELECT t FROM Teacher t WHERE t.user.id = :userId")
    Optional<Teacher> findByUserIdWithDetails(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"subjects", "qualifications", "user"})
    @Query("SELECT t FROM Teacher t WHERE t.teacherId = :teacherId")
    Optional<Teacher> findByTeacherIdWithDetails(@Param("teacherId") String teacherId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT t FROM Teacher t")
    List<Teacher> findAllWithDetails();

    boolean existsByEmail(String email);

    boolean existsByTeacherId(String teacherId);

    boolean existsByEmployeeId(String employeeId);

    List<Teacher> findByStatus(Teacher.TeacherStatus status);

    long countByStatus(Teacher.TeacherStatus status);

    List<Teacher> findByDepartment(String department);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.department = :department")
    long countByDepartment(@Param("department") String department);

    List<Teacher> findByEmploymentType(Teacher.EmploymentType employmentType);

    @Query("SELECT t FROM Teacher t WHERE :subject MEMBER OF t.subjects")
    List<Teacher> findBySubjectsContaining(@Param("subject") String subject);

    @Query("""
        SELECT t
        FROM Teacher t
        WHERE LOWER(t.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(t.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(t.email) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(t.teacherId) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(t.employeeId) LIKE LOWER(CONCAT('%', :term, '%'))
        ORDER BY t.lastName ASC, t.firstName ASC
    """)
    List<Teacher> searchTeachers(@Param("term") String term);

    Page<Teacher> findAll(Pageable pageable);

    Page<Teacher> findByStatus(Teacher.TeacherStatus status, Pageable pageable);

    Page<Teacher> findByDepartment(String department, Pageable pageable);

    List<Teacher> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Teacher t WHERE t.createdAt >= :date ORDER BY t.createdAt DESC")
    List<Teacher> findTeachersSince(@Param("date") LocalDateTime date);

    List<Teacher> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );

    @Query("""
        SELECT t
        FROM Teacher t
        WHERE t.createdAt BETWEEN :startDate AND :endDate
        ORDER BY t.createdAt DESC
    """)
    List<Teacher> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Teacher t WHERE t.user IS NULL ORDER BY t.lastName ASC, t.firstName ASC")
    List<Teacher> findTeachersWithoutUserAccount();

    @Query("SELECT t.teacherId FROM Teacher t WHERE t.teacherId IS NOT NULL")
    List<String> findAllTeacherIds();

    @Query("SELECT t.employeeId FROM Teacher t WHERE t.employeeId IS NOT NULL")
    List<String> findAllEmployeeIds();
}