package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.BusRoute;
import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByEmergencyContactPhone(String emergencyContactPhone);

    List<Student> findByStateOfOriginOrderByLastNameAscFirstNameAsc(String stateOfOrigin);

    List<Student> findByLocalGovtAreaOrderByLastNameAscFirstNameAsc(String localGovtArea);

    List<Student> findByStatusOrderByLastNameAscFirstNameAsc(Student.StudentStatus status);

    long countByStatus(Student.StudentStatus status);

    boolean existsByAdmissionNumber(String admissionNumber);

    @EntityGraph(attributePaths = {"schoolClass", "parent", "busRoute"})
    List<Student> findAll(Sort sort);

    @Query("""
        SELECT s
        FROM Student s
        WHERE s.status = com.inkFront.schoolManagement.model.Student$StudentStatus.ACTIVE
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findAllActiveStudents();

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE sc.className IN :classes
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByStudentClasses(@Param("classes") Collection<String> classes);

    @EntityGraph(attributePaths = {"schoolClass", "parent", "busRoute"})
    List<Student> findBySchoolClassIdOrderByLastNameAscFirstNameAsc(Long schoolClassId);

    List<Student> findByExcludeFromPromotionTrueOrderByLastNameAscFirstNameAsc();

    List<Student> findByParentIdOrderByLastNameAscFirstNameAsc(Long parentId);

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    List<Student> findByParentEmailOrderByLastNameAscFirstNameAsc(String parentEmail);

    List<Student> findByParentPhoneOrderByLastNameAscFirstNameAsc(String parentPhone);

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> searchByName(@Param("searchTerm") String searchTerm);

    @Query("""
        SELECT s.schoolClass.className, COUNT(s)
        FROM Student s
        GROUP BY s.schoolClass.className
    """)
    List<Object[]> countStudentsByClass();

    @Query("""
        SELECT s.schoolClass.id, s.schoolClass.className, s.schoolClass.arm, COUNT(s)
        FROM Student s
        GROUP BY s.schoolClass.id, s.schoolClass.className, s.schoolClass.arm
    """)
    List<Object[]> countStudentsByClassWithArm();

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE s.admissionDate >= :date
        ORDER BY s.admissionDate DESC, s.lastName ASC, s.firstName ASC
    """)
    List<Student> findRecentAdmissions(@Param("date") LocalDate date);

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
        JOIN FETCH s.schoolClass
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE s.busRoute.id = :routeId
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findTransportStudentsByRouteId(@Param("routeId") Long routeId);

    // =========================
    // Compatibility methods
    // =========================

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE LOWER(REPLACE(TRIM(sc.className), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByStudentClass(@Param("studentClass") String studentClass);

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE LOWER(REPLACE(TRIM(sc.className), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
          AND LOWER(TRIM(sc.arm)) = LOWER(TRIM(:classArm))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByStudentClassAndClassArm(
            @Param("studentClass") String studentClass,
            @Param("classArm") String classArm
    );

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE LOWER(REPLACE(TRIM(sc.className), ' ', '')) = LOWER(REPLACE(TRIM(:className), ' ', ''))
          AND LOWER(TRIM(sc.arm)) = LOWER(TRIM(:arm))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByStudentClassAndClassArmNormalized(
            @Param("className") String className,
            @Param("arm") String arm
    );

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.schoolClass sc
        LEFT JOIN FETCH s.parent
        LEFT JOIN FETCH s.busRoute
        WHERE LOWER(REPLACE(TRIM(sc.className), ' ', '')) = LOWER(REPLACE(TRIM(:studentClass), ' ', ''))
          AND LOWER(TRIM(sc.arm)) = LOWER(TRIM(:classArm))
        ORDER BY s.lastName ASC, s.firstName ASC
    """)
    List<Student> findByClassScopeNormalized(
            @Param("studentClass") String studentClass,
            @Param("classArm") String classArm
    );
}