package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ParentRepository;
import com.inkFront.schoolManagement.repository.StudentRepository;
import com.inkFront.schoolManagement.service.StudentService;
import com.inkFront.schoolManagement.utils.ClassProgression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    @Autowired
    public StudentServiceImpl(
            StudentRepository studentRepository,
            ParentRepository parentRepository
    ) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    @Override
    public Student registerStudent(Student student) {
        log.info("Registering new student: {} {}", student.getFirstName(), student.getLastName());

        if (student.getAdmissionNumber() == null || student.getAdmissionNumber().isEmpty()) {
            student.setAdmissionNumber(generateAdmissionNumber());
        }

        if (student.getStatus() == null) {
            student.setStatus(Student.StudentStatus.ACTIVE);
        }

        if (student.getAdmissionDate() == null) {
            student.setAdmissionDate(LocalDate.now());
        }

        if (student.getProfilePictureUrl() == null || student.getProfilePictureUrl().isEmpty()) {
            student.setProfilePictureUrl("/uploads/profile-pictures/default.png");
        }

        linkParentIfPossible(student);

        log.info(
                "Saving student with profile picture URL: {} and parentId: {}",
                student.getProfilePictureUrl(),
                student.getParent() != null ? student.getParent().getId() : null
        );

        return studentRepository.save(student);
    }

    @Override
    public Student updateStudent(Long id, Student studentDetails) {
        log.info("Updating student with ID: {}", id);

        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        existingStudent.setFirstName(studentDetails.getFirstName());
        existingStudent.setLastName(studentDetails.getLastName());
        existingStudent.setMiddleName(studentDetails.getMiddleName());
        existingStudent.setStudentClass(studentDetails.getStudentClass());
        existingStudent.setClassArm(studentDetails.getClassArm());
        existingStudent.setGender(studentDetails.getGender());
        existingStudent.setDateOfBirth(studentDetails.getDateOfBirth());

        existingStudent.setParentName(studentDetails.getParentName());
        existingStudent.setParentPhone(studentDetails.getParentPhone());
        existingStudent.setParentEmail(studentDetails.getParentEmail());

        existingStudent.setAddress(studentDetails.getAddress());
        existingStudent.setLocalGovtArea(studentDetails.getLocalGovtArea());
        existingStudent.setStateOfOrigin(studentDetails.getStateOfOrigin());
        existingStudent.setNationality(studentDetails.getNationality());
        existingStudent.setReligion(studentDetails.getReligion());
        existingStudent.setStatus(studentDetails.getStatus());

        existingStudent.setEmergencyContactName(studentDetails.getEmergencyContactName());
        existingStudent.setEmergencyContactPhone(studentDetails.getEmergencyContactPhone());
        existingStudent.setEmergencyContactRelationship(studentDetails.getEmergencyContactRelationship());

        existingStudent.setExcludeFromPromotion(studentDetails.isExcludeFromPromotion());
        existingStudent.setPromotionHoldReason(studentDetails.getPromotionHoldReason());
        existingStudent.setPreviousSchool(studentDetails.getPreviousSchool());

        if (studentDetails.getProfilePictureUrl() != null && !studentDetails.getProfilePictureUrl().isEmpty()) {
            existingStudent.setProfilePictureUrl(studentDetails.getProfilePictureUrl());
        }

        linkParentIfPossible(existingStudent);

        log.info(
                "Updated student profile picture URL: {}, parentId: {}",
                existingStudent.getProfilePictureUrl(),
                existingStudent.getParent() != null ? existingStudent.getParent().getId() : null
        );

        return studentRepository.save(existingStudent);
    }

    private void linkParentIfPossible(Student student) {
        String parentEmail = normalize(student.getParentEmail());

        if (parentEmail == null) {
            student.setParent(null);
            return;
        }

        Optional<Parent> parentOpt = parentRepository.findByEmailIgnoreCase(parentEmail);

        if (parentOpt.isPresent()) {
            Parent parent = parentOpt.get();

            student.setParent(parent);

            if (isBlank(student.getParentName())) {
                student.setParentName(buildParentName(parent));
            }
            if (isBlank(student.getParentPhone())) {
                student.setParentPhone(parent.getPhoneNumber());
            }
        } else {
            log.warn("No parent record found for email: {}", parentEmail);
            student.setParent(null);
        }
    }

    private String buildParentName(Parent parent) {
        return Arrays.asList(parent.getFirstName(), parent.getMiddleName(), parent.getLastName())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public Optional<Student> getStudentById(Long id) {
        log.debug("Fetching student by ID: {}", id);
        return studentRepository.findById(id);
    }

    @Override
    public Optional<Student> getStudentByAdmissionNumber(String admissionNumber) {
        log.debug("Fetching student by admission number: {}", admissionNumber);
        return studentRepository.findByAdmissionNumber(admissionNumber);
    }

    @Override
    public List<Student> getAllStudents() {
        log.debug("Fetching all students");
        return studentRepository.findAll();
    }

    @Override
    public Page<Student> getAllStudentsPaginated(Pageable pageable) {
        log.debug("Fetching students with pagination");
        return studentRepository.findAll(pageable);
    }

    @Override
    public void deleteStudent(Long id) {
        log.info("Deleting student with ID: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        studentRepository.delete(student);
    }

    @Override
    public void deleteStudentByAdmissionNumber(String admissionNumber) {
        log.info("Deleting student with admission number: {}", admissionNumber);
        Student student = studentRepository.findByAdmissionNumber(admissionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with admission number: " + admissionNumber));
        studentRepository.delete(student);
    }

    @Override
    public List<Student> searchStudents(String searchTerm) {
        log.debug("Searching students with term: {}", searchTerm);
        return studentRepository.searchByName(searchTerm);
    }

    @Override
    public List<Student> getStudentsByClass(String studentClass) {
        log.debug("Fetching students in class: {}", studentClass);
        return studentRepository.findByStudentClass(studentClass);
    }

    @Override
    public List<Student> getStudentsByClassAndArm(String studentClass, String classArm) {
        log.debug("Fetching students in class: {} and arm: {}", studentClass, classArm);
        return studentRepository.findByStudentClassAndClassArm(studentClass, classArm);
    }

    @Override
    public List<Student> getStudentsByState(String stateOfOrigin) {
        log.debug("Fetching students from state: {}", stateOfOrigin);
        return studentRepository.findByStateOfOrigin(stateOfOrigin);
    }

    @Override
    public List<Student> getStudentsByLGA(String lga) {
        log.debug("Fetching students from LGA: {}", lga);
        return studentRepository.findByLocalGovtArea(lga);
    }

    @Override
    public List<Student> getActiveStudents() {
        log.debug("Fetching active students");
        return studentRepository.findByStatus(Student.StudentStatus.ACTIVE);
    }

    @Override
    public List<Student> getStudentsByStatus(Student.StudentStatus status) {
        log.debug("Fetching students with status: {}", status);
        return studentRepository.findByStatus(status);
    }

    @Override
    public Map<String, Long> getStudentCountByClass() {
        log.debug("Calculating student count by class");
        List<Object[]> results = studentRepository.countStudentsByClass();

        return results.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    public Long getTotalStudentCount() {
        log.debug("Getting total student count");
        return studentRepository.count();
    }

    @Override
    public Long getActiveStudentCount() {
        log.debug("Getting active student count");
        return (long) studentRepository.findByStatus(Student.StudentStatus.ACTIVE).size();
    }

    @Override
    public List<Student> getRecentAdmissions(int days) {
        log.debug("Fetching recent admissions from last {} days", days);
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return studentRepository.findRecentAdmissions(cutoffDate);
    }

    @Override
    public List<Student> registerBulkStudents(List<Student> students) {
        log.info("Registering {} students in bulk", students.size());
        return students.stream()
                .map(this::registerStudent)
                .collect(Collectors.toList());
    }

    @Override
    public void updateBulkStudentClass(List<Long> studentIds, String newClass) {
        log.info("Updating class for {} students to {}", studentIds.size(), newClass);
        List<Student> students = studentRepository.findAllById(studentIds);
        students.forEach(student -> student.setStudentClass(newClass));
        studentRepository.saveAll(students);
    }

    @Override
    public boolean isAdmissionNumberUnique(String admissionNumber) {
        return !studentRepository.existsByAdmissionNumber(admissionNumber);
    }

    @Override
    public String generateAdmissionNumber() {
        String year = String.valueOf(Year.now().getValue());
        long count = studentRepository.count() + 1;
        String sequential = String.format("%04d", count);
        return "NIS/" + year + "/" + sequential;
    }

    @Override
    public byte[] generateStudentReport(Long studentId) {
        log.info("Generating report for student ID: {}", studentId);
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return new byte[0];
    }

    @Override
    public byte[] generateClassReport(String studentClass) {
        log.info("Generating report for class: {}", studentClass);
        studentRepository.findByStudentClass(studentClass);
        return new byte[0];
    }

    @Override
    public Map<String, Object> getPromotionPreview() {
        log.debug("Generating promotion preview");

        List<Student> allStudents = studentRepository.findAll();

        Map<String, Long> currentDistribution = studentRepository.countStudentsByClass()
                .stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        Map<String, Long> projectedDistribution = new HashMap<>();
        List<Map<String, Object>> promotions = new ArrayList<>();
        int excluded = 0;

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) continue;

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> promo = new HashMap<>();
                promo.put("studentId", student.getId().toString());
                promo.put("student", student.getFirstName() + " " + student.getLastName());
                promo.put("from", student.getStudentClass());
                promo.put("to", "EXCLUDED");
                promo.put("status", "EXCLUDED");
                promo.put("reason", student.getPromotionHoldReason() != null
                        ? student.getPromotionHoldReason()
                        : "No reason provided");
                promotions.add(promo);
                continue;
            }

            String currentClass = student.getStudentClass();
            String nextClass = ClassProgression.getNextClass(currentClass);

            projectedDistribution.merge(nextClass, 1L, Long::sum);

            Map<String, Object> promo = new HashMap<>();
            promo.put("studentId", student.getId().toString());
            promo.put("student", student.getFirstName() + " " + student.getLastName());
            promo.put("from", currentClass);
            promo.put("to", nextClass);
            promo.put("status", "READY");
            promotions.add(promo);
        }

        Map<String, Object> preview = new HashMap<>();
        preview.put("currentDistribution", currentDistribution);
        preview.put("projectedDistribution", projectedDistribution);
        preview.put("promotions", promotions);
        preview.put("totalStudents", allStudents.size());
        preview.put("excludedCount", excluded);

        return preview;
    }

    @Override
    @Transactional
    public Map<String, Object> promoteAllStudents() {
        log.info("Starting end-of-session promotion for all students");

        List<Student> allStudents = studentRepository.findAll();
        int promoted = 0, graduated = 0, unchanged = 0, excluded = 0;
        List<Map<String, Object>> promotionDetails = new ArrayList<>();

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                unchanged++;
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId().toString());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClass", student.getStudentClass());
                detail.put("nextClass", "EXCLUDED");
                detail.put("status", "EXCLUDED");
                detail.put("reason", student.getPromotionHoldReason() != null
                        ? student.getPromotionHoldReason()
                        : "No reason provided");
                promotionDetails.add(detail);
                continue;
            }

            String currentClass = student.getStudentClass();
            String nextClass = ClassProgression.getNextClass(currentClass);

            Map<String, Object> detail = new HashMap<>();
            detail.put("studentId", student.getId().toString());
            detail.put("studentName", student.getFirstName() + " " + student.getLastName());
            detail.put("currentClass", currentClass);
            detail.put("nextClass", nextClass);

            if ("GRADUATED".equals(nextClass)) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                detail.put("status", "GRADUATED");
                graduated++;
            } else if (!nextClass.equals(currentClass)) {
                student.setStudentClass(nextClass);
                detail.put("status", "PROMOTED");
                promoted++;
            } else {
                detail.put("status", "UNCHANGED");
                unchanged++;
            }

            promotionDetails.add(detail);
        }

        studentRepository.saveAll(allStudents);

        Map<String, Object> result = new HashMap<>();
        result.put("promoted", promoted);
        result.put("graduated", graduated);
        result.put("unchanged", unchanged);
        result.put("excluded", excluded);
        result.put("total", allStudents.size());
        result.put("details", promotionDetails);
        result.put("timestamp", LocalDateTime.now());

        log.info(
                "Promotion complete: {} promoted, {} graduated, {} unchanged, {} excluded",
                promoted, graduated, unchanged, excluded
        );

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> promoteSelectedStudents(List<Long> studentIds) {
        log.info("Promoting {} selected students", studentIds.size());

        List<Student> selectedStudents = studentRepository.findAllById(studentIds);
        int promoted = 0, graduated = 0;

        for (Student student : selectedStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) continue;

            String currentClass = student.getStudentClass();
            String nextClass = ClassProgression.getNextClass(currentClass);

            if ("GRADUATED".equals(nextClass)) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                graduated++;
            } else if (!nextClass.equals(currentClass)) {
                student.setStudentClass(nextClass);
                promoted++;
            }
        }

        studentRepository.saveAll(selectedStudents);

        Map<String, Object> result = new HashMap<>();
        result.put("promoted", promoted);
        result.put("graduated", graduated);
        result.put("total", selectedStudents.size());

        return result;
    }

    @Override
    @Transactional
    public Student togglePromotionExclusion(Long studentId, boolean exclude, String reason) {
        log.info("Toggling promotion exclusion for student {}: exclude={}", studentId, exclude);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setExcludeFromPromotion(exclude);
        student.setPromotionHoldReason(exclude ? reason : null);

        return studentRepository.save(student);
    }

    @Override
    public List<Student> getExcludedStudents() {
        log.debug("Fetching students excluded from promotion");
        return studentRepository.findByExcludeFromPromotionTrue();
    }
}