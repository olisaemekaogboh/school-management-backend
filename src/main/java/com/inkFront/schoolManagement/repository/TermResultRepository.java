// src/main/java/com/inkFront/schoolManagement/repository/TermResultRepository.java
package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.TermResult;
import com.inkFront.schoolManagement.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermResultRepository extends JpaRepository<TermResult, Long> {

    Optional<TermResult> findByStudentAndSessionAndTerm(Student student, String session, Result.Term term);

    @Query("SELECT tr FROM TermResult tr WHERE tr.student.studentClass = :className AND tr.session = :session AND tr.term = :term")
    List<TermResult> findByClassAndSessionAndTerm(@Param("className") String className,
                                                  @Param("session") String session,
                                                  @Param("term") Result.Term term);


    @Query("SELECT tr FROM TermResult tr WHERE tr.student.studentClass = :className AND tr.session = :session AND tr.term = :term ORDER BY tr.average DESC")
    List<TermResult> findByClassAndSessionAndTermOrderByAverageDesc(@Param("className") String className,
                                                                    @Param("session") String session,
                                                                    @Param("term") Result.Term term);

    @Query("SELECT tr FROM TermResult tr WHERE tr.student.studentClass = :className AND tr.student.classArm = :arm AND tr.session = :session AND tr.term = :term ORDER BY tr.average DESC")
    List<TermResult> findByClassAndArmAndSessionAndTermOrderByAverageDesc(@Param("className") String className,
                                                                          @Param("arm") String arm,
                                                                          @Param("session") String session,
                                                                          @Param("term") Result.Term term);
}