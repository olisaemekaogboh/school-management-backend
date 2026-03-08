// src/main/java/com/inkFront/schoolManagement/service/IMPL/ClassServiceImpl.java
package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.ClassDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.service.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public SchoolClass createClass(ClassDTO classDTO) {
        log.info("Creating new class: {} - Arm {}", classDTO.getClassName(), classDTO.getArm());

        // Check if class with same name and arm already exists
        if (classRepository.findByClassNameAndArm(classDTO.getClassName(), classDTO.getArm()).isPresent()) {
            throw new BusinessException("Class already exists with name: " + classDTO.getClassName() + " and arm: " + classDTO.getArm());
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .className(classDTO.getClassName())
                .arm(classDTO.getArm()) // ADD THIS - set the arm
                .category(classDTO.getCategory())
                .description(classDTO.getDescription())
                .capacity(classDTO.getCapacity() != null ? classDTO.getCapacity() : 40)
                .currentEnrollment(0)
                .subjects(classDTO.getSubjects() != null ? classDTO.getSubjects() : new java.util.ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Assign class teacher if provided
        if (classDTO.getClassTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(classDTO.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + classDTO.getClassTeacherId()));
            schoolClass.setClassTeacher(teacher);
        }

        // The classCode will be auto-generated in @PrePersist
        SchoolClass savedClass = classRepository.save(schoolClass);
        log.info("Class created successfully with id: {} and code: {}", savedClass.getId(), savedClass.getClassCode());
        return savedClass;
    }
    @Override
    public SchoolClass updateClass(Long id, ClassDTO classDTO) {
        log.info("Updating class with id: {}", id);

        SchoolClass schoolClass = getClass(id);

        // Check if another class with same name and arm exists (excluding this one)
        classRepository.findByClassNameAndArm(classDTO.getClassName(), classDTO.getArm())
                .ifPresent(existingClass -> {
                    if (!existingClass.getId().equals(id)) {
                        throw new BusinessException("Another class already exists with name: " +
                                classDTO.getClassName() + " and arm: " + classDTO.getArm());
                    }
                });

        schoolClass.setClassName(classDTO.getClassName());
        schoolClass.setArm(classDTO.getArm()); // ADD THIS - update the arm
        schoolClass.setCategory(classDTO.getCategory());
        schoolClass.setDescription(classDTO.getDescription());
        schoolClass.setCapacity(classDTO.getCapacity());
        schoolClass.setSubjects(classDTO.getSubjects());
        schoolClass.setUpdatedAt(LocalDateTime.now());

        // Update class teacher if changed
        if (classDTO.getClassTeacherId() != null) {
            if (schoolClass.getClassTeacher() == null ||
                    !schoolClass.getClassTeacher().getId().equals(classDTO.getClassTeacherId())) {
                Teacher teacher = teacherRepository.findById(classDTO.getClassTeacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + classDTO.getClassTeacherId()));
                schoolClass.setClassTeacher(teacher);
            }
        } else {
            schoolClass.setClassTeacher(null);
        }

        // The classCode will be auto-updated in @PreUpdate based on className and arm
        SchoolClass updatedClass = classRepository.save(schoolClass);
        log.info("Class updated successfully with id: {} and code: {}", id, updatedClass.getClassCode());
        return updatedClass;
    }

    @Override
    public SchoolClass getClass(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    @Override
    public SchoolClass getClassByName(String className) {
        return classRepository.findByClassName(className)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with name: " + className));
    }

    @Override
    public void deleteClass(Long id) {
        log.info("Deleting class with id: {}", id);
        SchoolClass schoolClass = getClass(id);

        // Check if class has students
        if (schoolClass.getCurrentEnrollment() > 0) {
            throw new BusinessException("Cannot delete class with enrolled students");
        }

        classRepository.delete(schoolClass);
        log.info("Class deleted successfully with id: {}", id);
    }

    @Override
    public List<ClassDTO> getAllClasses() {
        return classRepository.findAllWithSubjects().stream()
                .map(ClassDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassDTO> getClassesByCategory(String category) {
        try {
            SchoolClass.ClassCategory classCategory = SchoolClass.ClassCategory.valueOf(category);
            return classRepository.findByCategory(classCategory).stream()
                    .map(ClassDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid category: " + category);
        }
    }

    @Override
    public SchoolClass assignClassTeacher(Long classId, Long teacherId) {
        SchoolClass schoolClass = getClass(classId);
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        schoolClass.setClassTeacher(teacher);
        schoolClass.setUpdatedAt(LocalDateTime.now());
        return classRepository.save(schoolClass);
    }

    @Override
    public SchoolClass addSubject(Long classId, String subject) {
        SchoolClass schoolClass = getClass(classId);
        if (!schoolClass.getSubjects().contains(subject)) {
            schoolClass.getSubjects().add(subject);
            schoolClass.setUpdatedAt(LocalDateTime.now());
            return classRepository.save(schoolClass);
        }
        return schoolClass;
    }

    @Override
    public SchoolClass removeSubject(Long classId, String subject) {
        SchoolClass schoolClass = getClass(classId);
        schoolClass.getSubjects().remove(subject);
        schoolClass.setUpdatedAt(LocalDateTime.now());
        return classRepository.save(schoolClass);
    }

    @Override
    public List<StudentResponseDTO> getStudentsInClass(Long classId) {
        SchoolClass schoolClass = getClass(classId);
        List<Student> students = studentRepository.findByStudentClass(schoolClass.getClassName());
        return students.stream()
                .map(StudentResponseDTO::fromStudent)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getClassStatistics() {
        // Implement statistics logic here
        return Map.of(
                "totalClasses", classRepository.count(),
                "message", "Statistics coming soon"
        );
    }

    @Override
    public byte[] generateClassListPdf(Long classId) throws Exception {
        // Implement PDF generation here
        throw new UnsupportedOperationException("PDF generation not implemented yet");
    }

    @Override
    public byte[] generateClassListExcel(Long classId) throws Exception {
        // Implement Excel generation here
        throw new UnsupportedOperationException("Excel generation not implemented yet");
    }
}