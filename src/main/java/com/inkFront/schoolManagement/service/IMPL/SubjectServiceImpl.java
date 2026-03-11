package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.*;
import com.inkFront.schoolManagement.repository.*;
import com.inkFront.schoolManagement.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public SubjectResponseDTO createSubject(SubjectRequestDTO request) {
        validateSubject(request);

        if (subjectRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new RuntimeException("Subject name already exists");
        }

        if (subjectRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new RuntimeException("Subject code already exists");
        }

        Subject subject = Subject.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .active(request.isActive())
                .build();

        return SubjectResponseDTO.fromEntity(subjectRepository.save(subject));
    }

    @Override
    public SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO request) {
        validateSubject(request);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        subjectRepository.findByNameIgnoreCase(request.getName().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Another subject already uses this name");
                    }
                });

        subjectRepository.findByCodeIgnoreCase(request.getCode().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Another subject already uses this code");
                    }
                });

        subject.setName(request.getName().trim());
        subject.setCode(request.getCode().trim().toUpperCase());
        subject.setActive(request.isActive());

        return SubjectResponseDTO.fromEntity(subjectRepository.save(subject));
    }

    @Override
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        subjectRepository.delete(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponseDTO getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        return SubjectResponseDTO.fromEntity(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponseDTO> getAllSubjects() {
        return subjectRepository.findAll()
                .stream()
                .map(SubjectResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponseDTO> getActiveSubjects() {
        return subjectRepository.findByActiveTrue()
                .stream()
                .map(SubjectResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public SubjectResponseDTO toggleSubjectStatus(Long id, boolean active) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        subject.setActive(active);
        return SubjectResponseDTO.fromEntity(subjectRepository.save(subject));
    }

    @Override
    public ClassSubjectResponseDTO assignSubjectToClass(ClassSubjectRequestDTO request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));

        String className = request.getClassName().trim();

        classSubjectRepository.findByClassNameAndSubject(className, subject)
                .ifPresent(existing -> {
                    throw new RuntimeException("Subject already assigned to this class");
                });

        ClassSubject classSubject = ClassSubject.builder()
                .className(className)
                .subject(subject)
                .build();

        return ClassSubjectResponseDTO.fromEntity(classSubjectRepository.save(classSubject));
    }

    @Override
    public void removeSubjectFromClass(String className, Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        classSubjectRepository.deleteByClassNameAndSubject(className.trim(), subject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubjectResponseDTO> getSubjectsForClass(String className) {
        return classSubjectRepository.findByClassNameOrderBySubject_NameAsc(className.trim())
                .stream()
                .map(ClassSubjectResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public TeacherSubjectResponseDTO assignSubjectToTeacher(TeacherSubjectRequestDTO request) {
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));

        String className = request.getClassName().trim();
        String classArm = request.getClassArm() != null ? request.getClassArm().trim() : "";

        teacherSubjectRepository.findByTeacherAndSubjectAndClassNameAndClassArm(
                teacher, subject, className, classArm
        ).ifPresent(existing -> {
            throw new RuntimeException("This teacher already has this subject for this class/arm");
        });

        TeacherSubject teacherSubject = TeacherSubject.builder()
                .teacher(teacher)
                .subject(subject)
                .className(className)
                .classArm(classArm)
                .build();

        return TeacherSubjectResponseDTO.fromEntity(teacherSubjectRepository.save(teacherSubject));
    }

    @Override
    public void removeTeacherSubject(Long id) {
        TeacherSubject teacherSubject = teacherSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher subject assignment not found"));

        teacherSubjectRepository.delete(teacherSubject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSubjectResponseDTO> getTeacherSubjects(Long teacherId) {
        return teacherSubjectRepository.findByTeacher_IdOrderByClassNameAscClassArmAsc(teacherId)
                .stream()
                .map(TeacherSubjectResponseDTO::fromEntity)
                .toList();
    }

    private void validateSubject(SubjectRequestDTO request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Subject name is required");
        }

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new RuntimeException("Subject code is required");
        }
    }
}