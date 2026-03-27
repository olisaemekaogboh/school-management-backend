package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.TeacherSubject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    @EntityGraph(attributePaths = {"subject", "teacher"})
    List<TeacherSubject> findByTeacher_IdOrderByClassNameAscClassArmAsc(Long teacherId);

    @EntityGraph(attributePaths = {"subject", "teacher"})
    List<TeacherSubject> findByClassNameAndClassArmOrderBySubject_NameAsc(String className, String classArm);

    @EntityGraph(attributePaths = {"subject", "teacher"})
    Optional<TeacherSubject> findByTeacherAndSubjectAndClassNameAndClassArm(
            Teacher teacher,
            Subject subject,
            String className,
            String classArm
    );

    @Query("""
        select count(ts) > 0
        from TeacherSubject ts
        where ts.teacher.id = :teacherId
          and upper(replace(trim(ts.className), ' ', '')) = upper(replace(trim(:className), ' ', ''))
          and upper(replace(trim(ts.classArm), ' ', '')) = upper(replace(trim(:classArm), ' ', ''))
    """)
    boolean existsTeacherAssignmentForClassArm(
            @Param("teacherId") Long teacherId,
            @Param("className") String className,
            @Param("classArm") String classArm
    );

    @Query("""
        select count(ts) > 0
        from TeacherSubject ts
        where ts.teacher.id = :teacherId
          and ts.subject.id = :subjectId
          and upper(replace(trim(ts.className), ' ', '')) = upper(replace(trim(:className), ' ', ''))
          and upper(replace(trim(ts.classArm), ' ', '')) = upper(replace(trim(:classArm), ' ', ''))
    """)
    boolean existsTeacherAssignmentForClassArmAndSubject(
            @Param("teacherId") Long teacherId,
            @Param("className") String className,
            @Param("classArm") String classArm,
            @Param("subjectId") Long subjectId
    );
}