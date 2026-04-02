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

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolClass",
            "subjectAnnualTotals",
            "subjectAverages",
            "firstTermSubjectScores",
            "secondTermSubjectScores",
            "thirdTermSubjectScores"
    })
    Optional<SessionResult> findByStudentAndSession(Student student, String session);

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolClass",
            "subjectAnnualTotals",
            "subjectAverages",
            "firstTermSubjectScores",
            "secondTermSubjectScores",
            "thirdTermSubjectScores"
    })
    List<SessionResult> findByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(
            Long classId,
            String session
    );

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolClass",
            "subjectAnnualTotals",
            "subjectAverages",
            "firstTermSubjectScores",
            "secondTermSubjectScores",
            "thirdTermSubjectScores"
    })
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

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolClass",
            "subjectAnnualTotals",
            "subjectAverages",
            "firstTermSubjectScores",
            "secondTermSubjectScores",
            "thirdTermSubjectScores"
    })
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

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolClass",
            "subjectAnnualTotals",
            "subjectAverages",
            "firstTermSubjectScores",
            "secondTermSubjectScores",
            "thirdTermSubjectScores"
    })
    @Query("""
        SELECT sr
        FROM SessionResult sr
        WHERE sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findBySessionOrderByAnnualAverageDesc(@Param("session") String session);

    @Query("""
        SELECT sr
        FROM SessionResult sr
        JOIN FETCH sr.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE sr.student = :student
          AND sr.session = :session
    """)
    Optional<SessionResult> findDetailedByStudentAndSession(
            @Param("student") Student student,
            @Param("session") String session
    );

    @Query("""
        SELECT sr
        FROM SessionResult sr
        JOIN FETCH sr.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE s.schoolClass.id = :classId
          AND sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findDetailedByStudent_SchoolClass_IdAndSessionOrderByAnnualAverageDesc(
            @Param("classId") Long classId,
            @Param("session") String session
    );

    @Query("""
        SELECT sr
        FROM SessionResult sr
        JOIN FETCH sr.student s
        LEFT JOIN FETCH s.schoolClass sc
        WHERE UPPER(REPLACE(TRIM(sc.className), ' ', '')) =
              UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findDetailedByClassAndSessionOrderByAnnualAverageDesc(
            @Param("className") String className,
            @Param("session") String session
    );

    @Query("""
        SELECT sr
        FROM SessionResult sr
        JOIN FETCH sr.student s
        LEFT JOIN FETCH s.schoolClass sc
        WHERE UPPER(REPLACE(TRIM(sc.className), ' ', '')) =
              UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND UPPER(REPLACE(TRIM(sc.arm), ' ', '')) =
              UPPER(REPLACE(TRIM(:arm), ' ', ''))
          AND sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findDetailedByClassAndArmAndSessionOrderByAnnualAverageDesc(
            @Param("className") String className,
            @Param("arm") String arm,
            @Param("session") String session
    );

    @Query("""
        SELECT sr
        FROM SessionResult sr
        JOIN FETCH sr.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE sr.session = :session
        ORDER BY sr.annualAverage DESC
    """)
    List<SessionResult> findDetailedBySessionOrderByAnnualAverageDesc(
            @Param("session") String session
    );

    @Query("""
        SELECT sr
        FROM SessionResult sr
        JOIN FETCH sr.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE sr.id = :id
    """)
    Optional<SessionResult> findDetailedById(@Param("id") Long id);

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