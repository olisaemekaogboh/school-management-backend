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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.function.Function;
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

        SchoolClass schoolClass = resolveRequiredClass(student);
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

        Student existingStudent = studentRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        SchoolClass schoolClass = resolveRequiredClass(studentDetails);

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

    private SchoolClass resolveRequiredClass(Student student) {
        if (student.getSchoolClass() == null || student.getSchoolClass().getId() == null) {
            throw new ResourceNotFoundException("Class is required");
        }

        return classRepository.findById(student.getSchoolClass().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
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

    private String getStudentClassName(Student student) {
        return student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : null;
    }

    private String getStudentClassArm(Student student) {
        return student.getSchoolClass() != null ? student.getSchoolClass().getArm() : null;
    }

    private Long getStudentClassId(Student student) {
        return student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;
    }

    private String buildClassKey(String className, String arm) {
        String normalizedClass = className == null ? "" : className.trim().replaceAll("\\s+", "").toUpperCase();
        String normalizedArm = arm == null ? "" : arm.trim().toUpperCase();
        return normalizedClass + "::" + normalizedArm;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findDetailedById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> getStudentByAdmissionNumber(String admissionNumber) {
        return studentRepository.findDetailedByAdmissionNumber(admissionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<Student> searchStudents(String searchTerm) {
        return studentRepository.searchByName(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByClassId(Long classId) {
        return studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByState(String stateOfOrigin) {
        return studentRepository.findByStateOfOriginOrderByLastNameAscFirstNameAsc(stateOfOrigin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByLGA(String lga) {
        return studentRepository.findByLocalGovtAreaOrderByLastNameAscFirstNameAsc(lga);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getActiveStudents() {
        return studentRepository.findByStatusOrderByLastNameAscFirstNameAsc(Student.StudentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByStatus(Student.StudentStatus status) {
        return studentRepository.findByStatusOrderByLastNameAscFirstNameAsc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStudentCountByClass() {
        List<Object[]> results = studentRepository.countStudentsByClassWithArm();

        return results.stream()
                .collect(Collectors.toMap(
                        arr -> {
                            String className = (String) arr[1];
                            String arm = (String) arr[2];
                            return arm != null && !arm.isBlank() ? className + " - " + arm : className;
                        },
                        arr -> (Long) arr[3],
                        Long::sum,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalStudentCount() {
        return studentRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getActiveStudentCount() {
        return studentRepository.countByStatus(Student.StudentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
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
    public void updateBulkStudentClass(List<Long> studentIds, Long newClassId) {
        List<Student> students = studentRepository.findAllById(studentIds);

        SchoolClass targetClass = classRepository.findById(newClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + newClassId));

        students.forEach(student -> student.setSchoolClass(targetClass));
        studentRepository.saveAll(students);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmissionNumberUnique(String admissionNumber) {
        return !studentRepository.existsByAdmissionNumber(admissionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateAdmissionNumber() {
        String year = String.valueOf(Year.now().getValue());
        long count = studentRepository.count() + 1;
        String sequential = String.format("%04d", count);
        return "NIS/" + year + "/" + sequential;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStudentReport(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateClassReport(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPromotionPreview() {
        List<Student> allStudents = studentRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        Map<String, Long> currentDistribution = getStudentCountByClass();

        Map<String, Long> projectedDistribution = new LinkedHashMap<>();
        List<Map<String, Object>> promotions = new ArrayList<>();
        int excluded = 0;

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                continue;
            }

            String currentClass = getStudentClassName(student);
            String currentArm = getStudentClassArm(student);
            Long currentClassId = getStudentClassId(student);

            if (currentClass == null || currentClassId == null) {
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> promo = new HashMap<>();
                promo.put("studentId", student.getId());
                promo.put("student", student.getFirstName() + " " + student.getLastName());
                promo.put("fromClassId", currentClassId);
                promo.put("from", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
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
            promo.put("studentId", student.getId());
            promo.put("student", student.getFirstName() + " " + student.getLastName());
            promo.put("fromClassId", currentClassId);
            promo.put("from", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
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
        Map<String, SchoolClass> classCache = classRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> buildClassKey(c.getClassName(), c.getArm()),
                        Function.identity(),
                        (a, b) -> a,
                        HashMap::new
                ));

        int promoted = 0;
        int graduated = 0;
        int unchanged = 0;
        int excluded = 0;
        List<Map<String, Object>> promotionDetails = new ArrayList<>();

        for (Student student : allStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                unchanged++;
                continue;
            }

            String currentClass = getStudentClassName(student);
            String currentArm = getStudentClassArm(student);

            if (currentClass == null) {
                unchanged++;
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClassId", getStudentClassId(student));
                detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
                detail.put("nextClass", "EXCLUDED");
                detail.put("status", "EXCLUDED");
                detail.put("reason", student.getPromotionHoldReason() != null
                        ? student.getPromotionHoldReason()
                        : "No reason provided");
                promotionDetails.add(detail);
                continue;
            }

            String nextClassName = ClassProgression.getNextClass(currentClass);

            if (nextClassName == null || nextClassName.equalsIgnoreCase(currentClass)) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                studentRepository.save(student);

                graduated++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClassId", getStudentClassId(student));
                detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
                detail.put("nextClass", "GRADUATED");
                detail.put("status", "GRADUATED");
                promotionDetails.add(detail);
                continue;
            }

            SchoolClass nextSchoolClass = classCache.get(buildClassKey(nextClassName, currentArm));

            if (nextSchoolClass == null) {
                unchanged++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClassId", getStudentClassId(student));
                detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
                detail.put("nextClass", nextClassName + (currentArm != null ? " - " + currentArm : ""));
                detail.put("status", "SKIPPED");
                detail.put("reason", "Target class not found");
                promotionDetails.add(detail);
                continue;
            }

            student.setSchoolClass(nextSchoolClass);
            studentRepository.save(student);

            promoted++;
            Map<String, Object> detail = new HashMap<>();
            detail.put("studentId", student.getId());
            detail.put("studentName", student.getFirstName() + " " + student.getLastName());
            detail.put("currentClassId", getStudentClassId(student));
            detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
            detail.put("nextClassId", nextSchoolClass.getId());
            detail.put("nextClass", nextSchoolClass.getClassName() + (nextSchoolClass.getArm() != null && !nextSchoolClass.getArm().isBlank()
                    ? " - " + nextSchoolClass.getArm()
                    : ""));
            detail.put("status", "PROMOTED");
            promotionDetails.add(detail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("promotedCount", promoted);
        result.put("graduatedCount", graduated);
        result.put("excludedCount", excluded);
        result.put("unchangedCount", unchanged);
        result.put("details", promotionDetails);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> promoteSelectedStudents(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("promotedCount", 0);
            result.put("graduatedCount", 0);
            result.put("excludedCount", 0);
            result.put("unchangedCount", 0);
            result.put("details", List.of());
            return result;
        }

        List<Student> selectedStudents = studentRepository.findAllById(studentIds);
        List<Student> allClassesSeed = studentRepository.findAllById(studentIds); // harmless local seed to keep method flow simple
        Map<String, SchoolClass> classCache = classRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> buildClassKey(c.getClassName(), c.getArm()),
                        Function.identity(),
                        (a, b) -> a,
                        HashMap::new
                ));

        int promoted = 0;
        int graduated = 0;
        int unchanged = 0;
        int excluded = 0;
        List<Map<String, Object>> promotionDetails = new ArrayList<>();

        for (Student student : selectedStudents) {
            if (student.getStatus() != Student.StudentStatus.ACTIVE) {
                unchanged++;
                continue;
            }

            String currentClass = getStudentClassName(student);
            String currentArm = getStudentClassArm(student);

            if (currentClass == null) {
                unchanged++;
                continue;
            }

            if (student.isExcludeFromPromotion()) {
                excluded++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClassId", getStudentClassId(student));
                detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
                detail.put("nextClass", "EXCLUDED");
                detail.put("status", "EXCLUDED");
                detail.put("reason", student.getPromotionHoldReason() != null
                        ? student.getPromotionHoldReason()
                        : "No reason provided");
                promotionDetails.add(detail);
                continue;
            }

            String nextClassName = ClassProgression.getNextClass(currentClass);

            if (nextClassName == null || nextClassName.equalsIgnoreCase(currentClass)) {
                student.setStatus(Student.StudentStatus.GRADUATED);
                studentRepository.save(student);

                graduated++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClassId", getStudentClassId(student));
                detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
                detail.put("nextClass", "GRADUATED");
                detail.put("status", "GRADUATED");
                promotionDetails.add(detail);
                continue;
            }

            SchoolClass nextSchoolClass = classCache.get(buildClassKey(nextClassName, currentArm));

            if (nextSchoolClass == null) {
                unchanged++;
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getFirstName() + " " + student.getLastName());
                detail.put("currentClassId", getStudentClassId(student));
                detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
                detail.put("nextClass", nextClassName + (currentArm != null ? " - " + currentArm : ""));
                detail.put("status", "SKIPPED");
                detail.put("reason", "Target class not found");
                promotionDetails.add(detail);
                continue;
            }

            student.setSchoolClass(nextSchoolClass);
            studentRepository.save(student);

            promoted++;
            Map<String, Object> detail = new HashMap<>();
            detail.put("studentId", student.getId());
            detail.put("studentName", student.getFirstName() + " " + student.getLastName());
            detail.put("currentClassId", getStudentClassId(student));
            detail.put("currentClass", currentArm != null && !currentArm.isBlank() ? currentClass + " - " + currentArm : currentClass);
            detail.put("nextClassId", nextSchoolClass.getId());
            detail.put("nextClass", nextSchoolClass.getClassName() + (nextSchoolClass.getArm() != null && !nextSchoolClass.getArm().isBlank()
                    ? " - " + nextSchoolClass.getArm()
                    : ""));
            detail.put("status", "PROMOTED");
            promotionDetails.add(detail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("promotedCount", promoted);
        result.put("graduatedCount", graduated);
        result.put("excludedCount", excluded);
        result.put("unchangedCount", unchanged);
        result.put("details", promotionDetails);
        result.put("requestedIds", studentIds);
        result.put("processedCount", selectedStudents.size());
        result.put("debugSeedCount", allClassesSeed.size());

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> promoteClass(Long classId) {
        List<Student> classStudents = studentRepository.findBySchoolClassId(classId);

        if (classStudents.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("classId", classId);
            result.put("promotedCount", 0);
            result.put("graduatedCount", 0);
            result.put("excludedCount", 0);
            result.put("unchangedCount", 0);
            result.put("details", List.of());
            return result;
        }

        List<Long> studentIds = classStudents.stream()
                .map(Student::getId)
                .toList();

        Map<String, Object> result = promoteSelectedStudents(studentIds);
        result.put("classId", classId);
        return result;
    }

    @Override
    public Student togglePromotionExclusion(Long studentId, boolean exclude, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setExcludeFromPromotion(exclude);
        student.setPromotionHoldReason(exclude ? reason : null);

        return studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getExcludedStudents() {
        return studentRepository.findByExcludeFromPromotionTrueOrderByLastNameAscFirstNameAsc();
    }
}