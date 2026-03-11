package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.ClassSubject;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    Optional<ClassSubject> findBySchoolClassAndSubject(SchoolClass schoolClass, Subject subject);

    List<ClassSubject> findBySchoolClassOrderBySubject_NameAsc(SchoolClass schoolClass);

    void deleteBySchoolClassAndSubject(SchoolClass schoolClass, Subject subject);

    void deleteAllBySchoolClass(SchoolClass schoolClass);
}