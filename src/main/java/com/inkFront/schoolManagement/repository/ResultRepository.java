package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentAndSessionAndTerm(
            Student student,
            String session,
            Result.Term term
    );

    Optional<Result> findByStudentAndSubjectAndSessionAndTerm(
            Student student,
            Subject subject,
            String session,
            Result.Term term
    );

    @Query("""
        SELECT r
        FROM Result r
        WHERE r.student.studentClass = :className
          AND r.session = :session
          AND r.term = :term
    """)
    List<Result> findByClassAndSessionAndTerm(
            @Param("className") String className,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r
        FROM Result r
        WHERE r.student.studentClass = :className
          AND r.student.classArm = :arm
          AND r.session = :session
          AND r.term = :term
    """)
    List<Result> findByClassAndArmAndSessionAndTerm(
            @Param("className") String className,
            @Param("arm") String arm,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r
        FROM Result r
        WHERE UPPER(REPLACE(TRIM(r.student.studentClass), ' ', '')) = UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND r.session = :session
          AND r.term = :term
    """)
    List<Result> findByClassAndSessionAndTermNormalized(
            @Param("className") String className,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r
        FROM Result r
        WHERE UPPER(REPLACE(TRIM(r.student.studentClass), ' ', '')) = UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND UPPER(REPLACE(TRIM(r.student.classArm), ' ', '')) = UPPER(REPLACE(TRIM(:arm), ' ', ''))
          AND r.session = :session
          AND r.term = :term
    """)
    List<Result> findByClassAndArmAndSessionAndTermNormalized(
            @Param("className") String className,
            @Param("arm") String arm,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r.student, AVG(r.total)
        FROM Result r
        WHERE r.session = :session
          AND r.term = :term
        GROUP BY r.student
        ORDER BY AVG(r.total) DESC
    """)
    List<Object[]> getSchoolRanking(
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r.student, AVG(r.total)
        FROM Result r
        WHERE r.student.studentClass = :className
          AND r.session = :session
          AND r.term = :term
        GROUP BY r.student
        ORDER BY AVG(r.total) DESC
    """)
    List<Object[]> getClassRanking(
            @Param("className") String className,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r.student, AVG(r.total)
        FROM Result r
        WHERE r.student.studentClass = :className
          AND r.student.classArm = :arm
          AND r.session = :session
          AND r.term = :term
        GROUP BY r.student
        ORDER BY AVG(r.total) DESC
    """)
    List<Object[]> getArmRanking(
            @Param("className") String className,
            @Param("arm") String arm,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r.student, AVG(r.total)
        FROM Result r
        WHERE UPPER(REPLACE(TRIM(r.student.studentClass), ' ', '')) = UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND r.session = :session
          AND r.term = :term
        GROUP BY r.student
        ORDER BY AVG(r.total) DESC
    """)
    List<Object[]> getClassRankingNormalized(
            @Param("className") String className,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT r.student, AVG(r.total)
        FROM Result r
        WHERE UPPER(REPLACE(TRIM(r.student.studentClass), ' ', '')) = UPPER(REPLACE(TRIM(:className), ' ', ''))
          AND UPPER(REPLACE(TRIM(r.student.classArm), ' ', '')) = UPPER(REPLACE(TRIM(:arm), ' ', ''))
          AND r.session = :session
          AND r.term = :term
        GROUP BY r.student
        ORDER BY AVG(r.total) DESC
    """)
    List<Object[]> getArmRankingNormalized(
            @Param("className") String className,
            @Param("arm") String arm,
            @Param("session") String session,
            @Param("term") Result.Term term
    );
}