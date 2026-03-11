package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Subject;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByTeacher_IdOrderByClassNameAscClassArmAsc(Long teacherId);

    List<TeacherSubject> findByClassNameAndClassArmOrderBySubject_NameAsc(String className, String classArm);

    Optional<TeacherSubject> findByTeacherAndSubjectAndClassNameAndClassArm(
            Teacher teacher,
            Subject subject,
            String className,
            String classArm
    );
}