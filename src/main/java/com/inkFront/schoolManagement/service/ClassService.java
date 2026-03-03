// src/main/java/com/inkFront/schoolManagement/service/ClassService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.ClassDTO;
import com.inkFront.schoolManagement.dto.StudentResponseDTO;
import com.inkFront.schoolManagement.model.SchoolClass;

import java.util.List;
import java.util.Map;

public interface ClassService {
    SchoolClass createClass(ClassDTO classDTO);
    SchoolClass updateClass(Long id, ClassDTO classDTO);
    SchoolClass getClass(Long id);
    SchoolClass getClassByName(String className);
    void deleteClass(Long id);
    List<ClassDTO> getAllClasses();
    List<ClassDTO> getClassesByCategory(String category);

    SchoolClass assignClassTeacher(Long classId, Long teacherId);
    SchoolClass addSubject(Long classId, String subject);
    SchoolClass removeSubject(Long classId, String subject);

    List<StudentResponseDTO> getStudentsInClass(Long classId);
    Map<String, Object> getClassStatistics();

    byte[] generateClassListPdf(Long classId) throws Exception;
    byte[] generateClassListExcel(Long classId) throws Exception;
}