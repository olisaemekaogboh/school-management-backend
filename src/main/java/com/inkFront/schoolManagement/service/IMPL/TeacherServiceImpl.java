package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.dto.CompleteRegistrationDTO;
import com.inkFront.schoolManagement.dto.TeacherDTO;
import com.inkFront.schoolManagement.dto.TeacherInvitationDTO;
import com.inkFront.schoolManagement.dto.TeacherInviteDTO;
import com.inkFront.schoolManagement.exception.BusinessException;
import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Teacher;
import com.inkFront.schoolManagement.model.TeacherInvitation;
import com.inkFront.schoolManagement.model.User;
import com.inkFront.schoolManagement.repository.ClassRepository;
import com.inkFront.schoolManagement.repository.TeacherInvitationRepository;
import com.inkFront.schoolManagement.repository.TeacherRepository;
import com.inkFront.schoolManagement.repository.UserRepository;
import com.inkFront.schoolManagement.service.FileStorageService;
import com.inkFront.schoolManagement.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherServiceImpl implements TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherServiceImpl.class);

    private final TeacherRepository teacherRepository;
    private final TeacherInvitationRepository teacherInvitationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final ClassRepository classRepository;

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

    private String normalizeIdValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateNextTeacherId() {
        String yearSuffix = String.valueOf(Year.now().getValue()).substring(2);
        long maxSequence = getMaxTeacherIdSequence();
        long sequence = maxSequence + 1;

        String candidate = "TCH" + yearSuffix + String.format("%04d", sequence);

        while (teacherRepository.existsByTeacherId(candidate)) {
            sequence++;
            candidate = "TCH" + yearSuffix + String.format("%04d", sequence);
        }

        return candidate;
    }

    private String generateEmployeeId() {
        String yearSuffix = String.valueOf(Year.now().getValue()).substring(2);
        long maxSequence = getMaxEmployeeIdSequence();
        long sequence = maxSequence + 1;

        String candidate = "EMP" + yearSuffix + String.format("%04d", sequence);

        while (teacherRepository.existsByEmployeeId(candidate)) {
            sequence++;
            candidate = "EMP" + yearSuffix + String.format("%04d", sequence);
        }

        return candidate;
    }

    private long getMaxTeacherIdSequence() {
        List<String> allTeacherIds = teacherRepository.findAllTeacherIds();
        long maxSeq = 0;

        for (String id : allTeacherIds) {
            if (id != null && id.matches(".*\\d+$")) {
                try {
                    String numericPart = id.replaceAll(".*?(\\d+)$", "$1");
                    long seq = Long.parseLong(numericPart);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Could not parse numeric part from teacher ID: {}", id);
                }
            }
        }

        return maxSeq;
    }

    private long getMaxEmployeeIdSequence() {
        List<String> allEmployeeIds = teacherRepository.findAllEmployeeIds();
        long maxSeq = 0;

        for (String id : allEmployeeIds) {
            if (id != null && id.matches(".*\\d+$")) {
                try {
                    String numericPart = id.replaceAll(".*?(\\d+)$", "$1");
                    long seq = Long.parseLong(numericPart);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Could not parse numeric part from employee ID: {}", id);
                }
            }
        }

        return maxSeq;
    }

    private void assignGeneratedIdsIfMissing(Teacher teacher) {
        if (normalizeIdValue(teacher.getTeacherId()) == null) {
            teacher.setTeacherId(generateNextTeacherId());
        } else {
            String existingId = teacher.getTeacherId().trim();
            if (!existingId.matches("^TCH\\d{2}\\d{4}$")) {
                log.warn("Teacher ID {} does not follow standard format, but keeping as is", existingId);
            }
            teacher.setTeacherId(existingId);
        }

        if (normalizeIdValue(teacher.getEmployeeId()) == null) {
            teacher.setEmployeeId(generateEmployeeId());
        } else {
            String existingId = teacher.getEmployeeId().trim();
            if (!existingId.matches("^EMP\\d{2}\\d{4}$") && !existingId.matches("^\\d{4}$")) {
                log.warn("Employee ID {} does not follow standard format, but keeping as is", existingId);
            }
            teacher.setEmployeeId(existingId);
        }

        String employeeId = teacher.getEmployeeId();
        if (employeeId != null && employeeId.matches("^\\d{4}$")) {
            String yearSuffix = String.valueOf(Year.now().getValue()).substring(2);
            String newEmployeeId = "EMP" + yearSuffix + employeeId;

            if (!teacherRepository.existsByEmployeeId(newEmployeeId)) {
                teacher.setEmployeeId(newEmployeeId);
                log.info("Converted legacy employee ID {} to {}", employeeId, newEmployeeId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDTO getTeacherByTeacherId(String id) {
        Teacher teacher = teacherRepository.findByTeacherIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
        return toTeacherDTO(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherDTO> getAllTeachers() {
        return teacherRepository.findAll()
                .stream()
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
    public TeacherDTO createTeacher(TeacherDTO teacherDTO, MultipartFile profilePicture) {
        log.info("Creating teacher with email: {}", teacherDTO.getEmail());

        String normalizedEmail = teacherDTO.getEmail() != null
                ? teacherDTO.getEmail().trim().toLowerCase()
                : null;

        if (normalizedEmail != null && teacherRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email already exists");
        }

        String requestedTeacherId = normalizeIdValue(teacherDTO.getTeacherId());
        if (requestedTeacherId != null && teacherRepository.existsByTeacherId(requestedTeacherId)) {
            throw new BusinessException("Teacher ID already exists");
        }

        String requestedEmployeeId = normalizeIdValue(teacherDTO.getEmployeeId());
        if (requestedEmployeeId != null && teacherRepository.existsByEmployeeId(requestedEmployeeId)) {
            throw new BusinessException("Employee ID already exists");
        }

        Teacher teacher = new Teacher();
        teacher.setSubjects(new HashSet<>());
        teacher.setQualifications(new HashSet<>());

        mapTeacherFields(teacher, teacherDTO, true);

        if (normalizedEmail != null) {
            teacher.setEmail(normalizedEmail);
        }

        if (requestedTeacherId != null) {
            teacher.setTeacherId(requestedTeacherId);
        }

        if (requestedEmployeeId != null) {
            teacher.setEmployeeId(requestedEmployeeId);
        }

        assignGeneratedIdsIfMissing(teacher);

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String pictureUrl = fileStorageService.storeFile(profilePicture);
            teacher.setProfilePictureUrl(pictureUrl);
        }

        Teacher savedTeacher = teacherRepository.save(teacher);

        log.info("Teacher created successfully with id: {} and employeeId: {}",
                savedTeacher.getId(), savedTeacher.getEmployeeId());
        return toTeacherDTO(savedTeacher);
    }

    @Override
    public TeacherDTO updateTeacher(Long id, TeacherDTO teacherDTO, MultipartFile profilePicture) {
        log.info("Updating teacher with id: {}", id);

        Teacher existingTeacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        String requestedEmail = teacherDTO.getEmail() != null
                ? teacherDTO.getEmail().trim().toLowerCase()
                : null;

        if (requestedEmail != null
                && existingTeacher.getEmail() != null
                && !requestedEmail.equalsIgnoreCase(existingTeacher.getEmail())
                && teacherRepository.existsByEmail(requestedEmail)) {
            throw new BusinessException("Email already exists");
        }

        String requestedTeacherId = normalizeIdValue(teacherDTO.getTeacherId());
        if (requestedTeacherId != null
                && !requestedTeacherId.equals(existingTeacher.getTeacherId())
                && teacherRepository.existsByTeacherId(requestedTeacherId)) {
            throw new BusinessException("Teacher ID already exists");
        }

        String requestedEmployeeId = normalizeIdValue(teacherDTO.getEmployeeId());
        if (requestedEmployeeId != null
                && !requestedEmployeeId.equals(existingTeacher.getEmployeeId())
                && teacherRepository.existsByEmployeeId(requestedEmployeeId)) {
            throw new BusinessException("Employee ID already exists");
        }

        String preservedTeacherId = existingTeacher.getTeacherId();
        String preservedEmployeeId = existingTeacher.getEmployeeId();

        if (existingTeacher.getSubjects() == null) {
            existingTeacher.setSubjects(new HashSet<>());
        }
        if (existingTeacher.getQualifications() == null) {
            existingTeacher.setQualifications(new HashSet<>());
        }

        mapTeacherFields(existingTeacher, teacherDTO, false);

        if (requestedEmail != null) {
            existingTeacher.setEmail(requestedEmail);
        }

        if (requestedTeacherId != null) {
            existingTeacher.setTeacherId(requestedTeacherId);
        } else {
            existingTeacher.setTeacherId(preservedTeacherId);
        }

        if (requestedEmployeeId != null) {
            existingTeacher.setEmployeeId(requestedEmployeeId);
        } else if (normalizeIdValue(preservedEmployeeId) != null) {
            existingTeacher.setEmployeeId(preservedEmployeeId);
        } else {
            existingTeacher.setEmployeeId(generateEmployeeId());
        }

        if (existingTeacher.getEmployeeId() != null &&
                existingTeacher.getEmployeeId().matches("^\\d{4}$")) {
            String yearSuffix = String.valueOf(Year.now().getValue()).substring(2);
            String newEmployeeId = "EMP" + yearSuffix + existingTeacher.getEmployeeId();
            if (!teacherRepository.existsByEmployeeId(newEmployeeId)) {
                existingTeacher.setEmployeeId(newEmployeeId);
                log.info("Converted legacy employee ID to standard format: {}", newEmployeeId);
            }
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String pictureUrl = fileStorageService.storeFile(profilePicture);
            existingTeacher.setProfilePictureUrl(pictureUrl);
        }

        Teacher updatedTeacher = teacherRepository.save(existingTeacher);

        log.info("Teacher updated successfully with id: {} and employeeId: {}",
                updatedTeacher.getId(), updatedTeacher.getEmployeeId());
        return toTeacherDTO(updatedTeacher);
    }

    @Override
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        int affectedClasses = classRepository.clearTeacherFromClasses(id);
        log.info("Cleared teacher {} from {} class(es) before delete", id, affectedClasses);

        if (teacher.getUser() != null) {
            User linkedUser = teacher.getUser();
            linkedUser.setTeacher(null);
            teacher.setUser(null);
        }

        teacherRepository.delete(teacher);
        log.info("Teacher {} deleted successfully", id);
    }

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

    @Override
    @Transactional
    public TeacherDTO addSubject(Long id, String subject) {
        log.info("Adding subject {} to teacher with id: {}", subject, id);

        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (teacher.getSubjects() == null) {
            teacher.setSubjects(new HashSet<>());
        }

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

        if (teacher.getSubjects() != null) {
            teacher.getSubjects().remove(subject);
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return toTeacherDTO(updatedTeacher);
    }

    @Override
    @Transactional
    public TeacherDTO addQualification(Long id, String qualification) {
        log.info("Adding qualification {} to teacher with id: {}", qualification, id);

        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (teacher.getQualifications() == null) {
            teacher.setQualifications(new HashSet<>());
        }

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
                teacher.setEmploymentStatus(Teacher.EmploymentStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid employment status: " + status);
            }
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return toTeacherDTO(updatedTeacher);
    }

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
        return generateNextTeacherId();
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

            if (normalizeIdValue(teacher.getTeacherId()) == null) {
                teacher.setTeacherId(generateNextTeacherId());
            }

            if (normalizeIdValue(teacher.getEmployeeId()) == null) {
                teacher.setEmployeeId(generateEmployeeId());
            } else {
                String employeeId = teacher.getEmployeeId();
                if (employeeId != null && employeeId.matches("^\\d{4}$")) {
                    String yearSuffix = String.valueOf(Year.now().getValue()).substring(2);
                    String newEmployeeId = "EMP" + yearSuffix + employeeId;
                    if (!teacherRepository.existsByEmployeeId(newEmployeeId)) {
                        teacher.setEmployeeId(newEmployeeId);
                        log.info("Converted legacy employee ID to standard format: {}", newEmployeeId);
                    }
                }
            }

        } else {
            teacher = new Teacher();
            teacher.setFirstName(invitation.getFirstName());
            teacher.setLastName(invitation.getLastName());
            teacher.setEmail(invitation.getEmail());
            teacher.setPhoneNumber(invitation.getPhoneNumber());
            teacher.setUser(user);
            teacher.setTeacherId(generateNextTeacherId());
            teacher.setEmployeeId(generateEmployeeId());
            teacher.setSubjects(new HashSet<>());
            teacher.setQualifications(new HashSet<>());
            teacher.setEmploymentType(Teacher.EmploymentType.FULL_TIME);
            teacher.setEmploymentStatus(Teacher.EmploymentStatus.ACTIVE);
        }

        user.setTeacher(teacher);
        invitation.setUsed(true);

        teacherRepository.save(teacher);
        User savedUser = userRepository.save(user);
        teacherInvitationRepository.save(invitation);

        log.info("Teacher registration completed for: {} with teacher ID: {} and employee ID: {}",
                savedUser.getUsername(), teacher.getTeacherId(), teacher.getEmployeeId());

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

    private void mapTeacherFields(Teacher teacher, TeacherDTO dto, boolean isCreate) {
        if (dto.getFirstName() != null) {
            teacher.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null) {
            teacher.setLastName(dto.getLastName().trim());
        }

        teacher.setMiddleName(dto.getMiddleName() != null ? dto.getMiddleName().trim() : null);

        if (dto.getEmail() != null) {
            teacher.setEmail(dto.getEmail().trim().toLowerCase());
        }

        teacher.setPhoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : null);
        teacher.setAlternatePhone(dto.getAlternatePhone() != null ? dto.getAlternatePhone().trim() : null);
        teacher.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        teacher.setQualification(dto.getQualification() != null ? dto.getQualification().trim() : null);
        teacher.setSpecialization(dto.getSpecialization() != null ? dto.getSpecialization().trim() : null);
        teacher.setDepartment(dto.getDepartment() != null ? dto.getDepartment().trim() : null);
        teacher.setDesignation(dto.getDesignation() != null ? dto.getDesignation().trim() : null);

        String normalizedEmployeeId = normalizeIdValue(dto.getEmployeeId());
        if (normalizedEmployeeId != null) {
            teacher.setEmployeeId(normalizedEmployeeId);
        } else if (isCreate && normalizeIdValue(teacher.getEmployeeId()) == null) {
            teacher.setEmployeeId(null);
        }

        String normalizedTeacherId = normalizeIdValue(dto.getTeacherId());
        if (normalizedTeacherId != null) {
            teacher.setTeacherId(normalizedTeacherId);
        } else if (isCreate && normalizeIdValue(teacher.getTeacherId()) == null) {
            teacher.setTeacherId(null);
        }

        teacher.setEmergencyContactName(
                dto.getEmergencyContactName() != null ? dto.getEmergencyContactName().trim() : null
        );
        teacher.setEmergencyContactPhone(
                dto.getEmergencyContactPhone() != null ? dto.getEmergencyContactPhone().trim() : null
        );
        teacher.setEmergencyContactRelationship(
                dto.getEmergencyContactRelationship() != null
                        ? dto.getEmergencyContactRelationship().trim()
                        : null
        );

        teacher.setDateOfBirth(dto.getDateOfBirth());
        teacher.setDateOfJoining(dto.getDateOfJoining());
        teacher.setYearsOfExperience(dto.getYearsOfExperience());

        if (dto.getGender() != null && !dto.getGender().trim().isEmpty()) {
            teacher.setGender(parseGender(dto.getGender()));
        }

        if (dto.getEmploymentType() != null && !dto.getEmploymentType().trim().isEmpty()) {
            teacher.setEmploymentType(parseEmploymentType(dto.getEmploymentType()));
        }

        if (dto.getEmploymentStatus() != null && !dto.getEmploymentStatus().trim().isEmpty()) {
            teacher.setEmploymentStatus(parseEmploymentStatus(dto.getEmploymentStatus()));
        }

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            teacher.setStatus(parseTeacherStatus(dto.getStatus()));
        }

        if (dto.getMaritalStatus() != null && !dto.getMaritalStatus().trim().isEmpty()) {
            teacher.setMaritalStatus(parseMaritalStatus(dto.getMaritalStatus()));
        }

        if (teacher.getSubjects() == null) {
            teacher.setSubjects(new HashSet<>());
        } else {
            teacher.getSubjects().clear();
        }
        if (dto.getSubjects() != null) {
            teacher.getSubjects().addAll(
                    dto.getSubjects().stream()
                            .filter(s -> s != null && !s.trim().isEmpty())
                            .map(String::trim)
                            .collect(Collectors.toSet())
            );
        }

        if (teacher.getQualifications() == null) {
            teacher.setQualifications(new HashSet<>());
        } else {
            teacher.getQualifications().clear();
        }
        if (dto.getQualifications() != null) {
            teacher.getQualifications().addAll(
                    dto.getQualifications().stream()
                            .filter(q -> q != null && !q.trim().isEmpty())
                            .map(String::trim)
                            .collect(Collectors.toSet())
            );
        }
    }

    private Teacher.Gender parseGender(String value) {
        try {
            return Teacher.Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid gender: " + value);
        }
    }

    private Teacher.MaritalStatus parseMaritalStatus(String value) {
        try {
            return Teacher.MaritalStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid marital status: " + value);
        }
    }

    private Teacher.EmploymentType parseEmploymentType(String value) {
        try {
            return Teacher.EmploymentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid employment type: " + value);
        }
    }

    private Teacher.EmploymentStatus parseEmploymentStatus(String value) {
        try {
            return Teacher.EmploymentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid employment status: " + value);
        }
    }

    private Teacher.TeacherStatus parseTeacherStatus(String value) {
        try {
            return Teacher.TeacherStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid teacher status: " + value);
        }
    }
}