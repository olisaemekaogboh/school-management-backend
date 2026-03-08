package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.*;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.TeacherInvitation;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.TeacherInvitationRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.FileStorageService;
import com.inkFront.schoolManagement.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherInvitationRepository teacherInvitationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    private TeacherDTO toTeacherDTO(Teacher teacher) {
        if (teacher.getSubjects() != null) {
            teacher.getSubjects().size();
        }
        if (teacher.getQualifications() != null) {
            teacher.getQualifications().size();
        }
        if (teacher.getUser() != null) {
            teacher.getUser().getId();
        }
        return TeacherDTO.fromEntity(teacher);
    }

    // ========== BASIC CRUD OPERATIONS ==========

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getAllTeachers() {
        log.info("Fetching all teachers");
        return teacherRepository.findAllWithDetails().stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherDTO> getAllTeachersPaginated(Pageable pageable) {
        log.info("Fetching teachers page: {}", pageable.getPageNumber());
        return teacherRepository.findAll(pageable)
                .map(this::toTeacherDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDTO getTeacherDTO(Long id) {
        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
        return toTeacherDTO(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public Teacher getTeacher(Long id) {
        log.info("Fetching teacher entity with id: {}", id);
        return teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDTO getTeacherByTeacherId(String teacherId) {
        log.info("Fetching teacher with teacherId: {}", teacherId);
        Teacher teacher = teacherRepository.findByTeacherIdWithDetails(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with teacherId: " + teacherId));
        return toTeacherDTO(teacher);
    }

    @Override
    @Transactional
    public TeacherDTO createTeacher(TeacherDTO teacherDTO, MultipartFile profilePicture) {
        log.info("Creating new teacher with email: {}", teacherDTO.getEmail());

        if (teacherRepository.existsByEmail(teacherDTO.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        if (teacherDTO.getTeacherId() == null || teacherDTO.getTeacherId().isEmpty()) {
            teacherDTO.setTeacherId(generateTeacherId());
        } else if (teacherRepository.existsByTeacherId(teacherDTO.getTeacherId())) {
            throw new BusinessException("Teacher ID already exists");
        }

        Teacher teacher = TeacherDTO.toEntity(teacherDTO);

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String pictureUrl = fileStorageService.storeFile(profilePicture);
            teacher.setProfilePictureUrl(pictureUrl);
        }

        if (teacher.getStatus() == null) {
            teacher.setStatus(Teacher.TeacherStatus.ACTIVE);
        }

        if (teacher.getEmploymentStatus() == null) {
            teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        }

        if (teacher.getEmploymentType() == null) {
            teacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
        }

        if (teacher.getGender() == null) {
            teacher.setGender(Teacher.Gender.MALE);
        }

        if (teacher.getMaritalStatus() == null) {
            teacher.setMaritalStatus(Teacher.MaritalStatus.SINGLE);
        }

        Teacher savedTeacher = teacherRepository.save(teacher);
        log.info("Teacher created successfully with id: {}", savedTeacher.getId());

        Teacher teacherWithDetails = teacherRepository.findByIdWithDetails(savedTeacher.getId())
                .orElse(savedTeacher);

        return toTeacherDTO(teacherWithDetails);
    }

    @Override
    @Transactional
    public TeacherDTO updateTeacher(Long id, TeacherDTO teacherDTO, MultipartFile profilePicture) {
        log.info("Updating teacher with id: {}", id);

        Teacher existingTeacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (!existingTeacher.getEmail().equals(teacherDTO.getEmail()) &&
                teacherRepository.existsByEmail(teacherDTO.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        if (!existingTeacher.getTeacherId().equals(teacherDTO.getTeacherId()) &&
                teacherRepository.existsByTeacherId(teacherDTO.getTeacherId())) {
            throw new BusinessException("Teacher ID already exists");
        }

        existingTeacher.setFirstName(teacherDTO.getFirstName());
        existingTeacher.setLastName(teacherDTO.getLastName());
        existingTeacher.setMiddleName(teacherDTO.getMiddleName());
        existingTeacher.setEmail(teacherDTO.getEmail());
        existingTeacher.setPhoneNumber(teacherDTO.getPhoneNumber());
        existingTeacher.setAlternatePhone(teacherDTO.getAlternatePhone());
        existingTeacher.setDateOfBirth(teacherDTO.getDateOfBirth());

        if (teacherDTO.getGender() != null && !teacherDTO.getGender().isEmpty()) {
            try {
                existingTeacher.setGender(Teacher.Gender.valueOf(teacherDTO.getGender()));
            } catch (IllegalArgumentException e) {
                existingTeacher.setGender(Teacher.Gender.MALE);
            }
        }

        existingTeacher.setAddress(teacherDTO.getAddress());
        existingTeacher.setQualification(teacherDTO.getQualification());
        existingTeacher.setSpecialization(teacherDTO.getSpecialization());
        existingTeacher.setYearsOfExperience(teacherDTO.getYearsOfExperience());
        existingTeacher.setEmployeeId(teacherDTO.getEmployeeId());
        existingTeacher.setTeacherId(teacherDTO.getTeacherId());
        existingTeacher.setDepartment(teacherDTO.getDepartment());
        existingTeacher.setDesignation(teacherDTO.getDesignation());
        existingTeacher.setDateOfJoining(teacherDTO.getDateOfJoining());

        if (teacherDTO.getEmploymentType() != null && !teacherDTO.getEmploymentType().isEmpty()) {
            try {
                existingTeacher.setEmploymentType(Teacher.EmploymentType.valueOf(teacherDTO.getEmploymentType()));
            } catch (IllegalArgumentException e) {
                existingTeacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
            }
        }

        if (teacherDTO.getEmploymentStatus() != null && !teacherDTO.getEmploymentStatus().isEmpty()) {
            try {
                existingTeacher.setEmploymentStatus(Teacher.EmploymentStatus.valueOf(teacherDTO.getEmploymentStatus()));
            } catch (IllegalArgumentException e) {
                existingTeacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
            }
        }

        if (teacherDTO.getMaritalStatus() != null && !teacherDTO.getMaritalStatus().isEmpty()) {
            try {
                existingTeacher.setMaritalStatus(Teacher.MaritalStatus.valueOf(teacherDTO.getMaritalStatus()));
            } catch (IllegalArgumentException e) {
                existingTeacher.setMaritalStatus(Teacher.MaritalStatus.SINGLE);
            }
        }

        if (teacherDTO.getSubjects() != null) {
            existingTeacher.setSubjects(new HashSet<>(teacherDTO.getSubjects()));
        }

        if (teacherDTO.getQualifications() != null) {
            existingTeacher.setQualifications(new HashSet<>(teacherDTO.getQualifications()));
        }

        existingTeacher.setEmergencyContactName(teacherDTO.getEmergencyContactName());
        existingTeacher.setEmergencyContactPhone(teacherDTO.getEmergencyContactPhone());
        existingTeacher.setEmergencyContactRelationship(teacherDTO.getEmergencyContactRelationship());

        if (teacherDTO.getStatus() != null && !teacherDTO.getStatus().isEmpty()) {
            try {
                existingTeacher.setStatus(Teacher.TeacherStatus.valueOf(teacherDTO.getStatus()));
            } catch (IllegalArgumentException e) {
                existingTeacher.setStatus(Teacher.TeacherStatus.ACTIVE);
            }
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String pictureUrl = fileStorageService.storeFile(profilePicture);
            existingTeacher.setProfilePictureUrl(pictureUrl);
        }

        Teacher updatedTeacher = teacherRepository.save(existingTeacher);
        log.info("Teacher updated successfully with id: {}", updatedTeacher.getId());

        Teacher teacherWithDetails = teacherRepository.findByIdWithDetails(updatedTeacher.getId())
                .orElse(updatedTeacher);

        return toTeacherDTO(teacherWithDetails);
    }

    @Override
    public void deleteTeacher(Long id) {
        log.info("Deleting teacher with id: {}", id);

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (teacher.getUser() != null) {
            throw new BusinessException("Cannot delete teacher with associated user account. Deactivate the user instead.");
        }

        teacherRepository.delete(teacher);
        log.info("Teacher deleted successfully with id: {}", id);
    }

    // ========== SEARCH AND FILTER OPERATIONS ==========

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> searchTeachers(String term) {
        log.info("Searching teachers with term: {}", term);
        return teacherRepository.searchTeachers(term).stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getTeachersByStatus(String status) {
        log.info("Fetching teachers by status: {}", status);
        Teacher.TeacherStatus teacherStatus = Teacher.TeacherStatus.valueOf(status.toUpperCase());
        return teacherRepository.findByStatus(teacherStatus).stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getTeachersBySubject(String subject) {
        log.info("Fetching teachers by subject: {}", subject);
        return teacherRepository.findBySubjectsContaining(subject).stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getTeachersByDepartment(String department) {
        log.info("Fetching teachers by department: {}", department);
        return teacherRepository.findByDepartment(department).stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getRecentTeachers(int days) {
        log.info("Fetching teachers from last {} days", days);
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        return teacherRepository.findTeachersSince(date).stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getTeachersWithoutUserAccount() {
        log.info("Fetching teachers without user accounts");
        return teacherRepository.findTeachersWithoutUserAccount().stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getTeachersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching teachers between {} and {}", startDate, endDate);
        return teacherRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(this::toTeacherDTO)
                .collect(Collectors.toList());
    }

    // ========== SUBJECT AND QUALIFICATION MANAGEMENT ==========

    @Override
    @Transactional
    public TeacherDTO addSubject(Long id, String subject) {
        log.info("Adding subject {} to teacher with id: {}", subject, id);

        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (!teacher.getSubjects().contains(subject)) {
            teacher.getSubjects().add(subject);
            Teacher updatedTeacher = teacherRepository.save(teacher);
            return toTeacherDTO(updatedTeacher);
        }

        return toTeacherDTO(teacher);
    }

    @Override
    @Transactional
    public TeacherDTO removeSubject(Long id, String subject) {
        log.info("Removing subject {} from teacher with id: {}", subject, id);

        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        teacher.getSubjects().remove(subject);
        Teacher updatedTeacher = teacherRepository.save(teacher);

        return toTeacherDTO(updatedTeacher);
    }

    @Override
    @Transactional
    public TeacherDTO addQualification(Long id, String qualification) {
        log.info("Adding qualification {} to teacher with id: {}", qualification, id);

        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (!teacher.getQualifications().contains(qualification)) {
            teacher.getQualifications().add(qualification);
            Teacher updatedTeacher = teacherRepository.save(teacher);
            return toTeacherDTO(updatedTeacher);
        }

        return toTeacherDTO(teacher);
    }

    @Override
    @Transactional
    public TeacherDTO updateEmploymentStatus(Long id, String status) {
        log.info("Updating employment status to {} for teacher with id: {}", status, id);

        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (status != null && !status.isEmpty()) {
            try {
                teacher.setEmploymentStatus(Teacher.EmploymentStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid employment status: " + status);
            }
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return toTeacherDTO(updatedTeacher);
    }

    // ========== STATISTICS AND UTILITIES ==========

    @Override
    public Map<String, Object> getTeacherStatistics() {
        log.info("Fetching teacher statistics");

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalTeachers", teacherRepository.count());
        statistics.put("activeTeachers", teacherRepository.countByStatus(Teacher.TeacherStatus.ACTIVE));
        statistics.put("inactiveTeachers", teacherRepository.countByStatus(Teacher.TeacherStatus.INACTIVE));
        statistics.put("onLeaveTeachers", teacherRepository.countByStatus(Teacher.TeacherStatus.ON_LEAVE));
        statistics.put("terminatedTeachers", teacherRepository.countByStatus(Teacher.TeacherStatus.TERMINATED));
        statistics.put("teachersWithoutUser", teacherRepository.findTeachersWithoutUserAccount().size());
        statistics.put("pendingInvitations", getPendingInvitationCount());

        Map<String, Long> byDepartment = new HashMap<>();
        String[] departments = {"Science", "Arts", "Commercial", "Technical", "Primary", "Nursery"};
        for (String dept : departments) {
            byDepartment.put(dept, teacherRepository.countByDepartment(dept));
        }
        statistics.put("byDepartment", byDepartment);

        Map<String, Long> byEmploymentType = new HashMap<>();
        for (Teacher.EmploymentType type : Teacher.EmploymentType.values()) {
            byEmploymentType.put(type.name(), (long) teacherRepository.findByEmploymentType(type).size());
        }
        statistics.put("byEmploymentType", byEmploymentType);

        Map<String, Long> byEmploymentStatus = new HashMap<>();
        for (Teacher.EmploymentStatus status : Teacher.EmploymentStatus.values()) {
            byEmploymentStatus.put(status.name(), 0L);
        }
        statistics.put("byEmploymentStatus", byEmploymentStatus);

        return statistics;
    }

    @Override
    public String generateTeacherId() {
        log.info("Generating teacher ID");
        String year = String.valueOf(Year.now().getValue()).substring(2);
        long count = teacherRepository.count() + 1;
        return "TCH" + year + String.format("%04d", count);
    }

    @Override
    public boolean checkEmailExists(String email) {
        return teacherRepository.existsByEmail(email);
    }

    @Override
    public boolean checkTeacherIdExists(String teacherId) {
        return teacherRepository.existsByTeacherId(teacherId);
    }

    @Override
    public long getTotalTeacherCount() {
        return teacherRepository.count();
    }

    @Override
    public long getActiveTeacherCount() {
        return teacherRepository.countByStatus(Teacher.TeacherStatus.ACTIVE);
    }

    @Override
    public long getInactiveTeacherCount() {
        return teacherRepository.countByStatus(Teacher.TeacherStatus.INACTIVE);
    }

    @Override
    public Map<String, Long> getTeacherCountByDepartment() {
        Map<String, Long> counts = new HashMap<>();
        String[] departments = {"Science", "Arts", "Commercial", "Technical", "Primary", "Nursery"};
        for (String dept : departments) {
            counts.put(dept, teacherRepository.countByDepartment(dept));
        }
        return counts;
    }

    @Override
    public Map<String, Long> getTeacherCountByStatus() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("ACTIVE", teacherRepository.countByStatus(Teacher.TeacherStatus.ACTIVE));
        counts.put("INACTIVE", teacherRepository.countByStatus(Teacher.TeacherStatus.INACTIVE));
        counts.put("ON_LEAVE", teacherRepository.countByStatus(Teacher.TeacherStatus.ON_LEAVE));
        counts.put("TERMINATED", teacherRepository.countByStatus(Teacher.TeacherStatus.TERMINATED));
        return counts;
    }

    // ========== EXPORT OPERATIONS ==========

    @Override
    public byte[] exportToPDF() {
        log.info("Exporting teachers to PDF");
        throw new UnsupportedOperationException("PDF export not implemented yet");
    }

    @Override
    public byte[] exportToExcel() {
        log.info("Exporting teachers to Excel");
        throw new UnsupportedOperationException("Excel export not implemented yet");
    }

    @Override
    public byte[] exportTeachersByDepartment(String department) {
        log.info("Exporting teachers by department: {} to PDF", department);
        throw new UnsupportedOperationException("Department export not implemented yet");
    }

    @Override
    public byte[] exportTeachersByStatus(String status) {
        log.info("Exporting teachers by status: {} to PDF", status);
        throw new UnsupportedOperationException("Status export not implemented yet");
    }

    // ========== INVITATION METHODS ==========

    @Override
    @Transactional
    public void createInvitation(TeacherInviteDTO inviteDTO, String token) {
        log.info("Creating invitation for teacher: {}", inviteDTO.getEmail());

        Optional<TeacherInvitation> existingInvitation = teacherInvitationRepository.findByEmail(inviteDTO.getEmail());
        if (existingInvitation.isPresent()) {
            TeacherInvitation invitation = existingInvitation.get();
            if (!invitation.isUsed() && invitation.getExpiryDate().isAfter(LocalDateTime.now())) {
                throw new BusinessException("An active invitation has already been sent to this email");
            } else if (!invitation.isUsed() && invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
                teacherInvitationRepository.delete(invitation);
            }
        }

        if (userRepository.existsByEmail(inviteDTO.getEmail())) {
            throw new BusinessException("A user with this email already exists");
        }

        TeacherInvitation invitation = new TeacherInvitation();
        invitation.setFirstName(inviteDTO.getFirstName());
        invitation.setLastName(inviteDTO.getLastName());
        invitation.setEmail(inviteDTO.getEmail());
        invitation.setPhoneNumber(inviteDTO.getPhoneNumber());
        invitation.setToken(token);

        teacherInvitationRepository.save(invitation);
        log.info("Invitation created for: {}", inviteDTO.getEmail());
    }

    @Override
    public TeacherInvitation verifyInvitationToken(String token) {
        log.info("Verifying invitation token: {}", token);

        TeacherInvitation invitation = teacherInvitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid invitation token"));

        if (invitation.isUsed()) {
            throw new BusinessException("This invitation has already been used");
        }

        if (invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invitation has expired");
        }

        return invitation;
    }

    @Override
    @Transactional
    public User completeRegistration(CompleteRegistrationDTO completeDTO) {
        log.info("Completing registration for token: {}", completeDTO.getToken());

        TeacherInvitation invitation = verifyInvitationToken(completeDTO.getToken());

        if (userRepository.existsByUsername(completeDTO.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        User user = new User();
        user.setFirstName(invitation.getFirstName());
        user.setLastName(invitation.getLastName());
        user.setEmail(invitation.getEmail());
        user.setUsername(completeDTO.getUsername());
        user.setPassword(passwordEncoder.encode(completeDTO.getPassword()));
        user.setPhoneNumber(invitation.getPhoneNumber());
        user.setRole(User.Role.TEACHER);
        user.setActive(true);
        user.setEmailVerified(false);

        Optional<Teacher> existingTeacher = teacherRepository.findByEmail(invitation.getEmail());
        Teacher teacher;

        if (existingTeacher.isPresent()) {
            teacher = existingTeacher.get();
            teacher.setUser(user);
        } else {
            teacher = new Teacher();
            teacher.setFirstName(invitation.getFirstName());
            teacher.setLastName(invitation.getLastName());
            teacher.setEmail(invitation.getEmail());
            teacher.setPhoneNumber(invitation.getPhoneNumber());
            teacher.setUser(user);
            teacher.setTeacherId(generateTeacherId());
            teacher.setSubjects(new HashSet<>());
            teacher.setQualifications(new HashSet<>());
            teacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
            teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
            teacher.setStatus(Teacher.TeacherStatus.ACTIVE);
            teacher.setGender(Teacher.Gender.MALE);
            teacher.setMaritalStatus(Teacher.MaritalStatus.SINGLE);
        }

        user.setTeacher(teacher);
        invitation.setUsed(true);

        teacherRepository.save(teacher);
        User savedUser = userRepository.save(user);
        teacherInvitationRepository.save(invitation);

        log.info("Teacher registration completed for: {} with teacher ID: {}",
                savedUser.getUsername(), teacher.getTeacherId());

        return savedUser;
    }

    @Override
    @Transactional
    public void resendInvitation(String email) {
        log.info("Resending invitation to: {}", email);

        TeacherInvitation invitation = teacherInvitationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("No invitation found for email: " + email));

        if (invitation.isUsed()) {
            throw new BusinessException("This invitation has already been used");
        }

        String newToken = UUID.randomUUID().toString();
        invitation.setToken(newToken);
        invitation.setExpiryDate(LocalDateTime.now().plusHours(24));

        teacherInvitationRepository.save(invitation);
    }

    @Override
    public List<TeacherInvitationDTO> getPendingInvitations() {
        log.info("Fetching pending invitations");
        return teacherInvitationRepository.findByUsedFalseAndExpiryDateAfter(LocalDateTime.now())
                .stream()
                .map(TeacherInvitationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherInvitationDTO> getExpiredInvitations() {
        log.info("Fetching expired invitations");
        return teacherInvitationRepository.findByUsedFalseAndExpiryDateBefore(LocalDateTime.now())
                .stream()
                .map(TeacherInvitationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelInvitation(Long invitationId) {
        log.info("Cancelling invitation with id: {}", invitationId);

        TeacherInvitation invitation = teacherInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id: " + invitationId));

        teacherInvitationRepository.delete(invitation);
    }

    @Override
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("Cleaning up expired invitations");
        teacherInvitationRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }

    @Override
    public long getPendingInvitationCount() {
        return teacherInvitationRepository.countByUsedFalseAndExpiryDateAfter(LocalDateTime.now());
    }
}