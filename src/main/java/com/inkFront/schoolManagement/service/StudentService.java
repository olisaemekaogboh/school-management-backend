package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentService {

    // Basic CRUD operations
    Student registerStudent(Student student);
    Optional<Student> getStudentById(Long id);
    Optional<Student> getStudentByAdmissionNumber(String admissionNumber);
    List<Student> getAllStudents();
    Page<Student> getAllStudentsPaginated(Pageable pageable);
    Student updateStudent(Long id, Student student);
    void deleteStudent(Long id);
    void deleteStudentByAdmissionNumber(String admissionNumber);

    // Search and filter operations
    List<Student> searchStudents(String searchTerm);
    List<Student> getStudentsByClass(String studentClass);
    List<Student> getStudentsByClassAndArm(String studentClass, String classArm);
    List<Student> getStudentsByState(String stateOfOrigin);
    List<Student> getStudentsByLGA(String lga);
    List<Student> getActiveStudents();
    List<Student> getStudentsByStatus(Student.StudentStatus status);

    // Statistics and analytics
    Map<String, Long> getStudentCountByClass();
    Long getTotalStudentCount();
    Long getActiveStudentCount();
    List<Student> getRecentAdmissions(int days);

    // Bulk operations
    List<Student> registerBulkStudents(List<Student> students);
    void updateBulkStudentClass(List<Long> studentIds, String newClass);

    // Validation
    boolean isAdmissionNumberUnique(String admissionNumber);
    String generateAdmissionNumber();

    // Reports
    byte[] generateStudentReport(Long studentId);
    byte[] generateClassReport(String studentClass);


        // Promote all students with exclusions
        Map<String, Object> getPromotionPreview();
    Map<String, Object> promoteAllStudents();
    Map<String, Object> promoteSelectedStudents(List<Long> studentIds);
    Student togglePromotionExclusion(Long studentId, boolean exclude, String reason);
    List<Student> getExcludedStudents();
}