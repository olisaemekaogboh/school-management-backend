package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Attendance;
import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    Optional<Attendance> findByStudentAndDateAndSessionAndTerm(
            Student student,
            LocalDate date,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Attendance> findByStudentAndSessionAndTermOrderByDateAsc(
            Student student,
            String session,
            Result.Term term
    );

    @Query("""
        SELECT COUNT(a)
        FROM Attendance a
        WHERE a.student = :student
          AND a.session = :session
          AND a.term = :term
          AND a.status = :status
    """)
    long countByStudentAndSessionAndTermAndStatus(
            @Param("student") Student student,
            @Param("session") String session,
            @Param("term") Result.Term term,
            @Param("status") Attendance.AttendanceStatus status
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Attendance> findByStudent_SchoolClass_IdAndDateAndSessionAndTerm(
            Long classId,
            LocalDate date,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Attendance> findByStudent_SchoolClass_IdAndDateAndSessionAndTermOrderByStudent_LastNameAscStudent_FirstNameAsc(
            Long classId,
            LocalDate date,
            String session,
            Result.Term term
    );

    @EntityGraph(attributePaths = {"student", "student.schoolClass"})
    List<Attendance> findByStudent_SchoolClass_IdAndSessionAndTermOrderByDateAscStudent_LastNameAscStudent_FirstNameAsc(
            Long classId,
            String session,
            Result.Term term
    );

    @Query("""
        SELECT DISTINCT a.date
        FROM Attendance a
        WHERE a.session = :session
          AND a.term = :term
        ORDER BY a.date
    """)
    List<LocalDate> findDistinctDatesBySessionAndTerm(
            @Param("session") String session,
            @Param("term") Result.Term term
    );

    @Query("""
        SELECT COUNT(a)
        FROM Attendance a
        WHERE a.student.schoolClass.id = :classId
          AND a.session = :session
          AND a.term = :term
          AND a.status = :status
    """)
    long countByClassIdAndSessionAndTermAndStatus(
            @Param("classId") Long classId,
            @Param("session") String session,
            @Param("term") Result.Term term,
            @Param("status") Attendance.AttendanceStatus status
    );
}