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

    List<TermResult> findByStudent_SchoolClass_ClassNameAndSessionAndTermOrderByAverageDesc(
            String className,
            String session,
            Result.Term term
    );

    List<TermResult> findByStudent_SchoolClass_ClassNameAndStudent_SchoolClass_ArmAndSessionAndTermOrderByAverageDesc(
            String className,
            String arm,
            String session,
            Result.Term term
    );
}