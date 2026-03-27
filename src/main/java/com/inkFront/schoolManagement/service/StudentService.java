package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentService {

    Student registerStudent(Student student);

    Optional<Student> getStudentById(Long id);

    Optional<Student> getStudentByAdmissionNumber(String admissionNumber);

    List<Student> getAllStudents();

    Page<Student> getAllStudentsPaginated(Pageable pageable);

    Student updateStudent(Long id, Student student);

    void deleteStudent(Long id);

    void deleteStudentByAdmissionNumber(String admissionNumber);

    List<Student> searchStudents(String searchTerm);

    List<Student> getStudentsByClassId(Long classId);

    List<Student> getStudentsByState(String stateOfOrigin);

    List<Student> getStudentsByLGA(String lga);

    List<Student> getActiveStudents();

    List<Student> getStudentsByStatus(Student.StudentStatus status);

    Map<String, Long> getStudentCountByClass();

    Long getTotalStudentCount();

    Long getActiveStudentCount();

    List<Student> getRecentAdmissions(int days);

    List<Student> registerBulkStudents(List<Student> students);

    void updateBulkStudentClass(List<Long> studentIds, Long newClassId);

    boolean isAdmissionNumberUnique(String admissionNumber);

    String generateAdmissionNumber();

    byte[] generateStudentReport(Long studentId);

    byte[] generateClassReport(Long classId);

    Map<String, Object> getPromotionPreview();

    Map<String, Object> promoteAllStudents();

    Map<String, Object> promoteSelectedStudents(List<Long> studentIds);

    Map<String, Object> promoteClass(Long classId);

    Student togglePromotionExclusion(Long studentId, boolean exclude, String reason);

    List<Student> getExcludedStudents();
}