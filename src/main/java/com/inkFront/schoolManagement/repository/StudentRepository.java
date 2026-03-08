// src/main/java/com/inkFront/schoolManagement/repository/StudentRepository.java
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

    // Find by admission number
    Optional<Student> findByAdmissionNumber(String admissionNumber);

    // Find by class
    List<Student> findByStudentClass(String studentClass);

    // Find by class and arm
    List<Student> findByStudentClassAndClassArm(String studentClass, String classArm);

    // Find by parent email
    List<Student> findByParentEmail(String parentEmail);

    // Find by parent phone - THIS ALREADY EXISTS
    List<Student> findByParentPhone(String parentPhone);

    // ADD THIS METHOD - Find by emergency contact phone
    List<Student> findByEmergencyContactPhone(String emergencyContactPhone);

    // Find by state of origin
    List<Student> findByStateOfOrigin(String stateOfOrigin);

    // Find by LGA
    List<Student> findByLocalGovtArea(String localGovtArea);

    // Find by status
    List<Student> findByStatus(Student.StudentStatus status);

    // Search students by name
    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Student> searchByName(@Param("searchTerm") String searchTerm);

    // Count students by class
    @Query("SELECT s.studentClass, COUNT(s) FROM Student s GROUP BY s.studentClass")
    List<Object[]> countStudentsByClass();

    // Find recent admissions
    @Query("SELECT s FROM Student s WHERE s.admissionDate >= :date")
    List<Student> findRecentAdmissions(@Param("date") LocalDate date);

    // Check if admission number exists
    boolean existsByAdmissionNumber(String admissionNumber);

    // Find active students
    @Query("SELECT s FROM Student s WHERE s.status = 'ACTIVE'")
    List<Student> findAllActiveStudents();

    // Find by multiple classes
    @Query("SELECT s FROM Student s WHERE s.studentClass IN :classes")
    List<Student> findByStudentClasses(@Param("classes") List<String> classes);

    // Find students excluded from promotion
    List<Student> findByExcludeFromPromotionTrue();
    List<Student> findByParentId(Long parentId);
}