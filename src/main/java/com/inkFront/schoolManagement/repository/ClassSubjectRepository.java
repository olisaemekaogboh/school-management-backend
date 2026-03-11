package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.ClassSubject;
import com.inkFront.schoolManagement.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    List<ClassSubject> findByClassNameOrderBySubject_NameAsc(String className);

    Optional<ClassSubject> findByClassNameAndSubject(String className, Subject subject);

    void deleteByClassNameAndSubject(String className, Subject subject);
}