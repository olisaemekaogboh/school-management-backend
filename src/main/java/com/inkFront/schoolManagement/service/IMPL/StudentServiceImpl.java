package com.inkFront.schoolManagement.service.IMPL;

import com.inkFront.schoolManagement.exception.ResourceNotFoundException;
import com.inkFront.schoolManagement.model.Parent;
import com.inkFront.schoolManagement.model.SchoolClass;
import com.inkFront.schoolManagement.model.Student;
import com.inkFront.schoolManagement.repository.ClassRepository;
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
    private final ClassRepository classRepository;

    @Autowired
    public StudentServiceImpl(
            StudentRepository studentRepository,
            ParentRepository parentRepository,
            ClassRepository classRepository
    ) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.classRepository = classRepository;
    }

    @Override
    public Student registerStudent(Student student) {
        log.info("Registering new student: {} {}", student.getFirstName(), student.getLastName());

        if (student.getSchoolClass() == null || student.getSchoolClass().getId() == null) {
            throw new ResourceNotFoundException("Class is required");
        }

        SchoolClass schoolClass = classRepository.findById(student.getSchoolClass().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        student.setSchoolClass(schoolClass);

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

        return studentRepository.save(student);
    }

    @Override
    public Student updateStudent(Long id, Student studentDetails) {
        log.info("Updating student with ID: {}", id);

        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (studentDetails.getSchoolClass() == null || studentDetails.getSchoolClass().getId() == null) {
            throw new ResourceNotFoundException("Class is required");
        }

        SchoolClass schoolClass = classRepository.findById(studentDetails.getSchoolClass().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        existingStudent.setFirstName(studentDetails.getFirstName());
        existingStudent.setLastName(studentDetails.getLastName());
        existingStudent.setMiddleName(studentDetails.getMiddleName());
        existingStudent.setSchoolClass(schoolClass);
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
        return studentRepository.findById(id);
    }

    @Override
    public Optional<Student> getStudentByAdmissionNumber(String admissionNumber) {
        return studentRepository.findByAdmissionNumber(admissionNumber);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Page<Student> getAllStudentsPaginated(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Override
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        studentRepository.delete(student);
    }

    @Override
    public void deleteStudentByAdmissionNumber(String admissionNumber) {
        Student student = studentRepository.findByAdmissionNumber(admissionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with admission number: " + admissionNumber));
        studentRepository.delete(student);
    }

    @Override
    public List<Student> searchStudents(String searchTerm) {
        return studentRepository.searchByName(searchTerm);
    }

    @Override
    public List<Student> getStudentsByClass(String studentClass) {
        return studentRepository.findByStudentClass(studentClass);
    }

    @Override
    public List<Student> getStudentsByClassAndArm(String studentClass, String classArm) {
        return studentRepository.findByStudentClassAndClassArm(studentClass, classArm);
    }

    @Override
    public List<Student> getStudentsByState(String stateOfOrigin) {
        return studentRepository.findByStateOfOrigin(stateOfOrigin);
    }

    @Override
    public List<Student> getStudentsByLGA(String lga) {
        return studentRepository.findByLocalGovtArea(lga);
    }

    @Override
    public List<Student> getActiveStudents() {
        return studentRepository.findByStatus(Student.StudentStatus.ACTIVE);
    }

    @Override
    public List<Student> getStudentsByStatus(Student.StudentStatus status) {
        return studentRepository.findByStatus(status);
    }

    @Override
    public Map<String, Long> getStudentCountByClass() {
        List<Object[]> results = studentRepository.countStudentsByClass();

        return results.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    public Long getTotalStudentCount() {
        return studentRepository.count();
    }

    @Override
    public Long getActiveStudentCount() {
        return (long) studentRepository.findByStatus(Student.StudentStatus.ACTIVE).size();
    }

    @Override
    public List<Student> getRecentAdmissions(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return studentRepository.findRecentAdmissions(cutoffDate);
    }

    @Override
    public List<Student> registerBulkStudents(List<Student> students) {
        return students.stream()
                .map(this::registerStudent)
                .collect(Collectors.toList());
    }

    @Override
    public void updateBulkStudentClass(List<Long> studentIds, String newClass) {
        List<Student> students = studentRepository.findAllById(studentIds);

        SchoolClass targetClass = classRepository.findByClassName(newClass)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + newClass));

        students.forEach(student -> student.setSchoolClass(targetClass));
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
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return new byte[0];
    }

    @Override
    public byte[] generateClassReport(String studentClass) {
        studentRepository.findByStudentClass(studentClass);
        return new byte[0];
    }

    @Override
    public Map<String, Object> getPromotionPreview() {
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

            String currentClass = student.getStudentClass();
            if (currentClass == null) continue;

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> promo = new HashMap<>();
                promo.put("studentId", student.getId().toString());
                promo.put("student", student.getFirstName() + " " + student.getLastName());
                promo.put("from", currentClass);
                promo.put("to", "EXCLUDED");
                promo.put("status", "EXCLUDED");
                promo.put("reason", student.getPromotionHoldReason() != null
                        ? student.getPromotionHoldReason()
                        : "No reason provided");
                promotions.add(promo);
                continue;
            }

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
        List<Student> allStudents = studentRepository.findAll();
        int promoted = 0, graduated = 0, unchanged = 0, excluded = 0;
        List<Map<String, Object>> promotionDetails = new ArrayList<>();

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                unchanged++;
                continue;
            }

            String currentClass = student.getStudentClass();
            if (currentClass == null) {
                unchanged++;
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId().toString());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClass", currentClass);
                detail.put("nextClass", "EXCLUDED");
                detail.put("status", "EXCLUDED");
                detail.put("reason", student.getPromotionHoldReason() != null
                        ? student.getPromotionHoldReason()
                        : "No reason provided");
                promotionDetails.add(detail);
                continue;
            }

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
                SchoolClass promotedClass = classRepository.findByClassNameAndArmNormalized(
                        nextClass,
                        student.getClassArm()
                ).orElse(null);

                if (promotedClass != null) {
                    student.setSchoolClass(promotedClass);
                    detail.put("status", "PROMOTED");
                    promoted++;
                } else {
                    detail.put("status", "UNCHANGED");
                    detail.put("reason", "Target class does not exist");
                    unchanged++;
                }
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

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> promoteSelectedStudents(List<Long> studentIds) {
        List<Student> selectedStudents = studentRepository.findAllById(studentIds);
        int promoted = 0, graduated = 0;

        for (Student student : selectedStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) continue;

            String currentClass = student.getStudentClass();
            if (currentClass == null) continue;

            String nextClass = ClassProgression.getNextClass(currentClass);

            if ("GRADUATED".equals(nextClass)) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                graduated++;
            } else if (!nextClass.equals(currentClass)) {
                SchoolClass promotedClass = classRepository.findByClassNameAndArmNormalized(
                        nextClass,
                        student.getClassArm()
                ).orElse(null);

                if (promotedClass != null) {
                    student.setSchoolClass(promotedClass);
                    promoted++;
                }
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
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setExcludeFromPromotion(exclude);
        student.setPromotionHoldReason(exclude ? reason : null);

        return studentRepository.save(student);
    }

    @Override
    public List<Student> getExcludedStudents() {
        return studentRepository.findByExcludeFromPromotionTrue();
    }
}