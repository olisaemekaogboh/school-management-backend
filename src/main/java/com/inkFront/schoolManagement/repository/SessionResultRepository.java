package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.SessionResult;
import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionResultRepository extends JpaRepository<SessionResult, Long> {

    @EntityGraph(attributePaths = {"student", "subjectAnnualTotals", "subjectAverages"})
    Optional<SessionResult> findByStudentAndSession(Student student, String session);

    @EntityGraph(attributePaths = {"student", "subjectAnnualTotals", "subjectAverages"})
    @Query("""
        SELECT sr
        FROM SessionResult sr
        WHERE UPPER(REPLACE(TRIM(sr.student.schoolClass.className), ' ', '')) =
              UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findByClassAndSessionOrderByAnnualAverageDesc(
            @Param("className") String className,
            @Param("session") String session
    );

    @EntityGraph(attributePaths = {"student", "subjectAnnualTotals", "subjectAverages"})
    @Query("""
        SELECT sr
        FROM SessionResult sr
        WHERE UPPER(REPLACE(TRIM(sr.student.schoolClass.className), ' ', '')) =
              UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND UPPER(REPLACE(TRIM(sr.student.schoolClass.arm), ' ', '')) =
              UPPER(REPLACE(TRIM(:arm), ' ', ''))
          AND sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findByClassAndArmAndSessionOrderByAnnualAverageDesc(
            @Param("className") String className,
            @Param("arm") String arm,
            @Param("session") String session
    );

    @EntityGraph(attributePaths = {"student", "subjectAnnualTotals", "subjectAverages"})
    @Query("""
        SELECT sr
        FROM SessionResult sr
        WHERE sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findBySessionOrderByAnnualAverageDesc(@Param("session") String session);

    @Query("""
        SELECT sr.student.schoolClass.className, AVG(sr.annualAverage)
        FROM SessionResult sr
        WHERE sr.session = :session
        GROUP BY sr.student.schoolClass.className
    """)
    List<Object[]> getClassAverageBySession(@Param("session") String session);

    @Query("""
        SELECT COUNT(sr)
        FROM SessionResult sr
        WHERE sr.promoted = true
          AND sr.session = :session
    """)
    long countPromotedStudents(@Param("session") String session);

    @Query("""
        SELECT COUNT(sr)
        FROM SessionResult sr
        WHERE sr.promoted = false
          AND sr.session = :session
    """)
    long countRetainedStudents(@Param("session") String session);
}