package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.*;

import java.util.List;

public interface SubjectService {

    SubjectResponseDTO createSubject(SubjectRequestDTO request);

    SubjectResponseDTO updateSubject(Long id, SubjectRequestDTO request);

    void deleteSubject(Long id);

    SubjectResponseDTO getSubjectById(Long id);

    List<SubjectResponseDTO> getAllSubjects();

    List<SubjectResponseDTO> getActiveSubjects();

    SubjectResponseDTO toggleSubjectStatus(Long id, boolean active);

    ClassSubjectResponseDTO assignSubjectToClass(ClassSubjectRequestDTO request);

    void removeSubjectFromClass(String className, Long subjectId);

    List<ClassSubjectResponseDTO> getSubjectsForClass(String className);

    TeacherSubjectResponseDTO assignSubjectToTeacher(TeacherSubjectRequestDTO request);

    void removeTeacherSubject(Long id);

    List<TeacherSubjectResponseDTO> getTeacherSubjects(Long teacherId);
}