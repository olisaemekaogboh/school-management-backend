package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    @EntityGraph(attributePaths = {"student", "student.schoolClass", "subject", "termResult"})
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

    @EntityGraph(attributePaths = {"student", "student.schoolClass", "subject", "termResult"})
    @Query("""
        SELECT r
        FROM Result r
        WHERE r.student.schoolClass.id = :classId
          AND r.session = :session
          AND r.term = :term
        ORDER BY r.student.lastName ASC, r.student.firstName ASC, r.subject.name ASC
    """)
    List<Result> findByClassIdAndSessionAndTerm(
            @Param("classId") Long classId,
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
        WHERE r.student.schoolClass.id = :classId
          AND r.session = :session
          AND r.term = :term
        GROUP BY r.student
        ORDER BY AVG(r.total) DESC
    """)
    List<Object[]> getClassRankingByClassId(
            @Param("classId") Long classId,
            @Param("session") String session,
            @Param("term") Result.Term term
    );
}