package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.BusRoute;
import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByEmergencyContactPhone(String emergencyContactPhone);

    List<Student> findByStateOfOrigin(String stateOfOrigin);

    List<Student> findByLocalGovtArea(String localGovtArea);

    List<Student> findByStatus(Student.StudentStatus status);

    boolean existsByAdmissionNumber(String admissionNumber);

    @Query("SELECT s FROM Student s WHERE s.status = 'ACTIVE'")
    List<Student> findAllActiveStudents();

    @Query("""
        SELECT s
        FROM Student s
        WHERE s.schoolClass.className IN :classes
    """)
    List<Student> findByStudentClasses(@Param("classes") List<String> classes);
    List<Student> findBySchoolClassIdOrderByLastNameAscFirstNameAsc(Long schoolClassId);
    List<Student> findByExcludeFromPromotionTrue();

    List<Student> findByParentId(Long parentId);

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    @Query("""
        SELECT s
        FROM Student s
        WHERE LOWER(REPLACE(TRIM(s.schoolClass.className), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByStudentClass(@Param("studentClass") String studentClass);

    @Query("""
        SELECT s
        FROM Student s
        WHERE LOWER(REPLACE(TRIM(s.schoolClass.className), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
          AND LOWER(TRIM(s.schoolClass.arm)) = LOWER(TRIM(:classArm))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByStudentClassAndClassArm(
            @Param("studentClass") String studentClass,
            @Param("classArm") String classArm
    );

    List<Student> findByParentEmail(String parentEmail);

    List<Student> findByParentPhone(String parentPhone);

    @Query("""
        SELECT s
        FROM Student s
        WHERE LOWER(REPLACE(TRIM(s.schoolClass.className), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
          AND LOWER(TRIM(s.schoolClass.arm)) = LOWER(TRIM(:classArm))
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

    @Query("""
        SELECT s.schoolClass.className, COUNT(s)
        FROM Student s
        GROUP BY s.schoolClass.className
    """)
    List<Object[]> countStudentsByClass();

    @Query("SELECT s FROM Student s WHERE s.admissionDate >= :date")
    List<Student> findRecentAdmissions(@Param("date") LocalDate date);

    @Query("""
        SELECT s
        FROM Student s
        WHERE LOWER(REPLACE(TRIM(s.schoolClass.className), ' ', '')) = LOWER(REPLACE(TRIM(:className), ' ', ''))
          AND LOWER(TRIM(s.schoolClass.arm)) = LOWER(TRIM(:arm))
        ORDER BY s.lastName, s.firstName
        """)
    List<Student> findByStudentClassAndClassArmNormalized(
            @Param("className") String className,
            @Param("arm") String arm
    );

    List<Student> findByBusRouteIdOrderByLastNameAscFirstNameAsc(Long busRouteId);

    List<Student> findByBusRoute(BusRoute busRoute);

    long countByBusRouteId(Long busRouteId);

    long countByBusRoute(BusRoute busRoute);

    long countByBusRouteIsNotNull();

    long countByBusRouteIsNull();

    boolean existsByBusRouteId(Long busRouteId);

    @Query("""
        SELECT s
        FROM Student s
        WHERE s.busRoute.id = :routeId
        ORDER BY s.lastName ASC, s.firstName ASC
        """)
    List<Student> findTransportStudentsByRouteId(@Param("routeId") Long routeId);
}