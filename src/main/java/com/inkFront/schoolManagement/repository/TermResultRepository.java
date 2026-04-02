package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermResultRepository extends JpaRepository<TermResult, Long> {

    @EntityGraph(attributePaths = {"student", "student.schoolClass", "subjectResults", "subjectResults.subject"})
    Optional<TermResult> findByStudentAndSessionAndTerm(
            Student student,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass", "subjectResults", "subjectResults.subject"})
    Optional<TermResult> findByStudentIdAndSessionAndTerm(
            Long studentId,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<TermResult> findBySessionAndTermOrderByAverageDesc(
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<TermResult> findByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
            Long classId,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<TermResult> findByStudent_SchoolClass_ClassNameAndSessionAndTermOrderByAverageDesc(
            String className,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<TermResult> findByStudent_SchoolClass_ClassNameAndStudent_SchoolClass_ArmAndSessionAndTermOrderByAverageDesc(
            String className,
            String arm,
            String session,
            Result.Term term
    );

    @Query("""
        SELECT DISTINCT tr
        FROM TermResult tr
        JOIN FETCH tr.student s
        LEFT JOIN FETCH s.schoolClass
        LEFT JOIN FETCH tr.subjectResults sr
        LEFT JOIN FETCH sr.subject
        WHERE tr.student = :student
          AND tr.session = :session
          AND tr.term = :term
    """)
    Optional<TermResult> findDetailedByStudentAndSessionAndTerm(
            @Param("student") Student student,
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT tr
        FROM TermResult tr
        JOIN FETCH tr.student s
        LEFT JOIN FETCH s.schoolClass
        WHERE s.schoolClass.id = :classId
          AND tr.session = :session
          AND tr.term = :term
        ORDER BY tr.average DESC
    """)
    List<TermResult> findDetailedByStudent_SchoolClass_IdAndSessionAndTermOrderByAverageDesc(
            @Param("classId") Long classId,
            @Param("session") String session,
            @Param("term") Result.Term term
    );
}