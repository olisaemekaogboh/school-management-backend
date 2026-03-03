// src/main/java/com/inkFront/schoolManagement/service/TeacherService.java
package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.TeacherInvitation;
import com.inkFront.schoolManagement.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TeacherService {

    // ========== BASIC CRUD OPERATIONS ==========
    List<TeacherDTO> getAllTeachers();
    Page<TeacherDTO> getAllTeachersPaginated(Pageable pageable);
    TeacherDTO getTeacherDTO(Long id);
    Teacher getTeacher(Long id);
    TeacherDTO getTeacherByTeacherId(String teacherId);
    TeacherDTO createTeacher(TeacherDTO teacherDTO, MultipartFile profilePicture);
    TeacherDTO updateTeacher(Long id, TeacherDTO teacherDTO, MultipartFile profilePicture);
    void deleteTeacher(Long id);

    // ========== SEARCH AND FILTER OPERATIONS ==========
    List<TeacherDTO> searchTeachers(String term);
    List<TeacherDTO> getTeachersByStatus(String status);
    List<TeacherDTO> getTeachersBySubject(String subject);
    List<TeacherDTO> getTeachersByDepartment(String department);
    List<TeacherDTO> getRecentTeachers(int days);
    List<TeacherDTO> getTeachersWithoutUserAccount();
    List<TeacherDTO> getTeachersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // ========== SUBJECT AND QUALIFICATION MANAGEMENT ==========
    TeacherDTO addSubject(Long id, String subject);
    TeacherDTO removeSubject(Long id, String subject);
    TeacherDTO addQualification(Long id, String qualification);
    TeacherDTO updateEmploymentStatus(Long id, String status);

    // ========== STATISTICS AND UTILITIES ==========
    Map<String, Object> getTeacherStatistics();
    String generateTeacherId();
    boolean checkEmailExists(String email);
    boolean checkTeacherIdExists(String teacherId);
    long getTotalTeacherCount();
    long getActiveTeacherCount();
    long getInactiveTeacherCount();
    Map<String, Long> getTeacherCountByDepartment();
    Map<String, Long> getTeacherCountByStatus();

    // ========== EXPORT OPERATIONS ==========
    byte[] exportToPDF();
    byte[] exportToExcel();
    byte[] exportTeachersByDepartment(String department);
    byte[] exportTeachersByStatus(String status);

    // ========== INVITATION METHODS ==========
    void createInvitation(TeacherInviteDTO inviteDTO, String token);
    TeacherInvitation verifyInvitationToken(String token);
    User completeRegistration(CompleteRegistrationDTO completeDTO);
    void resendInvitation(String email);
    List<TeacherInvitationDTO> getPendingInvitations();
    List<TeacherInvitationDTO> getExpiredInvitations();
    void cancelInvitation(Long invitationId);
    void cleanupExpiredInvitations();
    long getPendingInvitationCount();
}