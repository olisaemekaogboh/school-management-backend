package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermResultRepository extends JpaRepository<TermResult, Long> {

    // REQUIRED by ResultServiceImpl
    Optional<TermResult> findByStudentAndSessionAndTerm(
            Student student,
            String session,
            Result.Term term
    );

    Optional<TermResult> findByStudentIdAndSessionAndTerm(
            Long studentId,
            String session,
            Result.Term term
    );

    List<TermResult> findBySessionAndTermOrderByAverageDesc(
            String session,
            Result.Term term
    );

    List<TermResult> findByStudent_StudentClassAndSessionAndTermOrderByAverageDesc(
            String studentClass,
            String session,
            Result.Term term
    );

    List<TermResult> findByStudent_StudentClassAndStudent_ClassArmAndSessionAndTermOrderByAverageDesc(
            String studentClass,
            String classArm,
            String session,
            Result.Term term
    );
}