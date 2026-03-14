package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByEmergencyContactPhone(String emergencyContactPhone);

    List<Student> findByStateOfOrigin(String stateOfOrigin);

    List<Student> findByLocalGovtArea(String localGovtArea);

    List<Student> findByStatus(Student.StudentStatus status);

    boolean existsByAdmissionNumber(String admissionNumber);

    @Query("SELECT s FROM Student s WHERE s.status = 'ACTIVE'")
    List<Student> findAllActiveStudents();

    @Query("SELECT s FROM Student s WHERE s.studentClass IN :classes")
    List<Student> findByStudentClasses(@Param("classes") List<String> classes);

    List<Student> findByExcludeFromPromotionTrue();

    List<Student> findByParentId(Long parentId);

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    List<Student> findByStudentClass(String studentClass);

    List<Student> findByStudentClassAndClassArm(String studentClass, String classArm);

    List<Student> findByParentEmail(String parentEmail);

    List<Student> findByParentPhone(String parentPhone);

    @Query("""
        SELECT s
        FROM Student s
        WHERE LOWER(REPLACE(TRIM(s.studentClass), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
          AND LOWER(TRIM(s.classArm)) = LOWER(TRIM(:classArm))
        ORDER BY s.lastName ASC, s.firstName ASC
        """)
    List<Student> findByClassScopeNormalized(
            @Param("studentClass") String studentClass,
            @Param("classArm") String classArm
    );

    @Query("""
        SELECT s
        FROM Student s
        WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        """)
    List<Student> searchByName(@Param("searchTerm") String searchTerm);

    @Query("SELECT s.studentClass, COUNT(s) FROM Student s GROUP BY s.studentClass")
    List<Object[]> countStudentsByClass();

    @Query("SELECT s FROM Student s WHERE s.admissionDate >= :date")
    List<Student> findRecentAdmissions(@Param("date") LocalDate date);
    @Query("""
    SELECT s
    FROM Student s
    WHERE lower(replace(trim(s.studentClass), ' ', '')) = lower(replace(trim(:className), ' ', ''))
      AND lower(trim(s.classArm)) = lower(trim(:arm))
    ORDER BY s.lastName, s.firstName
""")
    List<Student> findByStudentClassAndClassArmNormalized(@Param("className") String className,
                                                          @Param("arm") String arm);
}